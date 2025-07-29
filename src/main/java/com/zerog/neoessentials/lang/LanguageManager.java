package com.zerog.neoessentials.lang;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * Language manager for multi-language support
 * Supports EssentialsX-style language files
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class LanguageManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageManager.class);
    
    private final Map<String, Properties> languageFiles = new HashMap<>();
    private String defaultLocale = "en_us";
    
    public LanguageManager() {
        loadLanguageFiles();
    }
    
    private void loadLanguageFiles() {
        String[] locales = {"en_us", "de_de", "es_es", "fr_fr", "it_it", "pt_br", "ru_ru", "zh_cn"};
        
        for (String locale : locales) {
            try {
                Properties props = new Properties();
                InputStream stream = getClass().getResourceAsStream("/assets/neoessentials/lang/" + locale + ".properties");
                if (stream != null) {
                    props.load(stream);
                    languageFiles.put(locale, props);
                    stream.close();
                    LOGGER.debug("Loaded language file: {}", locale);
                } else {
                    LOGGER.warn("Language file not found: {}", locale);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load language file: {}", locale, e);
            }
        }
    }
    
    public String getMessage(String key, String locale) {
        Properties props = languageFiles.get(locale);
        if (props == null) {
            props = languageFiles.get(defaultLocale);
        }
        
        if (props != null) {
            return props.getProperty(key, "§cMissing translation: " + key);
        }
        
        return "§cMissing translation: " + key;
    }
    
    public String getMessage(String key, ServerPlayer player) {
        // TODO: Detect player's locale preference
        String locale = defaultLocale; // For now, use default
        return getMessage(key, locale);
    }
    
    public String formatMessage(String key, ServerPlayer player, Object... args) {
        String message = getMessage(key, player);
        return String.format(message, args);
    }
    
    public Component getComponent(String key, ServerPlayer player, Object... args) {
        String message = formatMessage(key, player, args);
        return Component.literal(message);
    }
    
    // Placeholder replacement
    public String replacePlaceholders(String message, ServerPlayer player) {
        if (player == null) return message;
        
        message = message.replace("{PLAYER}", player.getName().getString());
        message = message.replace("{DISPLAYNAME}", player.getDisplayName().getString());
        message = message.replace("{WORLD}", player.level().dimension().location().toString());
        message = message.replace("{X}", String.valueOf((int) player.getX()));
        message = message.replace("{Y}", String.valueOf((int) player.getY()));
        message = message.replace("{Z}", String.valueOf((int) player.getZ()));
        
        // TODO: Add more placeholders like {BALANCE}, {PREFIX}, {SUFFIX}, etc.
        
        return message;
    }
}
