package com.zerog.neoessentials.shop;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ContextChain;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.shop.api.ShopEconomyAdapter;
import com.zerog.neoessentials.shop.api.ShopEconomyRegistry;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Paid command purchases — shared by NPC shop command listings and {@code [Command]} admin sign
 * shops. The buyer pays, then the configured commands run, either from the console (full admin
 * rights) or as the buyer themselves (only what their own permissions allow).
 *
 * <p>Every command is parsed with the source it will actually run as BEFORE any money is taken:
 * an unknown command, a typo, or (in run-as-player mode) a command the buyer has no permission
 * for refuses the purchase instead of charging for something that then does nothing.
 */
public final class CommandShopPurchase {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandShopPurchase.class);

    private CommandShopPurchase() {}

    /**
     * Charges {@code buyer} and runs {@code commands}, sending them the outcome either way.
     *
     * @param label              what's being bought, for messages (e.g. "VIP Rank")
     * @param requiredPermission permission node needed to buy, or null/blank for none
     * @return true if the buyer was charged and the commands ran
     */
    public static boolean purchase(ServerPlayer buyer, BigDecimal price, List<String> commands,
                                   boolean runAsPlayer, String requiredPermission, String label) {
        if (commands == null || commands.isEmpty()) {
            buyer.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.cmd_not_configured"));
            return false;
        }
        if (requiredPermission != null && !requiredPermission.isBlank()
                && !PermissionAPI.hasPermission(buyer.getUUID(), requiredPermission)) {
            buyer.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.cmd_no_permission", label));
            return false;
        }

        MinecraftServer server = buyer.level().getServer();
        if (server == null) return false;
        CommandSourceStack source = runAsPlayer ? buyer.createCommandSourceStack() : server.createCommandSourceStack();

        List<String> resolved = commands.stream().map(c -> resolvePlaceholders(c, buyer)).toList();
        for (String cmd : resolved) {
            if (!isRunnable(server, source, cmd)) {
                LOGGER.warn("[CommandShop] Refused '{}' for {}: command '{}' can't run as {} (unknown, incomplete, or not permitted)",
                    label, buyer.getName().getString(), cmd, runAsPlayer ? "the player" : "console");
                buyer.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.cmd_unavailable"));
                return false;
            }
        }

        ShopEconomyAdapter eco = ShopEconomyRegistry.getInstance().getAdapter();
        BigDecimal charge = price.setScale(2, RoundingMode.HALF_UP);
        if (!eco.hasBalance(buyer.getUUID(), charge)) {
            buyer.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.npc_not_enough_money", eco.format(charge)));
            return false;
        }
        if (!eco.debit(buyer.getUUID(), charge)) {
            buyer.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.payment_failed"));
            return false;
        }

        for (String cmd : resolved) {
            try {
                server.getCommands().performPrefixedCommand(source, cmd);
            } catch (Exception e) {
                LOGGER.error("[CommandShop] Command '{}' from '{}' failed for {}: {}",
                    cmd, label, buyer.getName().getString(), e.getMessage(), e);
            }
        }

        NeoLog.info(LOGGER, LogCategory.GENERAL, "[CommandShop] {} bought '{}' for {} — ran {} command(s) as {}",
            buyer.getName().getString(), label, eco.format(charge), resolved.size(), runAsPlayer ? "player" : "console");
        buyer.sendSystemMessage(MessageUtil.component("commands.neoessentials.shop.cmd_bought", label, eco.format(charge)));
        return true;
    }

    /** Fills {player}/{uuid} and drops a leading "/", the same way kit and crate commands do. */
    public static String resolvePlaceholders(String command, ServerPlayer player) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        return cmd.replace("{player}", player.getName().getString())
                  .replace("{uuid}", player.getUUID().toString());
    }

    /** Whether {@code cmd} parses to an executable command for {@code source} — vanilla's own check in Commands.performCommand. */
    private static boolean isRunnable(MinecraftServer server, CommandSourceStack source, String cmd) {
        ParseResults<CommandSourceStack> parse = server.getCommands().getDispatcher().parse(cmd, source);
        if (Commands.getParseException(parse) != null) return false;
        return ContextChain.tryFlatten(parse.getContext().build(cmd)).isPresent();
    }
}
