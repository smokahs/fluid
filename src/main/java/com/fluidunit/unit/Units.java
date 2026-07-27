package com.fluidunit.unit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fluidunit.FluidUnit;

// parsing for the name=value config lists, and the amount arithmetic itself.
public final class Units {

    private Units() {}

    /** Namespace is listed but already writes the target unit. */
    public static final int NATIVE = -1;

    /** Namespace is in neither list, so its amounts are left alone. */
    public static final int UNKNOWN = 0;

    public static Map<String, String> pairs(List<? extends String> list) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String entry : list) {
            int split = entry.indexOf('=');
            if (split <= 0) {
                FluidUnit.LOGGER.warn("Ignoring config entry '{}', expected name=value", entry);
                continue;
            }
            out.put(entry.substring(0, split).trim(), entry.substring(split + 1).trim());
        }
        return out;
    }

    public static Map<String, Integer> intPairs(List<? extends String> list) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : pairs(list).entrySet()) {
            try {
                int value = Integer.parseInt(entry.getValue());
                if (value > 0) {
                    out.put(entry.getKey(), value);
                } else {
                    FluidUnit.LOGGER.warn("Ignoring unit '{}={}', must be positive", entry.getKey(), entry.getValue());
                }
            } catch (NumberFormatException notANumber) {
                FluidUnit.LOGGER.warn("Ignoring unit '{}={}', not a number", entry.getKey(), entry.getValue());
            }
        }
        return out;
    }

    // 90 to 144 is a multiply by eight fifths. every standard tinkers amount is a multiple of five
    // and every gregtech one a multiple of sixteen, so the usual values land exactly; anything else
    // rounds to nearest and gets reported.
    public static int scale(int amount, int from, int to) {
        if (from == to || from <= 0) {
            return amount;
        }
        long scaled = (long) amount * to;
        long rounded = (scaled + from / 2L) / from;
        return (int) Math.min(rounded, Integer.MAX_VALUE);
    }

    public static boolean exact(int amount, int from, int to) {
        return from == to || from <= 0 || ((long) amount * to) % from == 0;
    }
}
