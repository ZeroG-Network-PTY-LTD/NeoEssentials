package com.zerog.neoessentials.vault.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.vault.api.VaultEconomy;
import com.zerog.neoessentials.vault.api.VaultServiceRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Optional;

/**
 * /vault command — mirrors Vault's /vault-info and /vault-convert for NeoForge.
 *
 * <ul>
 *   <li>{@code /vault info}         — shows active providers</li>
 *   <li>{@code /vault convert <from> <to>} — converts balances between two registered economies</li>
 * </ul>
 *
 * Requires {@code neoessentials.vault.admin} permission or OP level 3.
 */
public class VaultCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vault")
            .requires(src -> src.hasPermission(3) ||
                com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                    src.getEntity() != null ? src.getEntity().getUUID() : null,
                    "neoessentials.vault.admin"))
            .then(Commands.literal("info")
                .executes(ctx -> executeInfo(ctx.getSource())))
            .then(Commands.literal("convert")
                .then(Commands.argument("from", StringArgumentType.word())
                    .then(Commands.argument("to", StringArgumentType.word())
                        .executes(ctx -> executeConvert(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "from"),
                            StringArgumentType.getString(ctx, "to"))))))
            .executes(ctx -> executeInfo(ctx.getSource()))
        );
    }

    // ── /vault info ───────────────────────────────────────────────────────────

    private static int executeInfo(CommandSourceStack src) {
        VaultServiceRegistry reg = VaultServiceRegistry.getInstance();

        src.sendSuccess(() -> Component.literal("§6§l=== NeoEssentials Vault API ==="), false);

        // Economy
        String econList = buildProviderList(reg.getEconomyProviders(),
            r -> r.provider.getName() + " [" + r.registeredBy + "]");
        Optional<VaultEconomy> eco = reg.getEconomy();
        src.sendSuccess(() -> Component.literal(String.format(
            "§eEconomy:    §f%s §7[all: %s]",
            eco.map(VaultEconomy::getName).orElse("§cnone"),
            econList.isEmpty() ? "none" : econList)), false);

        // Permission
        String permList = buildProviderList(reg.getPermissionProviders(),
            r -> r.provider.getName() + " [" + r.registeredBy + "]");
        src.sendSuccess(() -> Component.literal(String.format(
            "§ePermission: §f%s §7[all: %s]",
            reg.getPermission().map(p -> p.getName()).orElse("§cnone"),
            permList.isEmpty() ? "none" : permList)), false);

        // Chat
        String chatList = buildProviderList(reg.getChatProviders(),
            r -> r.provider.getName() + " [" + r.registeredBy + "]");
        src.sendSuccess(() -> Component.literal(String.format(
            "§eChat:       §f%s §7[all: %s]",
            reg.getChat().map(c -> c.getName()).orElse("§cnone"),
            chatList.isEmpty() ? "none" : chatList)), false);

        return 1;
    }

    // ── /vault convert <from> <to> ────────────────────────────────────────────

    private static int executeConvert(CommandSourceStack src, String fromName, String toName) {
        var providers = VaultServiceRegistry.getInstance().getEconomyProviders();

        if (providers.size() < 2) {
            src.sendFailure(Component.literal("§cYou need at least 2 economy providers registered to convert."));
            return 0;
        }

        VaultEconomy from = null, to = null;
        StringBuilder nameList = new StringBuilder();
        for (var reg : providers) {
            String n = reg.provider.getName().replace(" ", "");
            if (n.equalsIgnoreCase(fromName)) from = reg.provider;
            if (n.equalsIgnoreCase(toName))   to   = reg.provider;
            if (nameList.length() > 0) nameList.append(", ");
            nameList.append(n);
        }

        if (from == null) {
            src.sendFailure(Component.literal("§cEconomy '" + fromName + "' not found. Available: " + nameList));
            return 0;
        }
        if (to == null) {
            src.sendFailure(Component.literal("§cEconomy '" + toName + "' not found. Available: " + nameList));
            return 0;
        }

        final VaultEconomy fromFinal = from;
        final VaultEconomy toFinal   = to;

        src.sendSuccess(() -> Component.literal("§eConverting balances from §f" +
            fromFinal.getName() + " §eto §f" + toFinal.getName() + "§e... (this may take a moment)"), false);

        // Run on server thread — no offline player scanning needed in NeoForge
        // We iterate all known accounts from the economy
        int[] count = {0};
        try {
            net.minecraft.server.MinecraftServer server =
                net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                    java.util.UUID id = player.getUUID();
                    if (!fromFinal.hasAccount(id)) continue;
                    if (!toFinal.hasAccount(id))   toFinal.createPlayerAccount(id);
                    double diff = fromFinal.getBalance(id) - toFinal.getBalance(id);
                    if (diff > 0)       toFinal.depositPlayer(id, diff);
                    else if (diff < 0)  toFinal.withdrawPlayer(id, -diff);
                    count[0]++;
                }
            }
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cConversion failed: " + e.getMessage()));
            return 0;
        }

        final int converted = count[0];
        src.sendSuccess(() -> Component.literal(
            "§aConversion complete. §f" + converted + " §aaccount(s) processed. Verify data before use."), false);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface ProviderLabel<T> { String label(T t); }

    private static <T> String buildProviderList(
            Collection<T> regs, ProviderLabel<T> labeler) {
        StringBuilder sb = new StringBuilder();
        for (T r : regs) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(labeler.label(r));
        }
        return sb.toString();
    }
}

