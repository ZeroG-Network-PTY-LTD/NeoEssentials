package com.zerog.neoessentials.vault.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.vault.api.VaultEconomy;
import com.zerog.neoessentials.vault.api.VaultServiceRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

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
        if (!com.zerog.neoessentials.config.ConfigManager.isVaultModuleEnabled()) {
            return;
        }
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("vault")) {
            return;
        }

        dispatcher.register(Commands.literal("vault")
            .requires(src -> src.hasPermission(3) ||
                com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                    src.getEntity() != null ? src.getEntity().getUUID() : null,
                    "neoessentials.vault.admin"))
            .then(Commands.literal("info")
                .executes(ctx -> executeInfo(ctx.getSource())))
            .then(Commands.literal("convert")
                .then(Commands.argument("from", StringArgumentType.word())
                    .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        VaultServiceRegistry.getInstance().getEconomyProviders().stream()
                            .map(r -> r.provider.getName().replace(" ", "")), b))
                    .then(Commands.argument("to", StringArgumentType.word())
                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            VaultServiceRegistry.getInstance().getEconomyProviders().stream()
                                .map(r -> r.provider.getName().replace(" ", "")), b))
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

        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.vault.header"), false);

        // Economy
        String econList = buildProviderList(reg.getEconomyProviders(),
            r -> r.provider.getName() + " [" + r.registeredBy + "]");
        Optional<VaultEconomy> eco = reg.getEconomy();
        final String econName = eco.map(VaultEconomy::getName).orElse("§cnone");
        final String econAll = econList.isEmpty() ? "none" : econList;
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.vault.economy_line", econName, econAll), false);

        // Permission
        String permList = buildProviderList(reg.getPermissionProviders(),
            r -> r.provider.getName() + " [" + r.registeredBy + "]");
        final String permName = reg.getPermission().map(p -> p.getName()).orElse("§cnone");
        final String permAll = permList.isEmpty() ? "none" : permList;
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.vault.permission_line", permName, permAll), false);

        // Chat
        String chatList = buildProviderList(reg.getChatProviders(),
            r -> r.provider.getName() + " [" + r.registeredBy + "]");
        final String chatName = reg.getChat().map(c -> c.getName()).orElse("§cnone");
        final String chatAll = chatList.isEmpty() ? "none" : chatList;
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.vault.chat_line", chatName, chatAll), false);

        return 1;
    }

    // ── /vault convert <from> <to> ────────────────────────────────────────────

    private static int executeConvert(CommandSourceStack src, String fromName, String toName) {
        var providers = VaultServiceRegistry.getInstance().getEconomyProviders();

        if (providers.size() < 2) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.vault.convert_need_two"));
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
            final String fFromName = fromName, fNameList = nameList.toString();
            src.sendFailure(MessageUtil.component("commands.neoessentials.vault.convert_economy_not_found", fFromName, fNameList));
            return 0;
        }
        if (to == null) {
            final String fToName = toName, fNameList = nameList.toString();
            src.sendFailure(MessageUtil.component("commands.neoessentials.vault.convert_economy_not_found", fToName, fNameList));
            return 0;
        }

        final VaultEconomy fromFinal = from;
        final VaultEconomy toFinal   = to;

        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.vault.convert_progress",
            fromFinal.getName(), toFinal.getName()), false);

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
            final String errMsg = e.getMessage();
            src.sendFailure(MessageUtil.component("commands.neoessentials.vault.convert_failed", errMsg));
            return 0;
        }

        final int converted = count[0];
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.vault.convert_complete", converted), false);
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

