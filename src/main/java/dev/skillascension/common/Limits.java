package dev.skillascension.common;

import dev.skillascension.AscensionConfig;

import java.util.regex.Pattern;

/** Skill tree limitation helpers shared by client and server. */
public final class Limits {
    private static String patternSource;
    private static Pattern pattern;

    private Limits() {
    }

    /** The limit for a tag after the singleplayer bonus. */
    public static int limit(String tag, int base, boolean singleplayer) {
        if (base > 0 && singleplayer && AscensionConfig.SINGLEPLAYER_LIMIT_TAGS.get().contains(tag)) {
            return base + AscensionConfig.SINGLEPLAYER_LIMIT_BONUS.get();
        }
        return base;
    }

    public static boolean hidden(String tag) {
        String src = AscensionConfig.HIDDEN_LIMIT_PATTERN.get();
        if (src == null || src.isEmpty()) return false;
        if (!src.equals(patternSource)) {
            patternSource = src;
            try {
                pattern = Pattern.compile(src);
            } catch (Exception e) {
                pattern = null;
            }
        }
        return pattern != null && pattern.matcher(tag).matches();
    }
}
