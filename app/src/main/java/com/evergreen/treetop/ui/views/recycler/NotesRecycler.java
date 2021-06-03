package com.evergreen.treetop.ui.views.recycler;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evergreen.treetop.ui.adapters.NotesAdapter;

import java.util.List;

public class NotesRecycler extends RecyclerView {

    public NotesRecycler(@NonNull Context context) {
        super(context);
        init();
    }

    public NotesRecycler(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NotesRecycler(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setAdapter(new NotesAdapter(getContext()));
        setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public void load(List<Pair<String, Boolean>> data) {
        data.forEach(pair -> getAdapter().add(pair.first, pair.second));
    }

    @Nullable
    @Override
    public NotesAdapter getAdapter() {
        return (NotesAdapter) super.getAdapter();
    }
}
