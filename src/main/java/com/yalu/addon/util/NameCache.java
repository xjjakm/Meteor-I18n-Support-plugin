package com.yalu.addon.util;

import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Snapshots original (English) names before translation mutates them.
 * Translation mutates Category#name, Tab#name and SettingGroup#name for display,
 * which would otherwise break key generation that reads these fields.
 *
 * Uses IdentityHashMap (reference-based) instead of HashMap (hashCode-based)
 * because Category may override hashCode/equals based on the mutable name field.
 * Once a name is cached, it must never be re-computed from the translated value.
 */
public final class NameCache {
    private static final Map<Category, String> CATEGORY_ORIGINAL =
        Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<Tab, String> TAB_ORIGINAL =
        Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<SettingGroup, String> GROUP_ORIGINAL =
        Collections.synchronizedMap(new IdentityHashMap<>());

    private NameCache() {}

    /** Returns original category name, recording on first observation. */
    public static String category(Category category) {
        String cached = CATEGORY_ORIGINAL.get(category);
        if (cached != null) return cached;
        synchronized (CATEGORY_ORIGINAL) {
            cached = CATEGORY_ORIGINAL.get(category);
            if (cached == null) {
                cached = category.name;
                CATEGORY_ORIGINAL.put(category, cached);
            }
        }
        return cached;
    }

    /** Returns original tab name, recording on first observation. */
    public static String tab(Tab tab) {
        String cached = TAB_ORIGINAL.get(tab);
        if (cached != null) return cached;
        synchronized (TAB_ORIGINAL) {
            cached = TAB_ORIGINAL.get(tab);
            if (cached == null) {
                cached = tab.name;
                TAB_ORIGINAL.put(tab, cached);
            }
        }
        return cached;
    }

    /** Returns original group name, recording on first observation. */
    public static String group(SettingGroup group) {
        String cached = GROUP_ORIGINAL.get(group);
        if (cached != null) return cached;
        synchronized (GROUP_ORIGINAL) {
            cached = GROUP_ORIGINAL.get(group);
            if (cached == null) {
                cached = group.name;
                GROUP_ORIGINAL.put(group, cached);
            }
        }
        return cached;
    }
}
