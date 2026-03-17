package com.example.myapplication;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化底部导航栏
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        // 首次进入，默认显示 HomeFragment
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // 设置底部导航点击监听
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_about) {
                selectedFragment = new AboutFragment();
            }

            if (selectedFragment != null) {
                // 每次替换 Fragment，简单实现，不保存状态
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // 处理返回键
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 获取当前显示的 Fragment
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof HomeFragment) {
                    HomeFragment homeFragment = (HomeFragment) currentFragment;
                    // 如果 WebView 可以回退，就让 WebView 回退
                    if (homeFragment.canGoBack()) {
                        homeFragment.goBack();
                        return;
                    }
                }
                // 否则执行默认的返回行为（退出应用）
                finish();
            }
        });
    }
}