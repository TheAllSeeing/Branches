package com.evergreen.treetop.ui.adapters;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.evergreen.treetop.R;

public abstract class AdapterWithEditHeader extends AdapterWithHeader {

    public AdapterWithEditHeader(Context context) {
        super(context);
    }

    @Override
    public int getHeaderLayout() {
        return R.layout.listrow_recycler_add_header;
    }

    @Override
    public final void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            holder.itemView.setOnClickListener(this::addAction);
        } else {
            _onBindViewHolder(holder, position - 1);
        }
    }

    protected abstract void _onBindViewHolder(@NonNull ViewHolder holder, int position);

    public abstract void addAction(View headerView);
}
