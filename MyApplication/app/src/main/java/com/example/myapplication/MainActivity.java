package com.example.myapplication;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.JSONObject;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Environment;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.database.Cursor;
import android.content.Intent;
import android.content.Context;
import androidx.core.content.ContextCompat;
import android.widget.Toast;
import android.os.Build;                    // 用于 Build.VERSION.SDK_INT
import java.io.File;                         // 用于 File 类
import androidx.core.content.FileProvider;    // 用于 FileProvider
import android.provider.Settings;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;

    private void checkUpdate() {
        // 网络请求在子线程执行
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // 1. 请求 update.json
                URL url = new URL("https://gitee.com/yu-zhe-zhang/app-update/raw/master/update.json");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int responseCode = conn.getResponseCode();
                Log.d("Update", "服务器响应码: " + responseCode); // 调试日志
                if (responseCode == 200) {
                    // 2. 读取响应内容
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    is.close();

                    // 3. 解析 JSON
                    JSONObject json = new JSONObject(response.toString());
                    int latestVersionCode = json.getInt("versionCode");
                    String latestVersionName = json.getString("versionName");
                    String updateContent = json.getString("updateContent");
                    String downloadUrl = json.getString("downloadUrl");
                    boolean isForce = json.getBoolean("isForce");

                    // 4. 获取当前版本号
                    int currentVersionCode = getPackageManager()
                            .getPackageInfo(getPackageName(), 0).versionCode;

                    // 5. 比较版本
                    if (latestVersionCode > currentVersionCode) {
                        // 切换到主线程显示更新对话框
                        runOnUiThread(() -> showUpdateDialog(downloadUrl, updateContent, isForce));
                    }
                } else {
                    Log.e("Update", "服务器返回错误码: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("Update", "检查更新失败", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void showUpdateDialog(String downloadUrl, String content, boolean isForce) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("发现新版本")
                .setMessage(content)
                .setPositiveButton("立即更新", (dialog, which) -> startDownload(downloadUrl))
                .setCancelable(!isForce);  // 非强制更新时可以取消

        if (!isForce) {
            builder.setNegativeButton("稍后", null);
        } else {
            // 强制更新时，点击返回键无效
            setFinishOnTouchOutside(false);
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(!isForce);  // 非强制更新时点击外部可取消
        dialog.show();
    }

    private void startDownload(String downloadUrl) {
        // 删除旧文件（可选）
        File oldFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "app_update.apk");
        if (oldFile.exists()) {
            oldFile.delete();
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle("应用更新");
        request.setDescription("正在下载新版本...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // 关键：明确设置 MIME 类型为 APK
        request.setMimeType("application/vnd.android.package-archive");

        // 设置文件名
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app_update.apk");

        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        long downloadId = dm.enqueue(request);
        getSharedPreferences("update", MODE_PRIVATE).edit().putLong("downloadId", downloadId).apply();

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(this, downloadCompleteReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            long savedId = getSharedPreferences("update", MODE_PRIVATE).getLong("downloadId", -1);

            if (downloadId == savedId) {
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = dm.query(query);

                if (cursor != null && cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                    if (statusIndex >= 0 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                        if (uriIndex >= 0) {
                            String fileUriString = cursor.getString(uriIndex);
                            Uri fileUri = Uri.parse(fileUriString);
                            File file = new File(fileUri.getPath());

                            // 检查文件名是否以 .txt 结尾，若是则重命名
                            if (file.getName().toLowerCase().endsWith(".txt")) {
                                String newName = file.getName().substring(0, file.getName().length() - 4); // 去掉 .txt
                                File newFile = new File(file.getParent(), newName);
                                // 如果目标文件已存在，先删除
                                if (newFile.exists()) {
                                    newFile.delete();
                                }
                                if (file.renameTo(newFile)) {
                                    Log.d("Update", "重命名为: " + newFile.getName());
                                    file = newFile;
                                    fileUri = Uri.fromFile(file);
                                } else {
                                    Log.e("Update", "重命名失败，尝试使用原文件安装");
                                }
                            }

                            // 调用安装
                            installApk(fileUri);
                        }
                    }
                    cursor.close();
                }
                unregisterReceiver(this);
            }
        }
    };

    private boolean copyFile(File src, File dst) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return true;
        } catch (Exception e) {
            Log.e("Update", "复制文件异常", e);
            return false;
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void installApk(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                // 引导用户开启权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "请允许安装未知应用后重试", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0+ 需要使用 FileProvider 生成 content:// URI
            File file = new File(uri.getPath());  // uri 可能是 file:///storage/... 格式
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
        } else {
            // Android 6.0 及以下直接使用 file:// URI
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        }
    }

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

        checkUpdate();

    }
}