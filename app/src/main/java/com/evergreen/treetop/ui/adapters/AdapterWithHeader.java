package com.evergreen.treetop.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public abstract class AdapterWithHeader extends Adapter<ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_NORMAL = -1;
    private final Context m_context;

    public AdapterWithHeader(Context context) {
        m_context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            return new ViewHolder(LayoutInflater.from(m_context).inflate(getHeaderLayout(), parent, false)) {};
        } else {
            return _onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public final int getItemCount() {
        return _getItemCount() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        }

        return _getItemViewType(position - 1);
    }

    public abstract int getHeaderLayout();
    public abstract int _getItemCount();
    public int _getItemViewType(int position) {
        return -1;
    }

    @NonNull
    public abstract ViewHolder _onCreateViewHolder(@NonNull ViewGroup parent, int viewType);
}
