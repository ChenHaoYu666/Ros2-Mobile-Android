package com.example.ros2_android_test_app;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.ros2_android_test_app.model.config.AppConfigStore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Ros2ConfigManager {
    private static final String PREF_NAME = "ros2_config";
    private static final String KEY_DOMAIN_ID = "ros_domain_id"; // 旧版全局 key
    private static final String KEY_DOMAIN_BY_CONFIG_JSON = "ros_domain_by_config_json"; // 新版按配置
    private static final int DEFAULT_DOMAIN_ID = 0;

    private static final Gson gson = new Gson();
    private static final Type mapType = new TypeToken<Map<String, Integer>>() {}.getType();

    private Ros2ConfigManager() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getDomainId(Context context) {
        if (context == null) {
            return DEFAULT_DOMAIN_ID;
        }
        String configId = resolveCurrentConfigId(context);
        return getDomainIdForConfig(context, configId);
    }

    public static int getDomainIdForConfig(Context context, String configId) {
        if (context == null) {
            return DEFAULT_DOMAIN_ID;
        }

        String id = normalizeConfigId(configId);
        Map<String, Integer> map = readDomainMap(context);
        Integer value = map.get(id);
        return value == null ? DEFAULT_DOMAIN_ID : value;
    }

    public static void setDomainId(Context context, int domainId) {
        if (context == null) {
            return;
        }
        String configId = resolveCurrentConfigId(context);
        setDomainIdForConfig(context, configId, domainId);
    }

    public static void setDomainIdForConfig(Context context, String configId, int domainId) {
        if (context == null) {
            return;
        }

        String id = normalizeConfigId(configId);
        Map<String, Integer> map = readDomainMap(context);
        map.put(id, domainId);

        getPrefs(context).edit()
                .putString(KEY_DOMAIN_BY_CONFIG_JSON, gson.toJson(map))
                .putInt(KEY_DOMAIN_ID, domainId) // 兼容旧逻辑
                .apply();
    }

    private static Map<String, Integer> readDomainMap(Context context) {
        String json = getPrefs(context).getString(KEY_DOMAIN_BY_CONFIG_JSON, null);
        if (json != null && !json.trim().isEmpty()) {
            try {
                Map<String, Integer> map = gson.fromJson(json, mapType);
                if (map != null) return map;
            } catch (Exception ignored) {
            }
        }

        // 兼容旧版单值：迁移到当前配置
        int legacy = getPrefs(context).getInt(KEY_DOMAIN_ID, DEFAULT_DOMAIN_ID);
        Map<String, Integer> migrated = new HashMap<>();
        migrated.put(resolveCurrentConfigId(context), legacy);

        getPrefs(context).edit()
                .putString(KEY_DOMAIN_BY_CONFIG_JSON, gson.toJson(migrated))
                .apply();

        return migrated;
    }

    private static String resolveCurrentConfigId(Context context) {
        AppConfigStore.AppConfig cfg = AppConfigStore.getCurrentConfig(context);
        if (cfg != null && cfg.id != null && !cfg.id.trim().isEmpty()) {
            return cfg.id;
        }
        return "default";
    }

    private static String normalizeConfigId(String configId) {
        if (configId == null || configId.trim().isEmpty()) return "default";
        return configId;
    }
}
