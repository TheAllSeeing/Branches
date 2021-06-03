package com.evergreen.treetop.ui.views.spinner;


import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evergreen.treetop.ui.views.utils.Shape;

public class EllipseSpinner extends BaseSpinner {

    public EllipseSpinner(@NonNull Context context) {
        super(context, Shape.OVAL_SPINNER);
    }

    public EllipseSpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, Shape.ELLIPSE);
    }

    public EllipseSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, Shape.ELLIPSE);
    }
}
