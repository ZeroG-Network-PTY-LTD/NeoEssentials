package com.zerog.neoessentials.votifier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

/**
 * Raw-socket Votifier vote listener — its own TCP port, separate from the Minecraft server
 * port. Supports both wire protocols on the same port (auto-detected per connection, same as
 * real Votifier/NuVotifier servers):
 *
 * <ul>
 *   <li><b>Protocol V1</b> — a 256-byte RSA/PKCS1-encrypted block, no framing. Decrypted
 *       plaintext is 5 newline-separated fields: {@code VOTE}, serviceName, username, address,
 *       timestamp.</li>
 *   <li><b>Protocol V2 (NuVotifier-compatible)</b> — magic bytes {@code 0x73 0x3A} ("s:"),
 *       then a 2-byte big-endian length prefix, then that many bytes of a JSON object
 *       {@code {"payload": "<json string>", "signature": "<base64 HMAC-SHA256>"}}. The
 *       signature is HMAC-SHA256 over the raw {@code payload} string bytes, keyed by the
 *       per-server shared token. The inner payload JSON carries serviceName/username/address/
 *       timestamp/challenge — challenge must match the one sent in this connection's greeting
 *       (replay protection).</li>
 * </ul>
 *
 * Byte-level protocol details verified against the reference NeoForge implementation
 * (github.com/uberswe/votifier, {@code common/.../VotifierServer.java}) rather than
 * reconstructed from memory — getting a wire-format detail wrong makes real vote sites fail
 * silently with no useful error on either end.
 */
