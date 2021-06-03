package com.evergreen.treetop.ui.custom.recycler;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evergreen.treetop.architecture.data.AppTask;
import com.evergreen.treetop.ui.adapters.TaskListAdapter;

import java.util.List;

public class TaskListRecycler extends RecyclerView {

    private final TaskListAdapter m_adapter;

    public TaskListRecycler(@NonNull Context context) {
        super(context);
        m_adapter = new TaskListAdapter(context);
        setAdapter(m_adapter);
        setLayoutManager(new LinearLayoutManager(context));
    }

    public TaskListRecycler(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        m_adapter = new TaskListAdapter(context);
        setAdapter(m_adapter);
        setLayoutManager(new LinearLayoutManager(context));
    }

    public TaskListRecycler(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        m_adapter = new TaskListAdapter(context);
        setAdapter(m_adapter);
        setLayoutManager(new LinearLayoutManager(context));
    }

    public void loadTasks(List<AppTask> tasks)  {
        m_adapter.add(tasks);
    }
}
