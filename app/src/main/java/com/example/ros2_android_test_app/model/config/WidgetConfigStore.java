package com.example.ros2_android_test_app.model.config;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WidgetConfigStore {
    private static final String TAG = "WidgetStore";
    private static final String PREF_NAME = "widget_config_store";
    private static final String KEY_WIDGETS_JSON = "widgets_json"; // 旧版单配置存储
    private static final String KEY_WIDGETS_BY_CONFIG_JSON = "widgets_by_config_json"; // 新版多配置存储
    private static final String FALLBACK_CONFIG_ID = "default";

    private static final Gson gson = new Gson();
    private static final Type listType = new TypeToken<List<WidgetConfig>>() {}.getType();
    private static final Type mapType = new TypeToken<Map<String, List<WidgetConfig>>>() {}.getType();

    public static List<WidgetConfig> load(Context context) {
        return load(context, resolveCurrentConfigId(context));
    }

    public static List<WidgetConfig> load(Context context, String configId) {
        if (context == null) return new ArrayList<>();
        String effectiveConfigId = normalizeConfigId(configId);

        Map<String, List<WidgetConfig>> all = readAllConfigWidgets(context);
        List<WidgetConfig> list = all.get(effectiveConfigId);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    public static boolean save(Context context, List<WidgetConfig> widgets) {
        return save(context, resolveCurrentConfigId(context), widgets);
    }

    public static boolean save(Context context, String configId, List<WidgetConfig> widgets) {
        if (context == null) return false;

        String effectiveConfigId = normalizeConfigId(configId);
        Map<String, List<WidgetConfig>> all = readAllConfigWidgets(context);
        all.put(effectiveConfigId, widgets == null ? new ArrayList<>() : new ArrayList<>(widgets));

        boolean success = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_WIDGETS_BY_CONFIG_JSON, gson.toJson(all))
                .commit();

        Log.d(TAG, "Save success: " + success + ", config=" + effectiveConfigId + ", count=" + (widgets == null ? 0 : widgets.size()));
        return success;
    }

    public static WidgetConfig addDefaultJoystick(Context context) {
        String configId = resolveCurrentConfigId(context);
        List<WidgetConfig> widgets = load(context, configId);
        WidgetConfig cfg = new WidgetConfig(UUID.randomUUID().toString(), "Joystick");
        cfg.name = generateUniqueName(widgets, "Joystick");
        widgets.add(cfg);
        save(context, configId, widgets);
        return cfg;
    }

    public static WidgetConfig addDefaultButton(Context context) {
        String configId = resolveCurrentConfigId(context);
        List<WidgetConfig> widgets = load(context, configId);
        WidgetConfig cfg = new WidgetConfig(UUID.randomUUID().toString(), "Button");
        cfg.name = generateUniqueName(widgets, "Button");
        widgets.add(cfg);
        save(context, configId, widgets);
        return cfg;
    }

    public static WidgetConfig addDefaultLabel(Context context) {
        String configId = resolveCurrentConfigId(context);
        List<WidgetConfig> widgets = load(context, configId);
        WidgetConfig cfg = new WidgetConfig(UUID.randomUUID().toString(), "Label");
        cfg.name = generateUniqueName(widgets, "Label");
        widgets.add(cfg);
        save(context, configId, widgets);
        return cfg;
    }

    public static WidgetConfig addDefaultMap(Context context) {
        String configId = resolveCurrentConfigId(context);
        List<WidgetConfig> widgets = load(context, configId);
        WidgetConfig cfg = new WidgetConfig(UUID.randomUUID().toString(), "Map");
        cfg.name = generateUniqueName(widgets, "Map");
        widgets.add(cfg);
        save(context, configId, widgets);
        return cfg;
    }

    private static String generateUniqueName(List<WidgetConfig> widgets, String baseName) {
        int maxNum = -1;
        boolean baseExists = false;

        for (WidgetConfig w : widgets) {
            if (w.name == null) continue;
            if (w.name.equals(baseName)) {
                baseExists = true;
                if (maxNum < 0) maxNum = 0;
            } else if (w.name.startsWith(baseName + " ")) {
                try {
                    int num = Integer.parseInt(w.name.substring(baseName.length() + 1));
                    if (num > maxNum) maxNum = num;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (!baseExists) return baseName;
        return baseName + " " + (maxNum + 1);
    }

    public static WidgetConfig findById(Context context, String id) {
        for (WidgetConfig cfg : load(context)) {
            if (cfg.id.equals(id)) return cfg;
        }
        return null;
    }

    public static void upsert(Context context, WidgetConfig updated) {
        String configId = resolveCurrentConfigId(context);
        List<WidgetConfig> widgets = load(context, configId);
        boolean found = false;
        for (int i = 0; i < widgets.size(); i++) {
            if (widgets.get(i).id.equals(updated.id)) {
                widgets.set(i, updated);
                found = true;
                break;
            }
        }
        if (!found) widgets.add(updated);
        save(context, configId, widgets);
    }

    public static void deleteById(Context context, String id) {
        String configId = resolveCurrentConfigId(context);
        List<WidgetConfig> widgets = load(context, configId);
        Iterator<WidgetConfig> it = widgets.iterator();
        while (it.hasNext()) {
            if (it.next().id.equals(id)) {
                it.remove();
                break;
            }
        }
        save(context, configId, widgets);
    }

    public static void clearAll(Context context) {
        clearAll(context, resolveCurrentConfigId(context));
    }

    public static void clearAll(Context context, String configId) {
        save(context, configId, new ArrayList<>());
    }

    public static void clearForConfig(Context context, String configId) {
        if (context == null) return;

        String effectiveConfigId = normalizeConfigId(configId);
        Map<String, List<WidgetConfig>> all = readAllConfigWidgets(context);
        all.remove(effectiveConfigId);

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_WIDGETS_BY_CONFIG_JSON, gson.toJson(all))
                .commit();
    }

    private static String resolveCurrentConfigId(Context context) {
        if (context == null) return FALLBACK_CONFIG_ID;
        AppConfigStore.AppConfig current = AppConfigStore.getCurrentConfig(context);
        if (current != null && current.id != null && !current.id.trim().isEmpty()) {
            return current.id;
        }
        return FALLBACK_CONFIG_ID;
    }

    private static String normalizeConfigId(String configId) {
        if (configId == null || configId.trim().isEmpty()) return FALLBACK_CONFIG_ID;
        return configId;
    }

    private static Map<String, List<WidgetConfig>> readAllConfigWidgets(Context context) {
        String mapJson = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_WIDGETS_BY_CONFIG_JSON, null);

        if (mapJson != null && !mapJson.trim().isEmpty()) {
            try {
                Map<String, List<WidgetConfig>> map = gson.fromJson(mapJson, mapType);
                return map == null ? new HashMap<>() : map;
            } catch (Exception ignore) {
            }
        }

        // 兼容旧版：将单配置 widgets_json 迁移到当前配置ID桶
        String legacyJson = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_WIDGETS_JSON, "[]");

        List<WidgetConfig> legacyList;
        try {
            List<WidgetConfig> parsed = gson.fromJson(legacyJson, listType);
            legacyList = parsed == null ? new ArrayList<>() : parsed;
        } catch (Exception e) {
            legacyList = new ArrayList<>();
        }

        Map<String, List<WidgetConfig>> migrated = new HashMap<>();
        if (!legacyList.isEmpty()) {
            migrated.put(resolveCurrentConfigId(context), new ArrayList<>(legacyList));
        }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_WIDGETS_BY_CONFIG_JSON, gson.toJson(migrated))
                .remove(KEY_WIDGETS_JSON)
                .commit();

        return migrated;
    }
}