public class VotifierServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VotifierServer.class);

    private static class Holder {
        static final VotifierServer INSTANCE = new VotifierServer();
    }
    public static VotifierServer getInstance() { return Holder.INSTANCE; }

    private final RSAKeyManager keyManager = new RSAKeyManager();
    private ServerSocket serverSocket;
    private Thread listenerThread;
    private volatile boolean running;
    private String v2Token = "";

    private VotifierServer() {}

    public void start() throws Exception {
        JsonObject votifier = getVotifierConfig();
        String host = votifier != null && votifier.has("host") ? votifier.get("host").getAsString() : "0.0.0.0";
        int port = votifier != null && votifier.has("port") ? votifier.get("port").getAsInt() : 8192;
        v2Token = votifier != null && votifier.has("v2Token") ? votifier.get("v2Token").getAsString() : "";

        Path configDir = Path.of(com.zerog.neoessentials.util.ResourceUtil.CONFIG_DIR).resolve("votifier");
        keyManager.load(configDir);

        if (v2Token == null || v2Token.isBlank()) {
            v2Token = UUID.randomUUID().toString();
            saveGeneratedToken(v2Token);
        }

        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(host, port));
        running = true;

        listenerThread = new Thread(this::listen, "Votifier-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        NeoLog.info(LOGGER, LogCategory.VOTIFIER, "Votifier listener started on {}:{}", host, port);
        NeoLog.info(LOGGER, LogCategory.VOTIFIER, "Votifier V1 public key (paste into your vote site's panel):");
        NeoLog.info(LOGGER, LogCategory.VOTIFIER, keyManager.getPublicKeyBase64());
        NeoLog.info(LOGGER, LogCategory.VOTIFIER, "Votifier V2 token (paste wherever the vote site asks for a NuVotifier token): {}", v2Token);
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            NeoLog.debug(LOGGER, LogCategory.VOTIFIER, "Error closing Votifier server socket", e);
        }
        if (listenerThread != null) listenerThread.interrupt();
        NeoLog.info(LOGGER, LogCategory.VOTIFIER, "Votifier listener stopped");
    }

    public boolean isRunning() { return running; }
    public String getPublicKeyBase64() { return keyManager.getPublicKeyBase64(); }
    public String getV2Token() { return v2Token; }

    private void listen() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(10_000);
                Thread handler = new Thread(() -> handleConnection(socket), "Votifier-Handler");
                handler.setDaemon(true);
                handler.start();
            } catch (IOException e) {
                if (running) {
                    NeoLog.debug(LOGGER, LogCategory.VOTIFIER, "Error accepting Votifier connection", e);
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            String challenge = UUID.randomUUID().toString();
            OutputStream out = socket.getOutputStream();
            out.write(("VOTIFIER 2 " + challenge + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();

            InputStream in = socket.getInputStream();
            int firstByte = in.read();
            int secondByte = in.read();
            if (firstByte == -1 || secondByte == -1) return;

            if (firstByte == 0x73 && secondByte == 0x3A) {
                handleV2(in, out, challenge);
            } else {
                handleV1(in, firstByte, secondByte);
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.VOTIFIER, "Error handling Votifier connection", e);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
                // connection already gone — nothing to clean up
            }
        }
    }

    private void handleV1(InputStream in, int firstByte, int secondByte) throws Exception {
        byte[] block = new byte[256];
        block[0] = (byte) firstByte;
        block[1] = (byte) secondByte;
        int offset = 2;
        while (offset < 256) {
            int read = in.read(block, offset, 256 - offset);
            if (read == -1) throw new IOException("Unexpected end of stream reading V1 vote block");
            offset += read;
        }

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, keyManager.getKeyPair().getPrivate());
        byte[] decrypted = cipher.doFinal(block);
        String message = new String(decrypted, StandardCharsets.UTF_8).trim();

        String[] lines = message.split("\n");
        if (lines.length >= 5 && "VOTE".equals(lines[0])) {
            Vote vote = new Vote(lines[1], lines[2], lines[3], lines[4]);
            NeoLog.info(LOGGER, LogCategory.VOTIFIER, "Received V1 vote from {} for player {}", vote.serviceName(), vote.username());
            NeoForge.EVENT_BUS.post(new PlayerVoteEvent(vote));
        } else {
            NeoLog.debug(LOGGER, LogCategory.VOTIFIER, "Invalid V1 vote message: {}", message);
        }
    }

    private void handleV2(InputStream in, OutputStream out, String challenge) throws Exception {
        int lengthHigh = in.read();
        int lengthLow = in.read();
        if (lengthHigh == -1 || lengthLow == -1) throw new IOException("Unexpected end of stream reading V2 length");
        int length = (lengthHigh << 8) | lengthLow;

        byte[] payload = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(payload, offset, length - offset);
            if (read == -1) throw new IOException("Unexpected end of stream reading V2 payload");
            offset += read;
        }

        String json = new String(payload, StandardCharsets.UTF_8);
        JsonObject message = JsonParser.parseString(json).getAsJsonObject();

        String payloadStr = message.get("payload").getAsString();
        String signature = message.get("signature").getAsString();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(v2Token.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expectedSig = mac.doFinal(payloadStr.getBytes(StandardCharsets.UTF_8));
        String expectedSigB64 = Base64.getEncoder().encodeToString(expectedSig);

        if (!expectedSigB64.equals(signature)) {
            NeoLog.debug(LOGGER, LogCategory.VOTIFIER, "Invalid V2 vote signature");
            sendV2Response(out, "error", "signature verification failed");
            return;
        }

        JsonObject voteData = JsonParser.parseString(payloadStr).getAsJsonObject();

        String voteChallenge = voteData.has("challenge") ? voteData.get("challenge").getAsString() : "";
        if (!challenge.equals(voteChallenge)) {
            NeoLog.debug(LOGGER, LogCategory.VOTIFIER, "Invalid V2 vote challenge");
            sendV2Response(out, "error", "challenge verification failed");
            return;
        }

        Vote vote = new Vote(
            voteData.get("serviceName").getAsString(),
            voteData.get("username").getAsString(),
            voteData.has("address") ? voteData.get("address").getAsString() : "",
            voteData.has("timestamp") ? voteData.get("timestamp").getAsString() : "");
        NeoLog.info(LOGGER, LogCategory.VOTIFIER, "Received V2 vote from {} for player {}", vote.serviceName(), vote.username());
        sendV2Response(out, "ok", null);
        NeoForge.EVENT_BUS.post(new PlayerVoteEvent(vote));
    }

    private void sendV2Response(OutputStream out, String status, String error) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("status", status);
        if (error != null) response.addProperty("error", error);
        out.write((response + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private JsonObject getVotifierConfig() {
        try {
            JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.VOTIFIER_CONFIG);
            return root.has("votifier") ? root.getAsJsonObject("votifier") : null;
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.VOTIFIER, "Failed to read votifier.json — using defaults", e);
            return null;
        }
    }

    /** Persists an auto-generated V2 token back into votifier.json so it survives restarts. */
    private void saveGeneratedToken(String token) {
        try {
            JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.VOTIFIER_CONFIG);
            JsonObject votifier = root.has("votifier") ? root.getAsJsonObject("votifier") : new JsonObject();
            votifier.addProperty("v2Token", token);
            root.add("votifier", votifier);
            ConfigManager.getInstance().saveConfig(ConfigManager.VOTIFIER_CONFIG, root);
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.VOTIFIER, "Failed to save auto-generated Votifier V2 token — it will regenerate every restart until this is fixed", e);
        }
    }
}
