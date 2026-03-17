package com.example.myapplication;

import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Button;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置 Toolbar 作为 ActionBar（可选）
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

// 找到关于按钮并设置点击监听
        Button btnAbout = findViewById(R.id.btnAbout);
        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });

        webView = findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        // ... 其他设置 ...
        // 3. 允许从 file:// 加载的页面访问跨域资源（关键！）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setAllowFileAccessFromFileURLs(true);
        }

        // 4. 允许访问文件（因为页面在 assets 中）
        webSettings.setAllowFileAccess(true);

        // 5. 允许内容 URL 访问（可选）
        webSettings.setAllowContentAccess(true);


        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");

        // 返回键处理（兼容手势导航）
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }
}