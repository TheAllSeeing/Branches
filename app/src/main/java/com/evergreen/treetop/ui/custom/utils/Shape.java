package com.evergreen.treetop.ui.custom.utils;

import com.evergreen.treetop.R;

public  enum Shape {
    ELLIPSE(R.drawable.circle),
    OVAL_RECT(R.drawable.oval_rectangle),
    OVAL_SPINNER(R.drawable.oval_dropdown);

    private final int m_drawableID;

    Shape(int drawableID) {
        m_drawableID = drawableID;
    }

    public int getID() {
        return m_drawableID;
    }
}
