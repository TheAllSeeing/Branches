package com.evergreen.treetop.activities.goals;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.evergreen.treetop.R;
import com.evergreen.treetop.activities.TM_DashboardActivity;
import com.evergreen.treetop.activities.tasks.TM_TaskEditorActivity;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.data.AppTask;
import com.evergreen.treetop.architecture.data.Goal;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.handlers.GoalDB;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.DBGoal.GoalDBKey;
import com.evergreen.treetop.architecture.utils.DBTask;
import com.evergreen.treetop.architecture.utils.UIUtils;
import com.evergreen.treetop.ui.views.recycler.TaskListRecycler;
import com.google.firebase.firestore.FieldValue;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class TM_GoalViewActivity extends AppCompatActivity {

    private Menu m_menuOptions;

    private View m_viewPriority;
    private TextView m_textTitle;
    private TextView m_textDescription;
    private ProgressBar m_progressBar;
    private TaskListRecycler m_listSubtasks;

    private String m_id;
    private Unit m_unit;
    private Goal m_goalToDisplay;

    private boolean firstRun;

    public static final String GOAL_ID_EXTRA_KEY = "goal-id";

    ActivityResultLauncher<Intent> m_subtaskLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {

                    assert result.getData() != null : "If result is OK then it should contain data!";

                    DBTask newTask = (DBTask) result.getData().getSerializableExtra(TM_TaskEditorActivity.RESULT_TASK_EXTRA_KEY);

                    GoalDB.getInstance().update(
                            m_id,
                            GoalDBKey.SUBTASK_IDS,
                            FieldValue.arrayUnion(newTask.getId())
                    );
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_view_tm);

        firstRun = true;

        m_id = getIntent().getStringExtra(GOAL_ID_EXTRA_KEY);

        if (m_id == null) {
            Toast.makeText(this, "Goal not specified. Aborting.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        m_textTitle = findViewById(R.id.tm_goal_view_text_title);
        m_textDescription = findViewById(R.id.tm_goal_view_text_description);
        m_viewPriority = findViewById(R.id.tm_goal_view_img_priority);
        m_progressBar = findViewById(R.id.tm_goal_view_prog_progress);
        m_listSubtasks = findViewById(R.id.tm_goal_view_list_subtasks);

        UIUtils.showLoading(this, R.id.tm_goal_view_constr_layout);

        new Thread( () -> {
            Looper.prepare();
            UIUtils.showLoading(this, R.id.tm_goal_view_constr_layout);
            initGoal();
            runOnUiThread(() -> {
                showGoal();
                UIUtils.hideLoading(this, R.id.tm_goal_view_constr_layout);
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (firstRun) {
            firstRun = false;
        } else if (m_id != null) {
            reloadGoal();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        m_menuOptions = menu;
        getMenuInflater().inflate(R.menu.menu_goals_navigation_options, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.tm_goal_options_meni_dashboard) {
            startActivity(new Intent(this, TM_DashboardActivity.class));

        } else if (itemId == R.id.tm_goal_options_meni_delete) {
            UIUtils.deleteGoalDialouge(this, m_goalToDisplay);

        } else if (itemId == R.id.tm_goal_options_meni_edit_mode) {
            startActivity(
                    new Intent(this, TM_GoalEditorActivity.class)
                            .putExtra(TM_GoalEditorActivity.GOAL_ID_EXTRA_KEY, m_id)
            );

        } else if (itemId == R.id.tm_goal_options_meni_toggle_complete) {
            setCompleted(!m_goalToDisplay.isCompleted());

        } else if (itemId == R.id.tm_goal_options_owner_meni_add_subtask) {
            m_subtaskLauncher.launch(
                    new Intent(this, TM_TaskEditorActivity.class)
                    .putExtra(TM_TaskEditorActivity.PARENT_GOAL_EXTRA_KEY, m_id)
                    .putExtra(TM_TaskEditorActivity.IS_ROOT_TASK_EXTRA_KEY, true)
            );
        }

        return true;
    }

    private void showCompleted(boolean completedStatus) {

        MenuItem completeToggle =
                m_menuOptions.findItem(R.id.tm_goal_options_meni_toggle_complete);

        if (completedStatus) {
            m_viewPriority.setBackground(ContextCompat.getDrawable(this, R.drawable.circle));

            if (!m_goalToDisplay.hasChildren()) {
                m_progressBar.setMax(1);
                m_progressBar.setProgress(1);
            }

            if (completeToggle != null) { // Null if user does not have permission to toggle
                completeToggle.setTitle("Set Incomplete");
            }



        } else {
            m_viewPriority.setBackground(ContextCompat.getDrawable(this, R.drawable.ring));

            if (!m_goalToDisplay.hasChildren()) {
                m_progressBar.setProgress(0);
            }

            if (completeToggle != null) { // Null if user does not have permission to toggle
                completeToggle.setTitle("Set Complete");
            }
        }
        UIUtils.setBackgroundColor(m_viewPriority, m_goalToDisplay.getPriorityColor(this));
    }

    private void setCompleted(boolean completed) {
        UIUtils.showLoading(this, R.id.tm_goal_view_constr_layout);
        GoalDB.getInstance().update(m_id, GoalDBKey.COMPLETED, completed)
                .addOnSuccessListener(aVoid -> {
                    m_goalToDisplay.setCompleted(completed);
                    setTitle(getUnitName());
                    showCompleted(completed);
                    UIUtils.hideLoading(this, R.id.tm_goal_view_constr_layout);
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not toggle task complete: Database error", Toast.LENGTH_SHORT).show();
                    Log.w("DB_ERROR", "Tried to set " + m_id + "(" +
                            m_goalToDisplay.getTitle() + "), but failed:\n" + ExceptionUtils.getStackTrace(e));
                    UIUtils.hideLoading(this, R.id.tm_goal_view_constr_layout);
                }).addOnCanceledListener(() -> {
                    Toast.makeText(this, "Action canceled", Toast.LENGTH_SHORT).show();
                    UIUtils.hideLoading(this, R.id.tm_goal_view_constr_layout);
                });
    }

    private void reloadGoal() {
        UIUtils.showLoading(this, R.id.tm_goal_view_constr_layout);
        new Thread( () -> {
            reinitGoal();
            runOnUiThread(() -> {
                showGoal();
                UIUtils.hideLoading(this, R.id.tm_goal_view_constr_layout);
            });
        }).start();
    }


    public void reinitGoal() {

        try {
            m_goalToDisplay = GoalDB.getInstance().awaitGoal(m_id);

            try {
                m_unit = m_goalToDisplay.getUnit();
            }  catch (ExecutionException e) {
                Log.w("DB_ERROR", "Tried to refresh unit of " + m_goalToDisplay.toString() + ", but failed: \n" + ExceptionUtils.getStackTrace(e));
                Toast.makeText(this, "Could not refresh goal. Info possibly outdated.", Toast.LENGTH_SHORT).show();
            } catch (InterruptedException e) {
                Log.w("DB_ERROR", "Canceled refreshing unit of" + m_goalToDisplay.toString() + ":\n" + ExceptionUtils.getStackTrace(e));
            } catch (NoSuchDocumentException e) {
                Log.w("DB_ERROR", "Tried to reload unit of" + m_id + ", but there is no such document:\n" + ExceptionUtils.getStackTrace(e));
                Toast.makeText(this, "Error: unit no longer exists.", Toast.LENGTH_SHORT).show();
            }

        } catch (ExecutionException e) {
            Log.w("DB_ERROR", "Tried to refresh goal from id " + m_id + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
            Toast.makeText(this, "Could not refresh goal. Info possibly outdated.", Toast.LENGTH_SHORT).show();
        } catch (InterruptedException e) {
            Log.w("DB_ERROR", "Canceled refreshing goal from id " + m_id + ":\n" + ExceptionUtils.getStackTrace(e));
        } catch (NoSuchDocumentException e) {
            Log.w("DB_ERROR", "Tried to reload goal from id " + m_id + ", but there is no such document:\n" + ExceptionUtils.getStackTrace(e));
            Toast.makeText(this, "Error: Task no longer exists.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    public void initGoal() {

        try {
            m_goalToDisplay = GoalDB.getInstance().awaitGoal(m_id);

        }  catch (ExecutionException e) {
            Log.w("DB_ERROR", "Tried to display goal from id " + m_id + ", but failed: \n" + ExceptionUtils.getStackTrace(e));
            Toast.makeText(this, "Could not find the given goal. Aborting.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (InterruptedException e) {
            Log.w("DB_ERROR", "Canceled display goal from id " + m_id + ", but the action was cancelled:\n" + ExceptionUtils.getStackTrace(e));
            finish();
        } catch (NoSuchDocumentException e) {
            Log.w("DB_ERROR", "Tried to display goal from id " + m_id + ", but there is no such document:\n" + ExceptionUtils.getStackTrace(e));
            Toast.makeText(this, "Could not find the given task. Aborting.", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (m_goalToDisplay != null) {
            try {
                m_unit = m_goalToDisplay.getUnit();

                runOnUiThread(() -> {
                    if (UserDB.getInstance().getCurrentUser().isIn(m_unit)) {
                        getMenuInflater().inflate(R.menu.menu_goals_member_options, m_menuOptions);
                    }

                    if (UserDB.getInstance().getCurrentUser().isLeading(m_unit)) {
                        getMenuInflater().inflate(R.menu.menu_goals_leader_options, m_menuOptions);

                        if (TM_GoalEditorActivity.getEditingIds().contains(m_id)) {
                            m_menuOptions.removeItem(R.id.tm_goal_options_meni_edit_mode);
                        }
                    }
                });

            }  catch (ExecutionException e) {
                Log.w("DB_ERROR", "Tried to display unit of " + m_goalToDisplay.toString() + ", but failed: \n" + ExceptionUtils.getStackTrace(e));
                Toast.makeText(this, "Could not find the goal unit.", Toast.LENGTH_SHORT).show();
            } catch (InterruptedException e) {
                Log.w("DB_ERROR", "Canceled display unit of " + m_goalToDisplay.toString()+ ", but the action was cancelled: " + ExceptionUtils.getStackTrace(e));
            } catch (NoSuchDocumentException e) {
                Log.w("DB_ERROR", "Tried to display unit of " + m_goalToDisplay.toString() + ", but there is no such document:" + ExceptionUtils.getStackTrace(e));
                Toast.makeText(this, "Could not identify unit.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showGoal() {

        Goal goal = m_goalToDisplay;

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(goal.getPriorityColor(this)));
        getSupportActionBar().setIcon(R.drawable.ic_app);
        showCompleted(goal.isCompleted());
        setTitle(getUnitName());


        m_textTitle.setText(goal.getTitle());
        m_textDescription.setText(goal.getDescription());

        UIUtils.setBackgroundColor(m_viewPriority, m_goalToDisplay.getPriorityColor(this));

        UIUtils.setProgressColor(m_progressBar, goal.getPriorityColor(this));

        if (goal.hasChildren()) {
            m_progressBar.setMax(goal.getChildCount());
            new Thread(() -> {
                Looper.prepare();
                loadSubtasks(goal);
            }).start();
        } else if (goal.isCompleted()) {
            m_progressBar.setMax(1);
            m_progressBar.setProgress(1);
        }

    }

    private void loadSubtasks(Goal goal) {
        try {
            List<AppTask> subtasks = goal.getChildren();
            runOnUiThread( () -> {
                m_progressBar.setMax(subtasks.size());
                m_progressBar.setProgress((int)subtasks.stream().filter(AppTask::isCompleted).count());
                m_listSubtasks.getAdapter().clear();
                m_listSubtasks.loadTasks(subtasks);
            });
        } catch (InterruptedException | ExecutionException e) {
            Toast.makeText(this, "Could not retrieve some subtasks", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR", "Tried to retrieve subtasks of " + goal.toString() + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
        } catch (NoSuchDocumentException e) {
            Toast.makeText(this, "Error: given tasks describes subtasks that do not exist", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR", "Tried to retrieve subtasks of " + goal.toString() + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
        }
    }

    private String getUnitName() {
        return m_unit == null ? "" : m_unit.getName();
    }


}