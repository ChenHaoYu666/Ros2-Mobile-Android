package com.example.ros2_android_test_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private EditText domainIdEdit;
    private TextView effectiveDomainLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        domainIdEdit = view.findViewById(R.id.domainIdEdit);
        effectiveDomainLabel = view.findViewById(R.id.effectiveDomainLabel);
        Button saveDomainIdBtn = view.findViewById(R.id.saveDomainIdBtn);

        int currentDomain = Ros2ConfigManager.getDomainId(requireContext());
        if (domainIdEdit != null) {
            domainIdEdit.setText(String.valueOf(currentDomain));
        }

        refreshEffectiveDomainLabel();

        if (saveDomainIdBtn != null) {
            saveDomainIdBtn.setOnClickListener(v -> {
                int domain = 0;
                if (domainIdEdit != null) {
                    String text = domainIdEdit.getText().toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            domain = Integer.parseInt(text);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "Invalid domain id", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }
                Ros2ConfigManager.setDomainId(requireContext(), domain);

                if (getActivity() instanceof ROSActivity) {
                    ((ROSActivity) getActivity()).restartRosRuntime();
                    refreshEffectiveDomainLabel();
                    Toast.makeText(requireContext(), "Domain ID saved: " + domain + ". ROS runtime restarted.", Toast.LENGTH_LONG).show();
                } else {
                Toast.makeText(requireContext(), "Domain ID saved: " + domain + ". Restart app to take effect.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshEffectiveDomainLabel();
    }

    private void refreshEffectiveDomainLabel() {
        if (effectiveDomainLabel == null) {
            return;
        }

        if (getActivity() instanceof ROSActivity) {
            int effectiveDomain = ((ROSActivity) getActivity()).getCurrentEffectiveDomainId();
            effectiveDomainLabel.setText("当前生效 Domain ID: " + effectiveDomain);
        } else {
            effectiveDomainLabel.setText("当前生效 Domain ID: --");
        }
    }
}
