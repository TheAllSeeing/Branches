package com.evergreen.treetop.ui.custom.edit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import com.evergreen.treetop.architecture.utils.Loggable;
import com.evergreen.treetop.ui.custom.utils.Shape;

@SuppressLint("ViewConstructor")
class BaseEdit extends AppCompatEditText implements Loggable {

    public BaseEdit(@NonNull Context context, Shape shape) {
        super(context);
        initConfigure(shape.getID());
    }

    public BaseEdit(@NonNull Context context, @Nullable AttributeSet attrs, Shape shape) {
        super(context, attrs);
        initConfigure(shape.getID());
    }

    public BaseEdit(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, Shape shape) {
        super(context, attrs, defStyleAttr);
        initConfigure(shape.getID());
    }

    private void initConfigure(int shapeId) {
        setBackground(ContextCompat.getDrawable(getContext(), shapeId));
        setGravity(Gravity.CENTER);
    }

    public void setBackgroundColor(int color) {
        getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public String getLabel() {
        if (getId() == View.NO_ID) return getClass().getName();
        return getResources().getResourceName(getId());
    }
}
