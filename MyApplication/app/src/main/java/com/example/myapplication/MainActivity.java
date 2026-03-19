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
import android.os.Build;
import java.io.File;
import androidx.core.content.FileProvider;
import android.provider.Settings;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private Handler mHandler = new Handler();
    private Runnable mDownloadCheckRunnable;
    private long mDownloadId = -1;

    private void checkUpdate() {
        // 网络请求在子线程执行
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String baseUrl = "https://gitee.com/yu-zhe-zhang/app-update/raw/master/update.json";
                long timestamp = System.currentTimeMillis();
                URL url = new URL(baseUrl + "?t=" + timestamp);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int responseCode = conn.getResponseCode();
                Log.d("Update", "服务器响应码: " + responseCode);
                if (responseCode == 200) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    is.close();

                    JSONObject json = new JSONObject(response.toString());
                    int latestVersionCode = json.getInt("versionCode");
                    String latestVersionName = json.getString("versionName");
                    String updateContent = json.getString("updateContent");
                    String downloadUrl = json.getString("downloadUrl");
                    boolean isForce = json.getBoolean("isForce");

                    int currentVersionCode = getPackageManager()
                            .getPackageInfo(getPackageName(), 0).versionCode;

                    Log.d("Update", "本地版本: " + currentVersionCode + ", 服务器版本: " + latestVersionCode);

                    if (latestVersionCode > currentVersionCode) {
                        int finalLatestVersionCode = latestVersionCode;
                        runOnUiThread(() -> showUpdateDialog(downloadUrl, updateContent, isForce, finalLatestVersionCode));
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

    private void showUpdateDialog(String downloadUrl, String content, boolean isForce, int latestVersionCode) {
        int ignoredVersion = getSharedPreferences("update", MODE_PRIVATE).getInt("ignored_version", 0);
        if (latestVersionCode == ignoredVersion) {
            Log.d("Update", "用户已忽略版本 " + latestVersionCode + "，不弹窗");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("发现新版本")
                .setMessage(content)
                .setPositiveButton("立即更新", (dialog, which) -> startDownload(downloadUrl))
                .setCancelable(!isForce);

        if (!isForce) {
            builder.setNegativeButton("稍后", null);
            builder.setNeutralButton("忽略此版本", (dialog, which) -> {
                getSharedPreferences("update", MODE_PRIVATE).edit()
                        .putInt("ignored_version", latestVersionCode).apply();
                Log.d("Update", "用户忽略版本 " + latestVersionCode);
                Toast.makeText(this, "已忽略此版本，有新版本时会再次提醒", Toast.LENGTH_SHORT).show();
            });
        } else {
            builder.setNegativeButton(null, null);
            setFinishOnTouchOutside(false);
        }

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(!isForce);
        dialog.show();
    }

    private void startDownload(String downloadUrl) {
        File oldFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "app_update.apk");
        if (oldFile.exists()) {
            oldFile.delete();
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle("应用更新");
        request.setDescription("正在下载新版本...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setMimeType("application/vnd.android.package-archive");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app_update.apk");

        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        mDownloadId = dm.enqueue(request);
        Log.d("Update", "【startDownload】新任务 ID = " + mDownloadId);
        getSharedPreferences("update", MODE_PRIVATE).edit().putLong("downloadId", mDownloadId).apply();

        // 启动轮询检查下载状态
        startPollingDownloadStatus();
    }

    private void startPollingDownloadStatus() {
        mDownloadCheckRunnable = new Runnable() {
            @Override
            public void run() {
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(mDownloadId);
                Cursor cursor = dm.query(query);

                if (cursor != null && cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);

                    if (statusIndex >= 0) {
                        int status = cursor.getInt(statusIndex);
                        Log.d("Update", "轮询下载状态: " + status);

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Log.d("Update", "轮询检测到下载成功");
                            if (uriIndex >= 0) {
                                String fileUriString = cursor.getString(uriIndex);
                                Uri fileUri = Uri.parse(fileUriString);
                                File file = new File(fileUri.getPath());

                                // 处理文件名（如果被加了.txt后缀）
                                if (file.getName().toLowerCase().endsWith(".txt")) {
                                    String newName = file.getName().substring(0, file.getName().length() - 4);
                                    File newFile = new File(file.getParent(), newName);
                                    if (file.renameTo(newFile)) {
                                        Log.d("Update", "重命名为: " + newFile.getName());
                                        file = newFile;
                                        fileUri = Uri.fromFile(file);
                                    } else {
                                        Log.e("Update", "重命名失败，尝试使用原文件安装");
                                    }
                                }

                                installApk(fileUri);
                            } else {
                                Log.e("Update", "无法获取下载文件URI");
                            }
                            cursor.close();
                            return; // 停止轮询
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            int reason = (reasonIndex >= 0) ? cursor.getInt(reasonIndex) : -1;
                            Log.e("Update", "下载失败，原因码: " + reason);
                            Toast.makeText(MainActivity.this, "下载失败，请稍后重试", Toast.LENGTH_SHORT).show();
                            cursor.close();
                            return;
                        }
                    }
                    cursor.close();
                } else {
                    Log.e("Update", "无法查询下载任务，可能已失效");
                    return;
                }

                // 未完成，1秒后再次检查
                mHandler.postDelayed(this, 1000);
            }
        };

        mHandler.post(mDownloadCheckRunnable);
    }

    private void installApk(Uri uri) {
        Log.d("Update", "installApk 被调用，URI: " + uri.toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "请允许安装未知应用后重试", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File file = new File(uri.getPath());
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Log.d("Update", "FileProvider 生成 URI: " + apkUri.toString());
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
        } else {
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

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_about) {
                selectedFragment = new AboutFragment();
            }

            if (selectedFragment != null) {
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof HomeFragment) {
                    HomeFragment homeFragment = (HomeFragment) currentFragment;
                    if (homeFragment.canGoBack()) {
                        homeFragment.goBack();
                        return;
                    }
                }
                finish();
            }
        });

        checkUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null && mDownloadCheckRunnable != null) {
            mHandler.removeCallbacks(mDownloadCheckRunnable);
        }
    }
}