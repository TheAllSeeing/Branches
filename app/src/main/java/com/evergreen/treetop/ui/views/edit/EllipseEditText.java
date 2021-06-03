package com.evergreen.treetop.ui.views.edit;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evergreen.treetop.ui.views.utils.Shape;

public class EllipseEditText extends BaseEdit {

    public EllipseEditText(@NonNull Context context) {
        super(context, Shape.ELLIPSE);
    }

    public EllipseEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, Shape.ELLIPSE);
    }

    public EllipseEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, Shape.ELLIPSE);
    }
}
