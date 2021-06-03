package com.evergreen.treetop.activities.tasks;

import android.Manifest.permission;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.CalendarContract;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.evergreen.treetop.R;
import com.evergreen.treetop.activities.TM_DashboardActivity;
import com.evergreen.treetop.activities.goals.TM_GoalViewActivity;
import com.evergreen.treetop.architecture.LoggingUtils;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.data.AppTask;
import com.evergreen.treetop.architecture.data.User;
import com.evergreen.treetop.architecture.handlers.TaskDB;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.DBTask;
import com.evergreen.treetop.architecture.utils.DBTask.TaskDBKey;
import com.evergreen.treetop.architecture.utils.UIUtils;
import com.evergreen.treetop.ui.views.recycler.TaskListRecycler;
import com.google.firebase.firestore.FieldValue;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TM_TaskViewActivity extends AppCompatActivity {

    private Menu m_menuOptions;

    private TextView m_textTitle;
    private TextView m_textDescription;
    private TextView m_textStartDeadline;
    private TextView m_textEndDeadline;
    private TextView m_textAssigner;
    private TextView m_textAssignees;
    private TextView m_textPriority;
    private TextView m_textUnit;
    private View m_viewCompleted;
    private ProgressBar m_progressBar;
    private TaskListRecycler m_listSubtasks;

    private String m_id;
    private AppTask m_taskToDisplay;

    private static final int START_CAL_PERMISSION_REQ = 0;
    private static final int END_CAL_PERMISSION_REQ = 1;

    public static final String TASK_ID_EXTRA_KEY = "task-id";
    /**
     * marks that the edit mode buttons should go back rather than open a new one
     */
    public static final String COMING_FROM_EDIT_EXTRA_KEY = "come-from-edit";


    /**
     * Launcher for creating a subtask via {@link TM_TaskEditorActivity}.
     */
    ActivityResultLauncher<Intent> m_subtaskLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Get created task from the activity result
                    DBTask newTask = (DBTask) result.getData().getSerializableExtra(TM_TaskEditorActivity.RESULT_TASK_EXTRA_KEY);
                    // Update this task accordingly
                    TaskDB.getInstance().update(m_id, TaskDBKey.SUBTASK_IDS, FieldValue.arrayUnion(newTask.getId()));
                    // Update UI accordingly
                    m_listSubtasks.getAdapter().add(AppTask.of(newTask));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_view_tm);
        m_id = getIntent().getStringExtra(TASK_ID_EXTRA_KEY);

        if (m_id == null) {
            Toast.makeText(this, "Could not find task id. Aborting.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        m_textTitle = findViewById(R.id.tm_task_view_text_title);
        m_textDescription = findViewById(R.id.tm_task_view_text_description);
        m_textStartDeadline = findViewById(R.id.tm_task_view_text_start_deadline);
        m_textEndDeadline = findViewById(R.id.tm_task_view_text_end_deadline);
        m_textAssigner = findViewById(R.id.tm_task_view_text_assigner);
        m_textAssignees = findViewById(R.id.tm_task_view_text_assignees);
        m_textPriority = findViewById(R.id.tm_task_view_text_priority);
        m_textUnit = findViewById(R.id.tm_task_view_text_unit);
        m_viewCompleted = findViewById(R.id.tm_task_view_view_priority_completed);
        m_progressBar = findViewById(R.id.tm_task_view_prog_progress);
        m_listSubtasks = findViewById(R.id.tm_task_view_list_subtasks);

        UIUtils.showLoading(this, R.id.tm_task_view_constr_layout);

        new Thread(() -> {
            initTask();
            // In order to not get NullPointerException when methods continue to run after cancel:
            if (m_taskToDisplay == null) return;

            Log.i("DB_EVENT", "Retrieved task " + m_taskToDisplay.toString());
            runOnUiThread(() -> {
                showTask(m_taskToDisplay);
                UIUtils.hideLoading(this, R.id.tm_task_view_constr_layout);
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (m_taskToDisplay != null) { // Required check for first method calls
            new Thread(this::reloadTask).start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tasks_navigation_options, menu);
        m_menuOptions = menu;
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // Uses ifs instead of switch -- see https://sites.google.com/a/android.com/tools/tips/non-constant-fields
        if (itemId == R.id.tm_task_options_meni_dashboard) {
            startActivity(new Intent(this, TM_DashboardActivity.class));

        } else if (itemId == R.id.tm_task_options_meni_edit_mode) {
            goToEdit();

        } else if (itemId == R.id.tm_task_options_meni_toggle_complete) {
            setCompleted(!m_taskToDisplay.isCompleted());

        } else if (itemId == R.id.tm_task_options_meni_view_goal) {
            startActivity(
                    new Intent(this, TM_GoalViewActivity.class)
                    .putExtra(TM_GoalViewActivity.GOAL_ID_EXTRA_KEY, m_taskToDisplay.getGoalId())
            );

        } else if (itemId == R.id.tm_task_options_meni_view_root_task) {
            startActivity(
                    new Intent(this, TM_TaskViewActivity.class)
                    .putExtra(TM_TaskViewActivity.TASK_ID_EXTRA_KEY, m_taskToDisplay.getRootTaskId())
            );

        } else if (itemId == R.id.tm_task_options_meni_delete) {
            UIUtils.deleteTaskDialouge(this, m_taskToDisplay);

        } else if (itemId == R.id.tm_task_nav_options_meni_parent) {
            goToParent();

        } else if (itemId == R.id.tm_task_options_owner_meni_add_subtask) {
            m_subtaskLauncher.launch(
                    new Intent(this, TM_TaskEditorActivity.class)
                    .putExtra(TM_TaskEditorActivity.PARENT_GOAL_EXTRA_KEY, m_id)
            );
        } else if (itemId == R.id.tm_task_options_meni_add_event_start) {
            addAsEvent(true, START_CAL_PERMISSION_REQ);
        } else if (itemId == R.id.tm_task_options_meni_add_event_end) {
            addAsEvent(false, END_CAL_PERMISSION_REQ);
        }


        return true;
    }

    private void goToParent() {
        if (m_taskToDisplay.isRootTask()) {
            startActivity(
                    new Intent(this, TM_GoalViewActivity.class)
                            .putExtra(TM_GoalViewActivity.GOAL_ID_EXTRA_KEY, m_taskToDisplay.getParentId())
            );
        } else {
            startActivity(
                    new Intent(this, TM_TaskViewActivity.class)
                            .putExtra(TM_TaskViewActivity.TASK_ID_EXTRA_KEY, m_taskToDisplay.getParentId())
            );
        }
    }

    private void goToEdit() {

        if (getIntent().getBooleanExtra(COMING_FROM_EDIT_EXTRA_KEY, false)) {
            finish();
        } else {
            startActivity(new Intent(this, TM_TaskEditorActivity.class)
                    .putExtra(TM_TaskEditorActivity.TASK_ID_EXTRA_KEY, m_id)
                    .putExtra(TM_TaskEditorActivity.PARENT_GOAL_EXTRA_KEY, m_taskToDisplay.getParentId())
                    .putExtra(TM_TaskEditorActivity.IS_ROOT_TASK_EXTRA_KEY, m_taskToDisplay.isRootTask())
            );
        }
    }

    private void setCompleted(boolean completed) {

        UIUtils.showLoading(this, R.id.tm_task_view_constr_layout);
        new Thread(() -> {
            Looper.prepare();

            try {
                TaskDB.getInstance().update(m_id, TaskDBKey.COMPLETED, completed);
                runOnUiThread(() -> {
                    m_taskToDisplay.setCompleted(completed);
                    showCompleted(completed);
                    UIUtils.hideLoading(this, R.id.tm_task_view_constr_layout);
                });
            } catch (ExecutionException e) {
                Toast.makeText(this, "Could not toggle task complete: Database error", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Tried to set task " + m_id + "(" +
                        m_taskToDisplay.getTitle() + "), but failed:\n" + ExceptionUtils.getStackTrace(e));
                UIUtils.hideLoading(this, R.id.tm_task_view_constr_layout);

            } catch (InterruptedException e) {
                Toast.makeText(this, "Action canceled", Toast.LENGTH_SHORT).show();
                UIUtils.hideLoading(this, R.id.tm_task_view_constr_layout);
            }
        }).start();
    }

    private void showCompleted(boolean completedStatus) {
        MenuItem completeToggle =
                m_menuOptions.findItem(R.id.tm_task_options_meni_toggle_complete);

        if (completedStatus) {
            m_viewCompleted.setBackground(ContextCompat.getDrawable(this, R.drawable.circle));

            if (!m_taskToDisplay.hasChildren()) {
                m_progressBar.setMax(1);
                m_progressBar.setProgress(1);
            }

            if (completeToggle != null) { // Null if user does not have permission to toggle
                completeToggle.setTitle("Set Incomplete");
            }


        } else {
            m_viewCompleted.setBackground(ContextCompat.getDrawable(this, R.drawable.ring));

            if (!m_taskToDisplay.hasChildren()) {
                m_progressBar.setProgress(0);
            }

            if (completeToggle != null) { // Null if user does not have permission to toggle
                completeToggle.setTitle("Set Complete");
            }
        }

        UIUtils.setBackgroundColor(m_viewCompleted, m_taskToDisplay.getPriorityColor(this));
    }

    private void addAsEvent(boolean start, int requestCode) {

        if (ContextCompat.checkSelfPermission(this, permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {permission.WRITE_CALENDAR, permission.READ_CALENDAR}, requestCode);
            return;
        }

        LocalDate day = start? m_taskToDisplay.getStartDeadline() : m_taskToDisplay.getEndDeadline();

        // Start at midnight exactly, lasting exactly one day  -- this will generate a whole day evemt.
        LocalDateTime startInstant = LocalDateTime.of(day, LocalTime.MIDNIGHT);
        long startEpochMillis = startInstant.toEpochSecond(OffsetDateTime.now().getOffset()) * 1000;
        long endEpochMillis = startEpochMillis + TimeUnit.DAYS.toMillis(1);

        // this handles the event creation.
        ContentResolver eventResolver = getContentResolver();
        // this saves the event details (title, description, starting time, ending time, location).
        ContentValues eventValues = new ContentValues();

        eventValues.put(CalendarContract.Events.DTSTART, startEpochMillis);
        eventValues.put(CalendarContract.Events.DTEND, endEpochMillis);
        eventValues.put(CalendarContract.Events.TITLE, m_taskToDisplay.getTitle() + " — " + (start? "Start" : "End") + " Deadline");
        eventValues.put(CalendarContract.Events.DESCRIPTION, m_taskToDisplay.getDescription());
        // sets the event to be in the first account on the phone.
        eventValues.put(CalendarContract.Events.CALENDAR_ID, 1);
        // sets the event to be in the default timezone.
        eventValues.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        // saves the event.
        Uri res = eventResolver.insert(CalendarContract.Events.CONTENT_URI, eventValues);

        // sets an alert dialogue that notifies the user that the event was saved.
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);

        if (res != null) {
            alertBuilder.setMessage("Event saved!\nCheck out your system calendar.");
        } else {
            alertBuilder.setMessage("Failed to save event.");
        }

        alertBuilder.setPositiveButton("OK", null);
        alertBuilder.create().show();
    }

    private void initTask() {
        Looper.prepare();
        try {
            m_taskToDisplay = TaskDB.getInstance().awaitTask(m_id);

            runOnUiThread(() -> {
                m_menuOptions.clear();

                if (UserDB.getInstance().getCurrentUser().isAssignerOf(m_taskToDisplay)) {
                    getMenuInflater().inflate(R.menu.menu_tasks_assigner_options, m_menuOptions);

                    if (TM_TaskEditorActivity.getEditingIds().contains(m_id)) {
                        m_menuOptions.removeItem(R.id.tm_task_options_meni_edit_mode);
                    }
                }

                if (UserDB.getInstance().getCurrentUser().isAssignedTo(m_taskToDisplay)) {
                    getMenuInflater().inflate(R.menu.menu_tasks_assignee_options, m_menuOptions);
                }

                getMenuInflater().inflate(R.menu.menu_tasks_navigation_options, m_menuOptions);

                if (m_taskToDisplay.isRootTask()) {
                    m_menuOptions.removeItem(R.id.tm_task_options_meni_view_root_task);
                }

            });

        } catch (ExecutionException e) {
            Log.w("DB_ERROR", "Tried to display task from id " + m_id + ", but failed: \n" + ExceptionUtils.getStackTrace(e));
            Toast.makeText(this, "Could not find the given task. Aborting.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (InterruptedException e) {
            Log.w("DB_ERROR", "Canceled display pf task from id " + m_id + ": \n" + ExceptionUtils.getStackTrace(e));
            finish();
        } catch (NoSuchDocumentException e) {
            Log.w("DB_ERROR", "Tried to display task from id " + m_id + ", but there is no such document.");
            Toast.makeText(this, "Could not find the given task. Aborting.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void reloadTask() {
        Looper.prepare();
        try {
            m_taskToDisplay = TaskDB.getInstance().awaitTask(m_id);
            runOnUiThread( () -> showTask(m_taskToDisplay));
        } catch (ExecutionException e) {
            Log.w("DB_ERROR", "Tried to refresh task from id " + m_id + ", but failed: \n" + ExceptionUtils.getStackTrace(e));
            Toast.makeText(this, "Could refresh task.", Toast.LENGTH_SHORT).show();
        } catch (InterruptedException e) {
            Log.w("DB_ERROR", "Canceled refreshing task from id " + m_id + ": \n" + ExceptionUtils.getStackTrace(e));
        } catch (NoSuchDocumentException e) {
            Log.w("DB_ERROR", "Tried to reload task from id " + m_id + ", but there is no such document.");
            Toast.makeText(this, "Error: Task no longer exists.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private void loadSubtasks(AppTask task) {
        try {
            List<AppTask> subtasks = task.getChildren();
            m_progressBar.setMax(task.getChildCount());
            runOnUiThread( () -> {
                m_progressBar.setProgress((int)subtasks.stream().filter(AppTask::isCompleted).count());
                m_listSubtasks.getAdapter().clear();
                m_listSubtasks.loadTasks(subtasks);
            });
        } catch (InterruptedException | ExecutionException e) {
            Toast.makeText(this, "Could not retrieve some subtasks", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR", "Tried to retrieve subtasks of " + task.toString() + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
        } catch (NoSuchDocumentException e) {
            Toast.makeText(this, "Error: given tasks describes subtasks that do not exist", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR", "Tried to retrieve subtasks of " + task.toString() + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
        }
    }


    private void showTask(AppTask task) {
        m_id = task.getId();

        // Show Priority char
        // Show title
        m_textTitle.setText(task.getTitle());
        // Show description
        m_textDescription.setText(task.getDescription());
        // Show start deadline
        m_textStartDeadline.setText(task.getStartDeadline().format(DateTimeFormatter.ISO_LOCAL_DATE));
        // Show end deadline
        m_textEndDeadline.setText(task.getEndDeadline().format(DateTimeFormatter.ISO_LOCAL_DATE));
        // Show priority char
        m_textPriority.setText(task.priorityChar());
        // Show completed & priority color
        showCompleted(task.isCompleted());
        // Shoe priority color
        UIUtils.setProgressColor(m_progressBar, task.getPriorityColor(this));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(task.getPriorityColor(this)));

        new Thread( () -> {
            Looper.prepare();

            try {
                m_textUnit.setText(task.getUnit().getName());
            } catch (ExecutionException e) {
                Toast.makeText(this, "Error: could not retrieve unit.", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR",
                        "Tried to retrieve unit of " + task.toString()
                                + "from id " + task.getUnitId() + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
            } catch (InterruptedException e) {
                Log.w(
                        "DB_ERROR",
                        "Canceled display of unit " + task.getUnitId()
                                + " from " + task.toString()
                );
            } catch (NoSuchDocumentException e) {
                Toast.makeText(this, "Error: could not identify task's unit.", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR",
                        "Tried to retrieve unit of " + task.toString()
                                + "from id " + task.getUnitId() + ", but no such unit exists");
            }
        }).start();

        new Thread(() -> {
            Looper.prepare();

            try {
                m_textAssigner.setText(task.getAssigner().getName());
            } catch (ExecutionException e) {
                Toast.makeText(this, "Error: could not retrieve assigner.", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR",
                        "Tried to retrieve assigner of " + task.toString() + " from id "
                                + task.getAssignerId() + ", but failed:\n"
                                + ExceptionUtils.getStackTrace(e));
            } catch (InterruptedException e) {
                Log.w(
                        "DB_ERROR",
                        "Canceled display of assigner " + task.getAssignerId() + " from "
                                + task.toString()
                );
            } catch (NoSuchDocumentException e) {
                Toast.makeText(this, "Error: could not identify task's unit.", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR",
                        "Tried to retrieve assigner of " + task.toString()
                                + " from id " + task.getAssignerId() + ", but no such user exists");
            }
        }).start();

        new Thread(() -> {
            Looper.prepare();

            try {
                m_textAssignees.setText(task.getAssignees().stream().map(User::getName).collect(Collectors.joining(", ")));
            } catch (ExecutionException e) {
                Toast.makeText(this, "Error: could not retrieve some assignees.", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR",
                        "Tried to retrieve assignees of " + task.toString() + "from id "
                                + LoggingUtils.stringify(task.getAssigneesIds()) + ", but failed:\n"
                                + ExceptionUtils.getStackTrace(e));
            } catch (InterruptedException e) {
                Log.w(
                        "DB_ERROR",
                        "Canceled display of assignees from " + task.toString()
                );
            } catch (NoSuchDocumentException e) {
                Toast.makeText(this,
                        "Error: could not identify some of s assignees.",
                        Toast.LENGTH_SHORT).show();

                Log.w("DB_ERROR",
                        "Tried to retrieve assignees of " + task.toString()
                                + ", but they contain non-existent users");
            }
        }).start();


        if (task.hasChildren()) {
            new Thread(() -> {
                Looper.prepare();
                loadSubtasks(task);
            }).start();
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case START_CAL_PERMISSION_REQ:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    addAsEvent(true, START_CAL_PERMISSION_REQ);
                }
                break;
            case END_CAL_PERMISSION_REQ:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    addAsEvent(false, END_CAL_PERMISSION_REQ);
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}