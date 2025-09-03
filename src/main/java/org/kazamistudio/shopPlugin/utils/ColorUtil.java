package org.kazamistudio.shopPlugin.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {
    // Regex để tìm mã hex kiểu #RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Dịch & thành mã màu và hỗ trợ cả hex.
     */
    public static String translate(String message) {
        if (message == null) return "";

        // Đổi hex trước
        Matcher matcher = HEX_PATTERN.matcher(message);
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            ChatColor color = ChatColor.of("#" + hexCode);
            message = message.replace("&#" + hexCode, color.toString());
        }

        // Đổi &a, &b... sang màu
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}