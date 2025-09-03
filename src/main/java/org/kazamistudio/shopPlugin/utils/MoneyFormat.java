package org.kazamistudio.shopPlugin.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class MoneyFormat {
    private static final NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

    public static String format(double amount) {
        return formatter.format(amount);
    }
}
