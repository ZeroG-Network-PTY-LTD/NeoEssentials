package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.i18n.CustomLanguageManager;
import com.zerog.neoessentials.i18n.CustomLanguageManager.LanguageFileInfo;
import com.zerog.neoessentials.i18n.CustomLanguageManager.ValidationReport;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.ResourceUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

/**
 * Commands for managing custom language files and translations
 */
public class LanguageCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("language")
                .requires(source -> source.hasPermission(4)) // Op level 4
                .then(Commands.literal("list")
                    .executes(LanguageCommand::listLanguages))
                .then(Commands.literal("reload")
                    .executes(LanguageCommand::reloadLanguages))
                .then(Commands.literal("stats")
                    .executes(LanguageCommand::showStats))
                .then(Commands.literal("template")
                    .then(Commands.argument("languageCode", StringArgumentType.word())
                        .executes(LanguageCommand::generateTemplate)))
                .then(Commands.literal("exportmissing")
                    .executes(LanguageCommand::exportMissingKeys))
                .then(Commands.literal("clearmissing")
                    .executes(LanguageCommand::clearMissingKeys))
                .then(Commands.literal("info")
                    .executes(LanguageCommand::showInfo))
                // --- New subcommands ---
                .then(Commands.literal("validate")
                    .then(Commands.argument("languageCode", StringArgumentType.word())
                        .executes(LanguageCommand::validateLanguage)))
                .then(Commands.literal("regenerate")
                    .then(Commands.argument("languageCode", StringArgumentType.word())
                        .executes(LanguageCommand::regenerateLanguage)))
                .then(Commands.literal("override")
                    .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.word())
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(LanguageCommand::overrideSet))))
                    .then(Commands.literal("get")
                        .then(Commands.argument("key", StringArgumentType.word())
                            .executes(LanguageCommand::overrideGet)))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("key", StringArgumentType.word())
                            .executes(LanguageCommand::overrideRemove)))
                    .then(Commands.literal("list")
                        .executes(LanguageCommand::overrideList))
                    .then(Commands.literal("clear")
                        .executes(LanguageCommand::overrideClear))
                    .then(Commands.literal("reload")
                        .executes(LanguageCommand::overrideReload)))
        );

        LOGGER.info("Language command registered");
    }

    /**
     * List all available custom languages
     */
    private static int listLanguages(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        CustomLanguageManager manager = CustomLanguageManager.getInstance();

        List<LanguageFileInfo> languages = manager.getCustomLanguages();

        if (languages.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.warning("No custom languages installed."), false);
            source.sendSuccess(() -> MessageUtil.info("To add a language, place a .json file in: neoessentials/languages/custom/"), false);
            source.sendSuccess(() -> MessageUtil.info("Use '/language template <code>' to generate a template."), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("â•â•â• Custom Languages ({0}) â•â•â•", languages.size()), false);

            for (LanguageFileInfo lang : languages) {
                String info = String.format("  Â§e%s Â§7- Â§f%s Â§7(Â§f%sÂ§7) Â§7by Â§f%s Â§7v%s",
                    lang.getLanguageCode(),
                    lang.getNativeName(),
                    lang.getEnglishName(),
                    lang.getAuthor(),
                    lang.getVersion()
                );
                source.sendSuccess(() -> MessageUtil.component(info), false);
            }
        }

        return 1;
    }

    /**
     * Reload all custom language files
     */
    private static int reloadLanguages(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        try {
            CustomLanguageManager.getInstance().reload();
            int count = CustomLanguageManager.getInstance().getCustomLanguages().size();

            source.sendSuccess(() -> MessageUtil.success("Successfully reloaded custom languages ({0} loaded).", count), true);
            LOGGER.info("Custom languages reloaded by {}", source.getTextName());
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("Failed to reload languages: {0}", e.getMessage()));
            LOGGER.error("Failed to reload languages", e);
        }

        return 1;
    }

    /**
     * Show statistics about translations
     */
    private static int showStats(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Map<String, Object> stats = CustomLanguageManager.getInstance().getStatistics();

        source.sendSuccess(() -> MessageUtil.success("â•â•â• Language System Statistics â•â•â•"), false);
        source.sendSuccess(() -> MessageUtil.info("Custom languages loaded: {0}", stats.get("customLanguagesLoaded")), false);
        source.sendSuccess(() -> MessageUtil.info("Missing keys tracked: {0}", stats.get("missingKeysTracked")), false);
        source.sendSuccess(() -> MessageUtil.info("Active admin overrides: {0}", stats.get("overrideCount")), false);
        source.sendSuccess(() -> MessageUtil.info("Custom language directory: Â§e{0}", stats.get("customLanguageDirectory")), false);
        source.sendSuccess(() -> MessageUtil.info("Template directory: Â§e{0}", stats.get("templateDirectory")), false);

        @SuppressWarnings("unchecked")
        List<String> codes = (List<String>) stats.get("languageCodes");
        if (!codes.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("Available languages: Â§e{0}", String.join(", ", codes)), false);
        }

        return 1;
    }

    /**
     * Generate a translation template for a specific language
     */
    private static int generateTemplate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String languageCode = StringArgumentType.getString(ctx, "languageCode");

        try {
            String fileName = languageCode + "_template.json";
            CustomLanguageManager.getInstance().generateTemplate(
                languageCode,
                ResourceUtil.getDataPath("languages/templates/" + fileName)
            );

            source.sendSuccess(() -> MessageUtil.success("Generated template for language: {0}", languageCode), true);
            source.sendSuccess(() -> MessageUtil.info("Template saved to: Â§eneoessentials/languages/templates/{0}", fileName), false);
            source.sendSuccess(() -> MessageUtil.info("Instructions:"), false);
            source.sendSuccess(() -> MessageUtil.info("  1. Edit the template file and translate the text"), false);
            source.sendSuccess(() -> MessageUtil.info("  2. Save it as Â§e{0}.jsonÂ§7 in Â§eneoessentials/languages/custom/", languageCode), false);
            source.sendSuccess(() -> MessageUtil.info("  3. Run Â§e/language reloadÂ§7 to load it"), false);

            LOGGER.info("Generated language template for {} by {}", languageCode, source.getTextName());
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("Failed to generate template: {0}", e.getMessage()));
            LOGGER.error("Failed to generate template for {}", languageCode, e);
        }

        return 1;
    }

    /**
     * Export missing translation keys to a file
     */
    private static int exportMissingKeys(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        try {
            int count = CustomLanguageManager.getInstance().getMissingKeys().size();

            if (count == 0) {
                source.sendSuccess(() -> MessageUtil.info("No missing translation keys tracked."), false);
                source.sendSuccess(() -> MessageUtil.info("Missing keys are tracked when translations are requested but not found."), false);
                return 1;
            }

            String fileName = "missing_keys_" + System.currentTimeMillis() + ".json";
            CustomLanguageManager.getInstance().exportMissingKeys(
                ResourceUtil.getDataPath("languages/templates/" + fileName)
            );

            source.sendSuccess(() -> MessageUtil.success("Exported {0} missing keys", count), true);
            source.sendSuccess(() -> MessageUtil.info("File: Â§eneoessentials/languages/templates/{0}", fileName), false);

            LOGGER.info("Exported {} missing keys by {}", count, source.getTextName());
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("Failed to export missing keys: {0}", e.getMessage()));
            LOGGER.error("Failed to export missing keys", e);
        }

        return 1;
    }

    /**
     * Clear missing keys tracker
     */
    private static int clearMissingKeys(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int count = CustomLanguageManager.getInstance().getMissingKeys().size();

        CustomLanguageManager.getInstance().clearMissingKeys();

        source.sendSuccess(() -> MessageUtil.success("Cleared {0} missing keys from tracker", count), true);

        return 1;
    }

    /**
     * Show general information about the language system
     */
    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> MessageUtil.success("â•â•â• NeoEssentials Language System â•â•â•"), false);
        source.sendSuccess(() -> MessageUtil.info("The language system supports custom translations."), false);
        source.sendSuccess(() -> MessageUtil.info(""), false);
        source.sendSuccess(() -> MessageUtil.info("Â§eAvailable Commands:"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language list Â§7- List all custom languages"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language reload Â§7- Reload language files"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language stats Â§7- Show statistics"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language template <code> Â§7- Generate template"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language exportmissing Â§7- Export missing keys"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language clearmissing Â§7- Clear missing keys tracker"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language validate <code> Â§7- Validate a language file"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language regenerate <code> Â§7- Regenerate from JAR (backs up first)"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language override set <key> <value> Â§7- Override a message key"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language override list Â§7- List all overrides"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language override get <key> Â§7- Show override for a key"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language override remove <key> Â§7- Remove an override"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language override clear Â§7- Clear all overrides"), false);
        source.sendSuccess(() -> MessageUtil.info("  Â§e/language override reload Â§7- Reload overrides from disk"), false);
        source.sendSuccess(() -> MessageUtil.info(""), false);
        source.sendSuccess(() -> MessageUtil.info("Â§eTo create a custom language:"), false);
        source.sendSuccess(() -> MessageUtil.info("  1. Use Â§e/language template <code>Â§7 to generate a template"), false);
        source.sendSuccess(() -> MessageUtil.info("  2. Translate the text in the template file"), false);
        source.sendSuccess(() -> MessageUtil.info("  3. Save as Â§e<code>.jsonÂ§7 in Â§eneoessentials/languages/custom/"), false);
        source.sendSuccess(() -> MessageUtil.info("  4. Run Â§e/language reload"), false);

        return 1;
    }

    // =========================================================================
    // Validate
    // =========================================================================

    /**
     * Validate a language file against the base English translations
     */
    private static int validateLanguage(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String languageCode = StringArgumentType.getString(ctx, "languageCode");

        ValidationReport report = CustomLanguageManager.getInstance().validateLanguage(languageCode);

        if (report.hasError()) {
            source.sendFailure(MessageUtil.error("Validation failed for {0}: {1}", languageCode, report.getErrorMessage()));
            return 0;
        }

        // Coverage colour: red <50%, yellow 50â€“89%, green â‰¥90%
        String coverageColor = report.getCoveragePercent() >= 90 ? "Â§a" :
                               report.getCoveragePercent() >= 50 ? "Â§e" : "Â§c";

        source.sendSuccess(() -> MessageUtil.success("â•â•â• Language Validation: {0} â•â•â•", languageCode), false);
        source.sendSuccess(() -> MessageUtil.info("Total base keys: Â§f{0}", report.getTotalKeys()), false);
        source.sendSuccess(() -> MessageUtil.info("Translated keys: Â§f{0}", report.getPresentKeys()), false);
        source.sendSuccess(() -> MessageUtil.component("  Coverage: " + coverageColor + report.getCoveragePercent() + "%"), false);
        source.sendSuccess(() -> MessageUtil.info("Missing keys: Â§f{0}", report.getMissingCount()), false);
        source.sendSuccess(() -> MessageUtil.info("Extra keys (not in base): Â§f{0}", report.getExtraCount()), false);

        if (report.getMissingCount() > 0) {
            int toShow = Math.min(report.getMissingCount(), 10);
            source.sendSuccess(() -> MessageUtil.warning("First {0} missing key(s):", toShow), false);
            for (int i = 0; i < toShow; i++) {
                final String key = report.getMissingKeys().get(i);
                source.sendSuccess(() -> MessageUtil.component("  Â§7- Â§f" + key), false);
            }
            if (report.getMissingCount() > 10) {
                source.sendSuccess(() -> MessageUtil.info("  ... and {0} more. Run /language exportmissing to export all.", report.getMissingCount() - 10), false);
            }
        }

        if (report.getCoveragePercent() < 100) {
            source.sendSuccess(() -> MessageUtil.info("Run Â§e/language regenerate {0}Â§7 to update the file from JAR.", languageCode), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("âœ“ Language file is fully up-to-date."), false);
        }

        LOGGER.info("Language validation for {} by {}: {}% coverage, {} missing, {} extra",
            languageCode, source.getTextName(), report.getCoveragePercent(),
            report.getMissingCount(), report.getExtraCount());
        return 1;
    }

    // =========================================================================
    // Regenerate
    // =========================================================================

    /**
     * Regenerate a language file from the JAR, preserving user edits, backing up first
     */
    private static int regenerateLanguage(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String languageCode = StringArgumentType.getString(ctx, "languageCode");

        try {
            int newKeys = CustomLanguageManager.getInstance().regenerate(languageCode);
            source.sendSuccess(() -> MessageUtil.success("Regenerated {0}.json from JAR. {1} new key(s) added.", languageCode, newKeys), true);
            source.sendSuccess(() -> MessageUtil.info("Your previous file was backed up to Â§e{0}.json.bakÂ§7.", languageCode), false);
            if (newKeys == 0) {
                source.sendSuccess(() -> MessageUtil.info("No new keys were needed â€” file was already up to date."), false);
            }
            LOGGER.info("Regenerated language {} by {} ({} new keys)", languageCode, source.getTextName(), newKeys);
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("Failed to regenerate {0}: {1}", languageCode, e.getMessage()));
            LOGGER.error("Failed to regenerate language {} for {}", languageCode, source.getTextName(), e);
        }
        return 1;
    }

    // =========================================================================
    // Override commands
    // =========================================================================

    private static int overrideSet(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String key = StringArgumentType.getString(ctx, "key");
        String value = StringArgumentType.getString(ctx, "value");

        CustomLanguageManager.getInstance().setOverride(key, value);
        // Also force MessageUtil to reload so the override is picked up immediately
        MessageUtil.reloadTranslations();

        source.sendSuccess(() -> MessageUtil.success("Override set: Â§e{0} Â§7â†’ Â§f{1}", key, value), true);
        LOGGER.info("{} set translation override: {} = {}", source.getTextName(), key, value);
        return 1;
    }

    private static int overrideGet(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String key = StringArgumentType.getString(ctx, "key");

        String value = CustomLanguageManager.getInstance().getOverride(key);
        if (value == null) {
            source.sendSuccess(() -> MessageUtil.warning("No override set for key: Â§f{0}", key), false);
        } else {
            source.sendSuccess(() -> MessageUtil.info("Override for Â§e{0}Â§7: Â§f{1}", key, value), false);
        }
        return 1;
    }

    private static int overrideRemove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String key = StringArgumentType.getString(ctx, "key");

        boolean removed = CustomLanguageManager.getInstance().removeOverride(key);
        if (removed) {
            MessageUtil.reloadTranslations();
            source.sendSuccess(() -> MessageUtil.success("Override removed for key: Â§f{0}", key), true);
            LOGGER.info("{} removed translation override for: {}", source.getTextName(), key);
        } else {
            source.sendSuccess(() -> MessageUtil.warning("No override found for key: Â§f{0}", key), false);
        }
        return 1;
    }

    private static int overrideList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Map<String, String> overrides = CustomLanguageManager.getInstance().getOverrides();

        if (overrides.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("No admin translation overrides are set."), false);
            source.sendSuccess(() -> MessageUtil.info("Use Â§e/language override set <key> <value>Â§7 to add one."), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("â•â•â• Translation Overrides ({0}) â•â•â•", overrides.size()), false);
            for (Map.Entry<String, String> entry : overrides.entrySet()) {
                source.sendSuccess(() -> MessageUtil.component("  Â§e" + entry.getKey() + " Â§7â†’ Â§f" + entry.getValue()), false);
            }
        }
        return 1;
    }

    private static int overrideClear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        int count = CustomLanguageManager.getInstance().getOverrides().size();
        CustomLanguageManager.getInstance().clearOverrides();
        MessageUtil.reloadTranslations();
        source.sendSuccess(() -> MessageUtil.success("Cleared {0} translation override(s).", count), true);
        LOGGER.info("{} cleared all ({}) translation overrides", source.getTextName(), count);
        return 1;
    }

    private static int overrideReload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        // Clear and re-load from disk by full reload
        CustomLanguageManager.getInstance().reload();
        int count = CustomLanguageManager.getInstance().getOverrides().size();
        source.sendSuccess(() -> MessageUtil.success("Reloaded overrides from disk. {0} override(s) active.", count), true);
        return 1;
    }
}
