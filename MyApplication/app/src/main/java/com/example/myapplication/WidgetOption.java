package com.example.myapplication;

/**
 * 小组件选项的数据模型。
 * 后续新增选项只需在 WidgetFragment.getDefaultOptions() 中 new 一个即可。
 */
public class WidgetOption {

    private final int id;
    private final String title;
    private final int drawableResId;

    public WidgetOption(int id, String title, int drawableResId) {
        this.id = id;
        this.title = title;
        this.drawableResId = drawableResId;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDrawableResId() {
        return drawableResId;
    }
}
