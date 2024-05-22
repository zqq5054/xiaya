package com.example.tv.ui.theme;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private Paint paint;

    public DividerItemDecoration() {
        paint = new Paint();
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(1);
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            int top = child.getBottom();
            int bottom = top + 2;
            c.drawLine(child.getLeft(), top, child.getRight(), bottom, paint);
        }
    }
}
