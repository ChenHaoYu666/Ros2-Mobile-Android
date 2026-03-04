package com.example.ros2_android_test_app.ui.fragments.main;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.ros2_android_test_app.R;
import com.example.ros2_android_test_app.ROSActivity;
import com.example.ros2_android_test_app.Ros2ConfigManager;
import com.example.ros2_android_test_app.ui.fragments.details.DetailsFragment;
import com.example.ros2_android_test_app.ui.fragments.master.MasterFragment;
import com.example.ros2_android_test_app.ui.fragments.ssh.SshFragment;
import com.example.ros2_android_test_app.ui.fragments.viz.VizFragment;
import com.example.ros2_android_test_app.viewmodel.MainViewModel;
import com.google.android.material.tabs.TabLayout;

public class MainFragment extends Fragment implements OnBackPressedListener {

    public static final String TAG = MainFragment.class.getSimpleName();

    private TabLayout tabLayout;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private MainViewModel mViewModel;
    private String currentTabName = "Master";

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tabLayout = view.findViewById(R.id.tabs);
        toolbar = view.findViewById(R.id.toolbar);
        drawerLayout = view.findViewById(R.id.drawer_layout);

        drawerLayout.setScrimColor(getResources().getColor(R.color.drawerFadeColor));

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(toolbar);

            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(activity, drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        tabLayout.selectTab(tabLayout.getTabAt(0));
        switchToTab("Master");

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.i(TAG, "On Tab selected: " + tab.getText());
                if (tab.getText() == null) {
                    return;
                }
                switchToTab(tab.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        if (this.getArguments() != null) {
            mViewModel.createFirstConfig(this.getArguments().getString("configName"));
        } else {
            mViewModel.createFirstConfig(null);
        }

        mViewModel.getConfigTitle().observe(getViewLifecycleOwner(), this::setTitle);

        mViewModel.getConfigSwitchVersion().observe(getViewLifecycleOwner(), version -> {
            if (version == null || getActivity() == null) return;

            int domainId = Ros2ConfigManager.getDomainId(requireContext());
            if (getActivity() instanceof ROSActivity) {
                ROSActivity activity = (ROSActivity) getActivity();
                if (activity.getCurrentEffectiveDomainId() != domainId) {
                    activity.restartRosRuntime();
                }
            }

            switchToTab(currentTabName);
        });
    }

    private void setTitle(String newTitle) {
        if (toolbar == null || newTitle == null) return;
        if (newTitle.equals(toolbar.getTitle().toString())) {
            return;
        }
        toolbar.setTitle(newTitle);
    }

    @Override
    public boolean onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        return false;
    }

    private void switchToTab(String tabName) {
        currentTabName = tabName;

        Fragment fragment;
        switch (tabName) {
            case "Master":
                fragment = new MasterFragment();
                break;
            case "Viz":
                fragment = new VizFragment();
                break;
            case "Details":
                fragment = new DetailsFragment();
                break;
            case "SSH":
                fragment = new SshFragment();
                break;
            default:
                fragment = new VizFragment();
                break;
        }

        FragmentTransaction tx = getChildFragmentManager().beginTransaction();
        tx.replace(R.id.fragment_container, fragment);
        tx.commitAllowingStateLoss();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home && drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }

        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }
}
