package xyz.dkos.gaming.mindustry.translator.utils;

import java.text.MessageFormat;

import arc.Core;

/**
 * Helper class for localized string retrieval.
 */
public class BundleHelper {

    /**
     * Gets a localized string from the bundle.
     *
     * @param key  Bundle key
     * @param args Optional format arguments
     * @return Localized string
     */
    public static String get(String key, Object... args) {
        String value = Core.bundle.get(key, key);
        if (args.length > 0) {
            return MessageFormat.format(value, args);
        }
        return value;
    }
}