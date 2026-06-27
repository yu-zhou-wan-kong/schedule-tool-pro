package com.example.myapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        // 项目地址点击复制
        TextView tvContact = view.findViewById(R.id.tv_contact);
        tvContact.setOnClickListener(v -> copyToClipboard(
                "https://github.com/yu-zhou-wan-kong/schedule-tool-pro",
                "项目地址已复制"));

        // 邮箱点击复制
        TextView tvEmail = view.findViewById(R.id.tv_email);
        tvEmail.setOnClickListener(v -> copyToClipboard(
                "zhang_yuzhe@foxmail.com",
                "邮箱已复制"));

        return view;
    }

    private void copyToClipboard(String text, String toastMsg) {
        ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), toastMsg, Toast.LENGTH_SHORT).show();
    }
}
