package com.zerog.neoessentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.util.Map;

public final class ColorService {
    private static boolean isColorCode(char c){ return "0123456789abcdef".indexOf(c) >= 0; }
    private static boolean isFormatCode(char c){ return "klmnor".indexOf(c) >= 0; }
    private static boolean isRgbLead(String s, int i){
        if (i + 13 >= s.length()) return false;
        if (s.charAt(i+1) != 'x') return false;
        for (int k = 2; k < 14; k += 2) if (s.charAt(i+k) != '§') return false;
        return true;
    }
    // private final boolean allowLegacyCodes; // Removed unused field
    private final boolean allowLegacyRGB;
    private final ColorPermission permission;

    public ColorService(Map<String, String> themeColors, boolean allowLegacyCodes, boolean allowLegacyRGB, ColorPermission permission) {
    // Removed themeColors
    this.allowLegacyRGB = allowLegacyRGB;
    this.permission = permission;
    }

    public Component applyUserFormatting(ServerPlayer sender, String raw) {
        String s = raw.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                char code = Character.toLowerCase(s.charAt(i + 1));
                if (isRgbLead(s, i) && allowLegacyRGB) {
                    if (permission.permitsRgb(sender)) {
                        out.append(s, i, i + 14);
                        i += 13;
                    } else {
                        i += 13;
                    }
                } else if (isColorCode(code) ? permission.canColor(sender, code) : isFormatCode(code) ? permission.canFormat(sender, code) : false) {
                    out.append('§').append(code);
                    i++;
                } else {
                    i++;
                }
            } else {
                out.append(c);
            }
        }
        return Component.literal(out.toString()); // You may want to use Adventure's LegacyComponentSerializer if available
    }
}

