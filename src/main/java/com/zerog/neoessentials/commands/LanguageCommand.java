package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.i18n.CustomLanguageManager;
import com.zerog.neoessentials.i18n.CustomLanguageManager.LanguageFileInfo;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
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
            source.sendSuccess(() -> MessageUtil.success("═══ Custom Languages ({0}) ═══", languages.size()), false);

            for (LanguageFileInfo lang : languages) {
                String info = String.format("  §e%s §7- §f%s §7(§f%s§7) §7by §f%s §7v%s",
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

        source.sendSuccess(() -> MessageUtil.success("═══ Language System Statistics ═══"), false);
        source.sendSuccess(() -> MessageUtil.info("Custom languages loaded: {0}", stats.get("customLanguagesLoaded")), false);
        source.sendSuccess(() -> MessageUtil.info("Missing keys tracked: {0}", stats.get("missingKeysTracked")), false);
        source.sendSuccess(() -> MessageUtil.info("Custom language directory: §e{0}", stats.get("customLanguageDirectory")), false);
        source.sendSuccess(() -> MessageUtil.info("Template directory: §e{0}", stats.get("templateDirectory")), false);

        @SuppressWarnings("unchecked")
        List<String> codes = (List<String>) stats.get("languageCodes");
        if (!codes.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("Available languages: §e{0}", String.join(", ", codes)), false);
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
                Paths.get("neoessentials", "languages", "templates", fileName)
            );

            source.sendSuccess(() -> MessageUtil.success("Generated template for language: {0}", languageCode), true);
            source.sendSuccess(() -> MessageUtil.info("Template saved to: §eneoessentials/languages/templates/{0}", fileName), false);
            source.sendSuccess(() -> MessageUtil.info("Instructions:"), false);
            source.sendSuccess(() -> MessageUtil.info("  1. Edit the template file and translate the text"), false);
            source.sendSuccess(() -> MessageUtil.info("  2. Save it as §e{0}.json§7 in §eneoessentials/languages/custom/", languageCode), false);
            source.sendSuccess(() -> MessageUtil.info("  3. Run §e/language reload§7 to load it"), false);

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
                Paths.get("neoessentials", "languages", "templates", fileName)
            );

            source.sendSuccess(() -> MessageUtil.success("Exported {0} missing keys", count), true);
            source.sendSuccess(() -> MessageUtil.info("File: §eneoessentials/languages/templates/{0}", fileName), false);

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

        source.sendSuccess(() -> MessageUtil.success("═══ NeoEssentials Language System ═══"), false);
        source.sendSuccess(() -> MessageUtil.info("The language system supports custom translations."), false);
        source.sendSuccess(() -> MessageUtil.info(""), false);
        source.sendSuccess(() -> MessageUtil.info("§eAvailable Commands:"), false);
        source.sendSuccess(() -> MessageUtil.info("  §e/language list §7- List all custom languages"), false);
        source.sendSuccess(() -> MessageUtil.info("  §e/language reload §7- Reload language files"), false);
        source.sendSuccess(() -> MessageUtil.info("  §e/language stats §7- Show statistics"), false);
        source.sendSuccess(() -> MessageUtil.info("  §e/language template <code> §7- Generate template"), false);
        source.sendSuccess(() -> MessageUtil.info("  §e/language exportmissing §7- Export missing keys"), false);
        source.sendSuccess(() -> MessageUtil.info("  §e/language clearmissing §7- Clear missing keys tracker"), false);
        source.sendSuccess(() -> MessageUtil.info(""), false);
        source.sendSuccess(() -> MessageUtil.info("§eSupported Language Codes:"), false);
        source.sendSuccess(() -> MessageUtil.info("  en_us, es_es, fr_fr, de_de, it_it, pt_br,"), false);
        source.sendSuccess(() -> MessageUtil.info("  ru_ru, ja_jp, ko_kr, zh_cn, nl_nl, etc."), false);
        source.sendSuccess(() -> MessageUtil.info(""), false);
        source.sendSuccess(() -> MessageUtil.info("§eTo create a custom language:"), false);
        source.sendSuccess(() -> MessageUtil.info("  1. Use §e/language template <code>§7 to generate a template"), false);
        source.sendSuccess(() -> MessageUtil.info("  2. Translate the text in the template file"), false);
        source.sendSuccess(() -> MessageUtil.info("  3. Save as §e<code>.json§7 in §eneoessentials/languages/custom/"), false);
        source.sendSuccess(() -> MessageUtil.info("  4. Run §e/language reload"), false);

        return 1;
    }
}
