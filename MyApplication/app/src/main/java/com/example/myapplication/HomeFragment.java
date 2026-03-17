package com.example.myapplication;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private WebView webView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 填充布局文件 fragment_home.xml（稍后创建）
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        webView = view.findViewById(R.id.webview);

        // 配置 WebView（从原 MainActivity 复制过来）
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setAllowFileAccessFromFileURLs(true);
        }
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");

        return view;
    }

    // 提供给 Activity 使用的方法：判断 WebView 能否回退
    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    // 让 WebView 回退一页
    public void goBack() {
        if (webView != null) {
            webView.goBack();
        }
    }
}