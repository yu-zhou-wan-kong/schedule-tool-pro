package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.github.chrisbanes.photoview.PhotoView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class WidgetFragment extends Fragment {

    private View scrollOptions;
    private View layoutImageDetail;
    private LinearLayout optionsContainer;
    private PhotoView widgetImage;
    private Button btnBack;

    private boolean showingDetail = false;

    // ============================================================
    // ★ 扩展入口：在这里添加新的小组件选项
    // ============================================================
    private static List<WidgetOption> getDefaultOptions() {
        List<WidgetOption> list = new ArrayList<>();
        list.add(new WidgetOption(1, "通勤车、倒班车时间表", R.drawable.widget_timetable));
        // >>> 后续新增选项只需加一行：
        // list.add(new WidgetOption(2, "新功能名称", R.drawable.xxx));
        return list;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_widget, container, false);
        initViews(view);
        loadOptions();
        return view;
    }

    private void initViews(View view) {
        scrollOptions = view.findViewById(R.id.scroll_options);
        layoutImageDetail = view.findViewById(R.id.layout_image_detail);
        optionsContainer = view.findViewById(R.id.options_container);
        widgetImage = view.findViewById(R.id.widget_image);
        btnBack = view.findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> showOptions());
    }

    private void loadOptions() {
        List<WidgetOption> options = getDefaultOptions();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (WidgetOption option : options) {
            View card = inflater.inflate(R.layout.item_widget_option, optionsContainer, false);
            TextView titleView = card.findViewById(R.id.option_title);
            titleView.setText(option.getTitle());
            card.setOnClickListener(v -> showDetail(option));
            optionsContainer.addView(card);
        }
    }

    private void showDetail(WidgetOption option) {
        scrollOptions.setVisibility(View.GONE);
        layoutImageDetail.setVisibility(View.VISIBLE);
        widgetImage.setImageResource(option.getDrawableResId());
        showingDetail = true;
    }

    private void showOptions() {
        scrollOptions.setVisibility(View.VISIBLE);
        layoutImageDetail.setVisibility(View.GONE);
        showingDetail = false;
    }

    // ============================================================
    // 供 MainActivity 处理返回键使用
    // ============================================================
    public boolean isShowingDetail() {
        return showingDetail;
    }

    public void goBackToList() {
        showOptions();
    }
}
