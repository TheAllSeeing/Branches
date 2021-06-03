package com.evergreen.treetop.ui.views.recycler;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evergreen.treetop.ui.adapters.UnitEditListAdapter;

import java.util.concurrent.ExecutionException;

public class UnitEditListRecycler extends RecyclerView {


    private UnitEditListAdapter m_adapter;

    public UnitEditListRecycler(@NonNull Context context) {
        super(context);
        constructWith(context);
    }

    public UnitEditListRecycler(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        constructWith(context);
    }

    public UnitEditListRecycler(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        constructWith(context);
    }

    private void constructWith(Context context) {
        m_adapter = new UnitEditListAdapter(context);
        setAdapter(m_adapter);
        setLayoutManager(new LinearLayoutManager(context));
    }

    public void setAddAction(OnClickListener listener) {
        m_adapter.bindHeader(listener);
    }

    public void invalidate() {
        setAdapter(m_adapter);
    }

    @Override
    public UnitEditListAdapter getAdapter() {
        return m_adapter;
    }
}
