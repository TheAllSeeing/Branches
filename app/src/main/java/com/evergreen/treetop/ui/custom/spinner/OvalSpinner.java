package com.evergreen.treetop.ui.custom.spinner;


import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evergreen.treetop.ui.custom.utils.Shape;

public class OvalSpinner extends BaseSpinner {

    public OvalSpinner(@NonNull Context context) {
        super(context, Shape.OVAL_SPINNER);
    }

    public OvalSpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, Shape.OVAL_SPINNER);
    }

    public OvalSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, Shape.OVAL_SPINNER);
    }
}
