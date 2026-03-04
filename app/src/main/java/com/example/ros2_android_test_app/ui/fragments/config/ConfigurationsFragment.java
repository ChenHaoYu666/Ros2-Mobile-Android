package com.example.ros2_android_test_app.ui.fragments.config;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ros2_android_test_app.R;
import com.example.ros2_android_test_app.model.config.AppConfigStore;
import com.example.ros2_android_test_app.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationsFragment extends Fragment {

    private MainViewModel mainViewModel;
    private TextView currentConfigText;
    private RecyclerView storedRecycler;
    private ConfigAdapter adapter;

    private final List<AppConfigStore.AppConfig> cachedConfigs = new ArrayList<>();

    public static ConfigurationsFragment newInstance() {
        return new ConfigurationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_configurations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        currentConfigText = view.findViewById(R.id.current_config_textview);
        ImageButton renameBtn = view.findViewById(R.id.current_config_rename_button);
        ImageButton deleteBtn = view.findViewById(R.id.current_config_delete_button);
        View addBtn = view.findViewById(R.id.add_config_button);
        storedRecycler = view.findViewById(R.id.last_opened_recyclerview);

        adapter = new ConfigAdapter();
        storedRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        storedRecycler.setAdapter(adapter);

        mainViewModel.getConfigTitle().observe(getViewLifecycleOwner(), title -> {
            currentConfigText.setText(title == null || title.isEmpty() ? getString(R.string.no_config) : title);
        });

        mainViewModel.getCurrentConfigId().observe(getViewLifecycleOwner(), currentId -> {
            adapter.setCurrentConfigId(currentId);
        });

        mainViewModel.getConfigs().observe(getViewLifecycleOwner(), list -> {
            cachedConfigs.clear();
            if (list != null) cachedConfigs.addAll(list);
            adapter.setItems(cachedConfigs);
        });

        renameBtn.setOnClickListener(v -> showRenameDialog());
        deleteBtn.setOnClickListener(v -> showDeleteDialog());
        addBtn.setOnClickListener(v -> showAddDialog());

        mainViewModel.refreshConfigs();
    }

    private void showAddDialog() {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(getString(R.string.new_config_name));

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.add_configuration))
                .setView(input)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    mainViewModel.addConfig(name);
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private void showRenameDialog() {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        String current = currentConfigText.getText() == null ? "" : currentConfigText.getText().toString();
        input.setText(current);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.rename_config))
                .setView(input)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    mainViewModel.renameCurrentConfig(name);
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private void showDeleteDialog() {
        if (cachedConfigs.size() <= 1) {
            Toast.makeText(requireContext(), "至少保留一个配置", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.really_delete))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> mainViewModel.deleteCurrentConfig())
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private void selectConfig(String configId) {
        mainViewModel.selectConfig(configId);

        Fragment parent = getParentFragment();
        if (parent != null && parent.getView() != null) {
            DrawerLayout drawer = parent.getView().findViewById(R.id.drawer_layout);
            if (drawer != null) drawer.closeDrawers();
        }
    }

    private class ConfigAdapter extends RecyclerView.Adapter<ConfigViewHolder> {
        private final List<AppConfigStore.AppConfig> items = new ArrayList<>();
        private String currentId;

        void setItems(List<AppConfigStore.AppConfig> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        void setCurrentConfigId(String currentId) {
            this.currentId = currentId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ConfigViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.config_chooser_item, parent, false);
            return new ConfigViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ConfigViewHolder holder, int position) {
            AppConfigStore.AppConfig item = items.get(position);
            holder.textView.setText(item.name);

            boolean selected = item.id != null && item.id.equals(currentId);
            holder.textView.setAlpha(selected ? 1.0f : 0.75f);
            holder.textView.setTextColor(requireContext().getColor(selected ? R.color.colorAccent : R.color.whiteHigh));

            holder.itemView.setOnClickListener(v -> selectConfig(item.id));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class ConfigViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ConfigViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.config_name_textview);
        }
    }
}
