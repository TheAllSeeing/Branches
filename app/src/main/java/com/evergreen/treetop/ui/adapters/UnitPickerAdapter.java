package com.evergreen.treetop.ui.adapters;

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.evergreen.treetop.R;
import com.evergreen.treetop.activities.units.TM_UnitEditorActivity;
import com.evergreen.treetop.activities.units.TM_UnitPickerActivity;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.LoggingUtils;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.data.User;
import com.evergreen.treetop.architecture.handlers.UnitDB;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.AndroidUtils;
import com.evergreen.treetop.architecture.utils.DBUnit;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class UnitPickerAdapter extends Adapter<ViewHolder> {

    private List<Unit> m_data;
    private Unit m_currentParent;
    private final TM_UnitPickerActivity m_context;
    private final User m_user;
    private final Unit m_rootUnit;
    private final boolean m_onlyLeading;


    public UnitPickerAdapter(TM_UnitPickerActivity context) throws ExecutionException, InterruptedException {
        this(context, null, false, null);
    }

    public UnitPickerAdapter(TM_UnitPickerActivity context, User user, boolean onlyLeading) throws ExecutionException, InterruptedException {
        this(context, user, onlyLeading, null);
    }

    public UnitPickerAdapter(TM_UnitPickerActivity context, User user, boolean onlyLeading, Unit rootUnit) throws ExecutionException, InterruptedException {
        m_data = rootUnit != null? new ArrayList<>(Collections.singletonList(rootUnit)) : UnitDB.getInstance().getRootUnits();
        m_context = context;
        m_user = user;
        m_onlyLeading = onlyLeading;
        m_rootUnit = rootUnit;

        filterForUser(user);

        if (m_data.size() == 0) {
            Toast.makeText(m_context, "No suitable units found. ", Toast.LENGTH_SHORT).show();
            Log.w("UI_ERROR", "User entered UnitPicker, but after filtering for user there" +
                    "are no suitable units left.");
            m_context.setResult(Activity.RESULT_CANCELED);
            m_context.finish();
        } else {
            Log.v("UI_EVENT", "Created a new Unit List with values " + LoggingUtils.stringify(m_data));
        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.v("UI_EVENT", "Created new UnitHolder");
        return new UnitHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        new Thread(() -> {
            UnitHolder holderUnit = (UnitHolder)holder;
            Unit unit = m_data.get(position);

            m_context.runOnUiThread( () -> {
                holderUnit.setContent(unit);

                holderUnit.getNameView().setOnClickListener(v -> {
                    m_context.setResult(
                            Activity.RESULT_OK,
                            new Intent()
                            .putExtra(TM_UnitPickerActivity.RESULT_PICKED_EXTRA_KEY, DBUnit.of(m_data.get(position)))
                    );

                    m_context.finish();
                });


                if (hasDown(unit))  {
                    holderUnit.getMoreView().setOnClickListener(v ->
                            new Thread( () -> {
                                try {
                                    goDown(unit);
                                } catch (ExecutionException e) {
                                    Toast.makeText(m_context, "Could not retrieve children", Toast.LENGTH_SHORT).show();
                                    Log.w("DB_ERROR", "Tried to retrieve children of " + unit.toString()
                                            + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
                                } catch (InterruptedException e) {
                                    Log.w("DB_ERROR", "Canceled retrieving " + unit.toString() + ":\n"
                                            + ExceptionUtils.getStackTrace(e));
                                }
                            }).start()
                    );
                } else {
                    holderUnit.getMoreView().setOnClickListener(AndroidUtils.DO_NOTHING_CLICK);
                }

                Log.v("UI_EVENT", "Bound new UnitHolder to " + m_data.get(position));
            });
        }).start();

    }

    @Override
    public int getItemCount() {
        return m_data.size();
    }

    public class UnitHolder extends ViewHolder {

        private final TextView m_textName;
        private final TextView m_textMore;

        public UnitHolder(ViewGroup parent) {

            super(LayoutInflater.from(m_context)
                    .inflate(R.layout.listrow_recycler_unit_picker, parent, false)
            );

            m_textName = itemView.findViewById(R.id.tm_unit_picker_text_title);
            m_textMore = itemView.findViewById(R.id.tm_unit_picker_text_more_icon);
        }

        public void setContent(Unit unit) {
            m_textName.setText(unit.getName());


            if (hasDown(unit)) {
                m_textMore.setText("ᐅ");
            } else { // Need to set default as it recycles previous views, so their config will remain.
                m_textMore.setText("");
            }
        }

        public View getView() {
            return itemView;
        }

        public TextView getNameView() {
            return m_textName;
        }

        public TextView getMoreView() {
            return m_textMore;
        }

    }

    public void reset() {
        try {
            m_data = m_rootUnit != null? new ArrayList<>(Collections.singletonList(m_rootUnit)) : UnitDB.getInstance().getRootUnits();
            filterForUser(m_user);
            m_context.runOnUiThread(this::notifyDataSetChanged);
        } catch (ExecutionException e) {
            Toast.makeText(m_context, "Failed to refresh units. Info could be outdated.", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR", "Failed to refresh unit picker:\n" + ExceptionUtils.getStackTrace(e));
        } catch (InterruptedException e) {
            Log.w("DB_ERROR", "Cancelled refresh of unit picker:\n" + ExceptionUtils.getStackTrace(e));
        }
    }

    public List<String> getUnitIds() {
        return m_data.stream().map(Unit::getId).collect(Collectors.toList());
    }

    public List<Unit> getData() {
        return m_data;
    }

    public void clear() {
        int init = getItemCount();
        m_data.clear();
        notifyItemRangeRemoved(0, init);
    }


    public void  filterForUser(User user) {
        if (user == null) return;

        List<Unit> newData;

        if (m_onlyLeading) {
            newData = m_data.stream().filter(unit -> user.getLeadingIds().contains(unit.getId())).collect(Collectors.toList());
        } else {
           newData = m_data.stream().filter(unit -> user.getUnitIds().contains(unit.getId())).collect(Collectors.toList());
        }


        m_data = newData;
    }

    private void goUp() throws ExecutionException, InterruptedException, NoSuchDocumentException {

        Unit pivot = m_data.get(0);

        if (isRootUnit(pivot)) {
            m_context.setResult(Activity.RESULT_CANCELED);
            m_context.finish();
            return;
        }

        Unit parent = pivot.getParent();

        if (parent.isRootUnit()) {
            m_data.clear();
            m_data.addAll(UnitDB.getInstance().getRootUnits());
            filterForUser(m_user);
            m_context.runOnUiThread(this::notifyDataSetChanged);

            if (UserDB.getInstance().getCurrentUser().isIn(parent.getId())) {
                if (m_context.getOptionsMenu().size() == 0) {
                    m_context.getOptionsMenu().add("New Unit").setIcon(R.drawable.ic_add).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            } else {
                m_context.runOnUiThread(m_context.getOptionsMenu()::clear);
            }

        } else {
            goDown(parent.getParent());
        }
    }

    private void goDown(Unit parent) throws ExecutionException, InterruptedException {
        m_currentParent = parent;
        m_data = parent.getChildren();
        filterForUser(m_user);
        m_context.runOnUiThread(() -> {
            notifyDataSetChanged();
            m_context.setTitle(parent.getName());
        });

    }

    public Intent newUnit() {
        return new Intent(m_context, TM_UnitEditorActivity.class)
                .putExtra(TM_UnitEditorActivity.PARENT_ID_EXTRA_KEY, m_data.get(0).getParentId());
    }

    public void newUnitCallback(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Unit res = Unit.of((DBUnit)result.getData().getSerializableExtra(TM_UnitEditorActivity.RESULT_UNIT_EXTRA_KEY));
            m_data.add(res);
            notifyItemInserted(getItemCount());
        }
    }

    // MUST be ran in TM_UnitPickerActivity#onBackPressed.
    public void onBackPressed() {
        new Thread( () -> {
            Looper.prepare();
            try {
                goUp();
            } catch (ExecutionException e) {
                Toast.makeText(m_context, "Failed to get parent units", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Tried to retrieve parents of " + m_data.get(0).toString()
                        + " but failed:\n" + ExceptionUtils.getStackTrace(e));
            } catch (InterruptedException e) {
                Log.w("DB_ERROR", "Cancelled retrieval of parent units.\n" + ExceptionUtils.getStackTrace(e));
            } catch (NoSuchDocumentException e) {
                Toast.makeText(m_context, "Could not identify parent unit", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Tried to retrieve parents of " + m_data.get(0).toString()
                        + " but there is no such unit:\n" + ExceptionUtils.getStackTrace(e));
            }
        }).start();
    }

    private boolean isRootUnit(Unit unit) {
        if (m_rootUnit == null) {
            return unit.isRootUnit();
        }

        return unit.getId().equals(m_rootUnit.getId());
    }

    public Unit getCurrentParent() {
        return m_currentParent;
    }

    private boolean hasDown(Unit unit) {

        boolean useRaw = m_user == null;
        boolean hasChildren = unit.getChildrenIds().size() > 0;

        boolean useLeading = m_onlyLeading;
        boolean hasLeading =
                SetUtils.intersection(
                        new HashSet<>(UserDB.getInstance().getCurrentUser().getLeadingIds()),
                        new HashSet<>(unit.getChildrenIds())
                ).size() > 0;


        boolean useIn = !useRaw && !useLeading;
        boolean hasIn =
                SetUtils.intersection(
                        new HashSet<>(UserDB.getInstance().getCurrentUser().getLeadingIds()),
                        new HashSet<>(unit.getChildrenIds())
                ).size() > 0;

        return
                useRaw && hasChildren
                || useIn && hasIn
                || useLeading && hasLeading;
    }



}


