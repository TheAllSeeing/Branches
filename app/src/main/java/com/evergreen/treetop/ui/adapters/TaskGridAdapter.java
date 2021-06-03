package com.evergreen.treetop.ui.adapters;

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.evergreen.treetop.R;
import com.evergreen.treetop.activities.TM_DashboardActivity;
import com.evergreen.treetop.activities.tasks.TM_TaskViewActivity;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.LoggingUtils;
import com.evergreen.treetop.architecture.data.AppTask;
import com.evergreen.treetop.architecture.data.Goal;
import com.evergreen.treetop.architecture.data.User;
import com.evergreen.treetop.architecture.handlers.TaskDB;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.UIUtils;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class TaskGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<AppTask> m_data;

    private final Activity m_context;

    private static int s_unitFilter = 0;
    private static int s_assignedFilter = 0;
    private static int s_completedFilter = 0;

    public static final int UNIT_FILTER_NONE = 0;
    public static final int UNIT_FILTER_IN = 1;
    public static final int UNIT_FILTER_LEADING = 2;

    public static final int ASSIGN_FILTER_NONE = 0;
    public static final int ASSIGN_FILTER_ASSIGNER = 1;
    public static final int ASSIGN_FILTER_ASSIGNEE = 2;

    public static final int COMPLETE_FILTER_NONE = 0;
    public static final int COMPLETE_FILTER_INCOMPLETE = 1;
    public static final int COMPLETE_FILTER_COMPLETE = 2;

    public static final int CODE_OK = 0;
    public static final int CODE_INTERRUPT = 1;
    public static final int CODE_EXECUTE_ERROR = 2;
    public static final int CODE_NULL_ERROR = 3;

    private int m_unitResCode = CODE_OK;
    private int m_assigneeResCode = CODE_OK;

    public TaskGridAdapter(Activity context, List<AppTask> data) {
        m_data = data;
        m_context = context;
        Log.v("UI_EVENT", "Created a new TaskGrid with values " + LoggingUtils.stringify(m_data));
    }


    public TaskGridAdapter(Activity context, AppTask[] data) {
        this(context, new ArrayList<>(Arrays.asList(data)));
    }


    public TaskGridAdapter(Activity context) {
        this(context, new ArrayList<>());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        Log.v("UI_EVENT", "Created new TaskHolder");
        return new TaskHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TaskHolder holderTask = (TaskHolder)holder;

        holderTask.setContent(m_data.get(position));
        holderTask.getView() .setOnClickListener(v -> m_context.startActivity(
                new Intent(m_context, TM_TaskViewActivity.class)
                .putExtra(TM_TaskViewActivity.TASK_ID_EXTRA_KEY, m_data.get(position).getId())
        ));

        Log.v("UI_EVENT", "Bound new TaskHolder to " + m_data.get(position));
    }

    @Override
    public int getItemCount() {
        return m_data.size();
    }

    public class TaskHolder extends RecyclerView.ViewHolder {

        private final TextView m_textTitle;
        private final TextView m_textInfo;
        private final ProgressBar m_progSepLine;

        public TaskHolder(ViewGroup parent) {

            super(LayoutInflater.from(m_context)
                    .inflate(R.layout.listrow_recycler_task_grid, parent, false)
            );

            m_textTitle = itemView.findViewById(R.id.tm_task_grid_item_text_title);
            m_progSepLine = itemView.findViewById(R.id.tm_task_grid_item_prog_color);
            m_textInfo = itemView.findViewById(R.id.tm_task_grid_item_text_info);
        }

        public void setContent(AppTask task) {
            m_textTitle.setText(task.getTitle());

            new Thread(() -> {
                String unitName = null, assigneeString = null;

                try {
                    unitName = task.getUnit().getName();
                } catch (ExecutionException e) {
                    m_unitResCode = CODE_EXECUTE_ERROR;
                    Log.w("DB_ERROR", "Tried to retrieve " + task.toString() + " unit for grid, but failed:\n" + ExceptionUtils.getStackTrace(e));
                } catch (InterruptedException e) {
                    m_unitResCode = CODE_INTERRUPT;
                    Log.w("DB_ERROR", "Cancel retrieval of " + task.toString() + " unit for grid:\n" + ExceptionUtils.getStackTrace(e));
                } catch (NoSuchDocumentException e) {
                    m_unitResCode = CODE_NULL_ERROR;
                    Log.w("DB_ERROR", "Could not retrieve " + task.toString() + " unit for grid; It specifies a non-existent unit:\n" + ExceptionUtils.getStackTrace(e));
                }

                try {
                    assigneeString = task.getAssignees().stream().map(User::getName).collect(Collectors.joining(", "));
                    if (assigneeString.equals("")) {
                        assigneeString = "No Assignees";
                    }
                } catch (ExecutionException e) {
                    m_assigneeResCode = CODE_EXECUTE_ERROR;
                    Log.w("DB_ERROR", "Tried to retrieve assignees of " + task.toString() + " for grid, but failed:\n" + ExceptionUtils.getStackTrace(e));
                } catch (InterruptedException e) {
                    m_assigneeResCode = CODE_INTERRUPT;
                    Log.w("DB_ERROR", "Cancel retrieval of " + task.toString() + " assignees for grid:\n" + ExceptionUtils.getStackTrace(e));
                } catch (NoSuchDocumentException e) {
                    m_assigneeResCode = CODE_NULL_ERROR;
                    Log.w("DB_ERROR", "Could not retrieve " + task.toString() + " assignees for grid; It specifies a non-existent users:\n" + ExceptionUtils.getStackTrace(e));
                }

                if (TM_DashboardActivity.getRunningInstance() != null) { // TODO a more general solution.

                    // Dumb workarounds for lambda variables.
                    final String finalAssigneeString = assigneeString;
                    final String finalUnitName = unitName;

                    TM_DashboardActivity.getRunningInstance().runOnUiThread(() -> {
                        if (finalUnitName != null && finalAssigneeString != null) {
                            m_textInfo.setText(finalUnitName + " · " + finalAssigneeString);
                        } else if (finalUnitName != null) {
                            m_textInfo.setText(finalUnitName);
                            Log.d("TASK_GRID", "Set to " + finalUnitName);
                        } else if (finalAssigneeString != null) {
                            m_textInfo.setText(finalAssigneeString);
                        } else {
                            m_textInfo.setText("INFO ERROR");
                        }
                    });
                }


            }).start();

            if (task.hasChildren()) {
                m_progSepLine.setMax(task.getChildCount());
                new Thread(() -> {
                    try {
                        int progress = task.getCompletedCount();
                        m_context.runOnUiThread(() -> m_progSepLine.setProgress(progress));
                    } catch (ExecutionException | NoSuchDocumentException | InterruptedException ignored) { }
                }).start();
            } else {
                if (task.isCompleted()) {
                    m_progSepLine.setMax(1);
                    m_progSepLine.setProgress(1);
                } else {
                    m_progSepLine.setProgress(0);
                }
            }

            UIUtils.setProgressColor(m_progSepLine, task.getPriorityColor(m_context));
        }

        public View getView() {
            return itemView;
        }

    }

    public List<String> getTaskIds() {
        return m_data.stream().map(AppTask::getId).collect(Collectors.toList());
    }

    public void reset() throws ExecutionException, InterruptedException {
        m_data = TaskDB.getInstance().getRootTasks()
                .stream().sorted(AppTask.PRIORITY_COMPARATOR)
                .collect(Collectors.toList());
    }

    public void cycleUnitFilter() {
        s_unitFilter = (s_unitFilter + 1) % 3;
        refresh();
    }

    public void cycleAssignedFilter() {
        s_assignedFilter = (s_assignedFilter + 1) % 3;
        refresh();
    }

    public void cycleCompleteFilter() {
        s_completedFilter = (s_completedFilter + 1) % 3;
        refresh();
    }

    public void refresh() {

        // FIXME when this fails the filters are still ncremented

        new Thread(() -> {
            Looper.prepare();
            try {
                reset();
                applyUnitFilter(s_unitFilter);
                applyAssignedFilter(s_assignedFilter);
                applyCompletedFilter(s_completedFilter);
                sort();
                m_context.runOnUiThread(this::notifyDataSetChanged);
            } catch (ExecutionException e) {
                Toast.makeText(m_context, "Could not refresh tasks: Database error", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Could not refresh task grid:\n" + ExceptionUtils.getStackTrace(e));
            } catch (InterruptedException e) {
                Log.w("DB_ERROR", "Cancelled refresh of task grid:\n" + ExceptionUtils.getStackTrace(e));
            }
        }).start();
    }

    private void applyUnitFilter(int filter) {

        User currentUser = UserDB.getInstance().getCurrentUser();
        switch (filter) {
            case UNIT_FILTER_IN:
                filterForInUnit(currentUser);
                break;
            case UNIT_FILTER_LEADING:
                filterForLeading(currentUser);
                break;
        }

    }

    private void applyAssignedFilter(int filter) {
        if (s_assignedFilter == ASSIGN_FILTER_ASSIGNER) {
            filterForAssigner(UserDB.getInstance().getCurrentUser());
        } else if (s_assignedFilter == ASSIGN_FILTER_ASSIGNEE) {
            filterForAssigned(UserDB.getInstance().getCurrentUser());
        }
    }

    private void applyCompletedFilter(int filter) {
        switch (s_completedFilter) {
            case COMPLETE_FILTER_INCOMPLETE:
                filterForIncomplete();
                break;
            case COMPLETE_FILTER_COMPLETE:
                filterForComplete();
                break;
        }

    }

    private void filterForInUnit(User user) {
        m_data = m_data.stream().filter(task -> user.getUnitIds().contains(task.getUnitId())).collect(Collectors.toList());
    }

    private void filterForLeading(User user) {
        m_data = m_data.stream().filter(task -> user.getLeadingIds().contains(task.getUnitId())).collect(Collectors.toList());
    }

    private void filterForComplete() {
        m_data = m_data.stream().filter(Goal::isCompleted).collect(Collectors.toList());
    }

    private void filterForIncomplete() {
        m_data = m_data.stream().filter(task -> !task.isCompleted()).collect(Collectors.toList());
    }

    private void filterForAssigner(User user) {
        m_data = m_data.stream().filter(task -> task.getAssignerId().equals(user.getId())).collect(Collectors.toList());
    }

    private void filterForAssigned(User user) {
        m_data = m_data.stream().filter(task -> task.getAssigneesIds().contains(user.getId())).collect(Collectors.toList());
    }

    private synchronized void sort() {
        m_data.sort(AppTask.PRIORITY_COMPARATOR);
    }

    public List<AppTask> getData() {
        return m_data;
    }

    public void clear() {
        m_data.clear();
        notifyDataSetChanged();
    }

    public int getUnitResult() {
        return m_unitResCode;
    }

    public int getAssigneeResult() {
        return m_assigneeResCode;
    }

    public static int getUnitFilter() {
        return s_unitFilter;
    }

    public static int getAssignedFilter() {
        return s_assignedFilter;
    }

    public static int getCompletedFilter() {
        return s_completedFilter;
    }
}
