package com.evergreen.treetop.ui.custom.text;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CircleTextView extends EllipseTextView {
    public CircleTextView(@NonNull Context context) {
        super(context);
    }

    public CircleTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setRadius(int r) {
        super.setHeight(r);
        super.setWidth(r);
    }

    @Override
    public void setHeight(int pixels) {
        setRadius(pixels);
    }

    @Override
    public void setWidth(int pixels) {
        setRadius(pixels);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int r = Math.max(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(r, r);
    }


}

