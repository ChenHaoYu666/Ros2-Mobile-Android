package com.example.ros2_android_test_app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ros2_android_test_app.model.config.AppConfigStore;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private final MutableLiveData<String> configTitle = new MutableLiveData<>("");
    private final MutableLiveData<List<AppConfigStore.AppConfig>> configs = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> currentConfigId = new MutableLiveData<>(null);
    private final MutableLiveData<Long> configSwitchVersion = new MutableLiveData<>(0L);

    public MainViewModel(@NonNull Application application) {
        super(application);
        refreshConfigs();
    }

    public void createFirstConfig(String configName) {
        AppConfigStore.AppConfig cfg = AppConfigStore.ensureDefault(getApplication(), configName);
        refreshConfigs();
        if (cfg != null) {
            currentConfigId.postValue(cfg.id);
            configTitle.postValue(cfg.name);
        }
    }

    public void refreshConfigs() {
        List<AppConfigStore.AppConfig> list = AppConfigStore.loadConfigs(getApplication());
        configs.postValue(list);
        AppConfigStore.AppConfig current = AppConfigStore.getCurrentConfig(getApplication());
        if (current != null) {
            currentConfigId.postValue(current.id);
            configTitle.postValue(current.name);
        }
    }

    public AppConfigStore.AppConfig addConfig(String name) {
        AppConfigStore.AppConfig cfg = AppConfigStore.addConfig(getApplication(), name);
        refreshConfigs();
        markConfigSwitched();
        return cfg;
    }

    public void selectConfig(String id) {
        AppConfigStore.setCurrentConfigId(getApplication(), id);
        refreshConfigs();
        markConfigSwitched();
    }

    public AppConfigStore.AppConfig renameCurrentConfig(String newName) {
        String id = currentConfigId.getValue();
        if (id == null) return null;
        AppConfigStore.AppConfig cfg = AppConfigStore.renameConfig(getApplication(), id, newName);
        refreshConfigs();
        return cfg;
    }

    public AppConfigStore.AppConfig deleteCurrentConfig() {
        String id = currentConfigId.getValue();
        if (id == null) return null;
        AppConfigStore.AppConfig next = AppConfigStore.deleteConfig(getApplication(), id);
        refreshConfigs();
        markConfigSwitched();
        return next;
    }

    public LiveData<String> getConfigTitle() {
        return configTitle;
    }

    public LiveData<List<AppConfigStore.AppConfig>> getConfigs() {
        return configs;
    }

    public LiveData<String> getCurrentConfigId() {
        return currentConfigId;
    }

    public LiveData<Long> getConfigSwitchVersion() {
        return configSwitchVersion;
    }

    public void markConfigSwitched() {
        Long current = configSwitchVersion.getValue();
        if (current == null) current = 0L;
        configSwitchVersion.postValue(current + 1L);
    }
}
