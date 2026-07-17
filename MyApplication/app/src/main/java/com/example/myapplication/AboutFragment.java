package com.example.myapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 填充版本号
        String version = String.format(getString(R.string.about_version), BuildConfig.VERSION_NAME);
        binding.tvVersion.setText(version);

        // 项目地址：点击打开浏览器，长按复制
        binding.tvContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/yu-zhou-wan-kong/schedule-tool-pro"));
            startActivity(intent);
        });
        binding.tvContact.setOnLongClickListener(v -> {
            copyToClipboard(
                    "https://github.com/yu-zhou-wan-kong/schedule-tool-pro",
                    "项目地址已复制");
            return true;
        });

        // 邮箱：点击打开邮件应用，长按复制
        binding.tvEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:zhang_yuzhe@foxmail.com"));
            startActivity(intent);
        });
        binding.tvEmail.setOnLongClickListener(v -> {
            copyToClipboard(
                    "zhang_yuzhe@foxmail.com",
                    "邮箱已复制");
            return true;
        });

        // 开源许可：弹出对话框
        binding.tvOss.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.about_oss_title)
                    .setMessage(R.string.about_oss_content)
                    .setPositiveButton("确定", null)
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void copyToClipboard(String text, String toastMsg) {
        ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), toastMsg, Toast.LENGTH_SHORT).show();
    }
}
