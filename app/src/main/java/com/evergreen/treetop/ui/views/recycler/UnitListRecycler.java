package com.evergreen.treetop.ui.views.recycler;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.ui.adapters.UnitListAdapter;

import java.util.concurrent.ExecutionException;

public class UnitListRecycler extends RecyclerView {

    private UnitListAdapter m_adapter;

    public UnitListRecycler(@NonNull Context context) {
        super(context);
        m_adapter = new UnitListAdapter(context);
        setAdapter(m_adapter);
        setLayoutManager(new LinearLayoutManager(context));
    }

    public UnitListRecycler(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        m_adapter = new UnitListAdapter(context);
        setAdapter(m_adapter);
        setLayoutManager(new LinearLayoutManager(context));
    }

    public UnitListRecycler(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        m_adapter = new UnitListAdapter(context);
        setAdapter(m_adapter);
        setLayoutManager(new LinearLayoutManager(context));
    }

    public void init(Unit parent) throws ExecutionException, InterruptedException {
        m_adapter.init(parent);
    }

    public void invalidate() {
        setAdapter(m_adapter);
    }

    @Nullable
    @Override
    public UnitListAdapter getAdapter() {
        return m_adapter;
    }
}
