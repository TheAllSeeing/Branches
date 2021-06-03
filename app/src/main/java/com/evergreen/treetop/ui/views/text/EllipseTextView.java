package com.evergreen.treetop.ui.views.text;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.evergreen.treetop.ui.views.utils.Shape;

public class EllipseTextView extends BaseText {

    public EllipseTextView(@NonNull Context context) {
        super(context, Shape.ELLIPSE);
    }

    public EllipseTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, Shape.ELLIPSE);
    }

    public EllipseTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, Shape.ELLIPSE);
    }

}

