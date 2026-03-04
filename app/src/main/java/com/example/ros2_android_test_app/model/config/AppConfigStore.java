package com.example.ros2_android_test_app.model.config;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AppConfigStore {
    private static final String PREF_NAME = "app_config_store";
    private static final String KEY_CONFIGS = "configs_json";
    private static final String KEY_CURRENT_ID = "current_config_id";

    private static final Gson gson = new Gson();
    private static final Type listType = new TypeToken<List<AppConfig>>() {}.getType();

    public static class AppConfig {
        public String id;
        public String name;

        public AppConfig(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static List<AppConfig> loadConfigs(Context context) {
        if (context == null) return new ArrayList<>();
        String json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CONFIGS, "[]");
        try {
            List<AppConfig> list = gson.fromJson(json, listType);
            if (list == null) return new ArrayList<>();
            return list;
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }

    public static void saveConfigs(Context context, List<AppConfig> configs) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CONFIGS, gson.toJson(configs))
                .commit();
    }

    public static String getCurrentConfigId(Context context) {
        if (context == null) return null;
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CURRENT_ID, null);
    }

    public static void setCurrentConfigId(Context context, String id) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_ID, id)
                .commit();
    }

    public static AppConfig ensureDefault(Context context, String defaultName) {
        List<AppConfig> list = loadConfigs(context);
        if (list.isEmpty()) {
            AppConfig cfg = new AppConfig(UUID.randomUUID().toString(),
                    defaultName == null || defaultName.trim().isEmpty() ? "New Config" : defaultName.trim());
            list.add(cfg);
            saveConfigs(context, list);
            setCurrentConfigId(context, cfg.id);
            return cfg;
        }

        String currentId = getCurrentConfigId(context);
        if (currentId == null || findById(list, currentId) == null) {
            AppConfig first = list.get(0);
            setCurrentConfigId(context, first.id);
            return first;
        }

        return findById(list, currentId);
    }

    public static AppConfig addConfig(Context context, String name) {
        List<AppConfig> list = loadConfigs(context);
        AppConfig cfg = new AppConfig(UUID.randomUUID().toString(), uniqueName(list, name));
        list.add(cfg);
        saveConfigs(context, list);
        setCurrentConfigId(context, cfg.id);
        return cfg;
    }

    public static AppConfig renameConfig(Context context, String id, String newName) {
        List<AppConfig> list = loadConfigs(context);
        AppConfig cfg = findById(list, id);
        if (cfg == null) return null;
        String trimmed = newName == null ? "" : newName.trim();
        if (trimmed.isEmpty()) return cfg;

        String unique = trimmed;
        int suffix = 2;
        while (existsName(list, unique, id)) {
            unique = trimmed + " " + suffix;
            suffix++;
        }
        cfg.name = unique;
        saveConfigs(context, list);
        return cfg;
    }

    public static AppConfig deleteConfig(Context context, String id) {
        List<AppConfig> list = loadConfigs(context);
        AppConfig target = findById(list, id);
        if (target == null) return ensureDefault(context, "New Config");

        if (list.size() <= 1) {
            return target;
        }

        list.remove(target);
        saveConfigs(context, list);
        WidgetConfigStore.clearForConfig(context, id);

        String currentId = getCurrentConfigId(context);
        if (id != null && id.equals(currentId)) {
            AppConfig next = list.get(0);
            setCurrentConfigId(context, next.id);
            return next;
        }

        return findById(list, getCurrentConfigId(context));
    }

    public static AppConfig getCurrentConfig(Context context) {
        List<AppConfig> list = loadConfigs(context);
        String id = getCurrentConfigId(context);
        AppConfig cfg = findById(list, id);
        if (cfg != null) return cfg;
        return ensureDefault(context, "New Config");
    }

    private static AppConfig findById(List<AppConfig> list, String id) {
        if (id == null) return null;
        for (AppConfig c : list) {
            if (id.equals(c.id)) return c;
        }
        return null;
    }

    private static boolean existsName(List<AppConfig> list, String name, String excludeId) {
        for (AppConfig c : list) {
            if (c.name == null) continue;
            if (c.name.equals(name) && (excludeId == null || !excludeId.equals(c.id))) return true;
        }
        return false;
    }

    private static String uniqueName(List<AppConfig> list, String baseName) {
        String base = (baseName == null || baseName.trim().isEmpty()) ? "New Config" : baseName.trim();
        if (!existsName(list, base, null)) return base;
        int i = 2;
        while (existsName(list, base + " " + i, null)) i++;
        return base + " " + i;
    }
}

