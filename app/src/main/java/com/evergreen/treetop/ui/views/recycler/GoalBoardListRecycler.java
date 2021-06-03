package com.evergreen.treetop.ui.views.recycler;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evergreen.treetop.ui.adapters.GoalBoardListAdapter;

public class GoalBoardListRecycler extends RecyclerView {

    private GoalBoardListAdapter m_adapter;

    public GoalBoardListRecycler(@NonNull Context context) {
        super(context);
        init(context);
    }

    public GoalBoardListRecycler(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GoalBoardListRecycler(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context)  {
        m_adapter = new GoalBoardListAdapter((Activity) context);
        setAdapter(m_adapter);
        getAdapter().refresh();
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {
            @Override
            public int getPaddingLeft() {
                View parent = (View) GoalBoardListRecycler.this.getParent();
                return Math.round(parent.getWidth() / 2f - GoalBoardListRecycler.this.getWidth() / 2f);
            }

            @Override
            public int getPaddingRight() {
                return getPaddingLeft();
            }
        });


    }

    @Override
    public GoalBoardListAdapter getAdapter() {
        return m_adapter;
    }
}
