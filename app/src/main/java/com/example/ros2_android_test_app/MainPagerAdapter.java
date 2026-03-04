package com.example.ros2_android_test_app;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.ros2_android_test_app.ui.fragments.StubFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new StubFragment();
            case 1:
                return new SettingsFragment();
            case 2:
                return new StubFragment();
            default:
                return new StubFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
