
package com.zerog.neoessentials.economy;
import com.zerog.neoessentials.util.DebugUtil;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import net.minecraft.network.chat.Component;

public class EconomyLocalization {
    private static final Map<String, String> translations = new HashMap<>();
    private static boolean loaded = false;

    private static void loadTranslations() {
        if (loaded) return;
        loaded = true;
        File langFile = new File("neoessentials/lang/en_us.json");
        if (langFile.exists()) {
            try (FileReader reader = new FileReader(langFile)) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> map = gson.fromJson(reader, type);
                if (map != null) translations.putAll(map);
            } catch (Exception e) {
                DebugUtil.debugStackTrace(e);
            }
        }
    }

    public static String localize(String key, Object... args) {
        loadTranslations();
        String template = translations.getOrDefault(key, key);
        try {
            return MessageFormat.format(template.replace("%s", "{0}"), args);
        } catch (Exception e) {
            return template;
        }
    }

    public static Component component(String key, Object... args) {
        // Deprecated for server-only mods: always use localize + literal
        return Component.literal(localize(key, args));
    }
}