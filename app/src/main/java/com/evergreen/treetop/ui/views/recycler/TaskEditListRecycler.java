package com.evergreen.treetop.ui.views.recycler;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evergreen.treetop.architecture.data.AppTask;
import com.evergreen.treetop.ui.adapters.TaskEditListAdapter;

import java.util.List;

public class TaskEditListRecycler extends RecyclerView {


    private TaskEditListAdapter m_adapter;

    public TaskEditListRecycler(@NonNull Context context) {
        super(context);
        m_adapter = new TaskEditListAdapter(context);
        setAdapter(m_adapter);
        setLayoutManager(new LinearLayoutManager(context));
    }

    public TaskEditListRecycler(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        m_adapter = new TaskEditListAdapter(context);
        setAdapter(m_adapter);
        setLayoutManager(new LinearLayoutManager(context));
    }

    public TaskEditListRecycler(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        m_adapter = new TaskEditListAdapter(context);
        setAdapter(m_adapter);
        setLayoutManager(new LinearLayoutManager(context));
    }

    public void loadTasks(List<AppTask> tasks)  {
        m_adapter.add(tasks);
    }

    public void setAddAction(OnClickListener listener) {
        m_adapter.bindHeader(listener);
    }

    public void invalidate() {
        setAdapter(m_adapter);
    }

    @Override
    public TaskEditListAdapter getAdapter() {
        return m_adapter;
    }
}
