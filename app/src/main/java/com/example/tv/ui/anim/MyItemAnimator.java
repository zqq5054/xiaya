package com.example.tv.ui.anim;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

public class MyItemAnimator extends DefaultItemAnimator {

    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder, @NonNull RecyclerView.ViewHolder newHolder, @NonNull ItemHolderInfo preLayoutInfo, @NonNull ItemHolderInfo postLayoutInfo) {
        // 不需要动画，直接设置选中效果
        if (oldHolder.itemView.isSelected()) {
            // 取消选中效果
            oldHolder.itemView.setBackgroundColor(Color.WHITE); // 设置默认背景色
        } else if (newHolder.itemView.isSelected()) {
            // 设置选中效果
            newHolder.itemView.setBackgroundColor(Color.BLUE); // 设置选中背景色
        }
        return true;
    }
}