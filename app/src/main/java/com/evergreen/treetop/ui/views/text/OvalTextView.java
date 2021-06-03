package com.evergreen.treetop.ui.views.text;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evergreen.treetop.ui.views.utils.Shape;

public class OvalTextView extends BaseText {

    public OvalTextView(@NonNull Context context) {
        super(context, Shape.OVAL_RECT);
    }

    public OvalTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, Shape.OVAL_RECT);
    }

    public OvalTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, Shape.OVAL_RECT);
    }
}
