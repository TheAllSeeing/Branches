package com.evergreen.treetop.activities.tasks;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.evergreen.treetop.R;
import com.evergreen.treetop.activities.TM_DashboardActivity;
import com.evergreen.treetop.activities.goals.TM_GoalViewActivity;
import com.evergreen.treetop.activities.units.TM_UnitPickerActivity;
import com.evergreen.treetop.activities.users.TM_UserPickerActivity;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.LoggingUtils;
import com.evergreen.treetop.architecture.data.AppTask;
import com.evergreen.treetop.architecture.data.Goal;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.data.User;
import com.evergreen.treetop.architecture.handlers.GoalDB;
import com.evergreen.treetop.architecture.handlers.TaskDB;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.DBTask;
import com.evergreen.treetop.architecture.utils.DBUnit;
import com.evergreen.treetop.architecture.utils.DBUser;
import com.evergreen.treetop.architecture.utils.TaskUtils;
import com.evergreen.treetop.architecture.utils.UIUtils;
import com.evergreen.treetop.ui.views.spinner.BaseSpinner;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class TM_TaskEditorActivity extends AppCompatActivity {

    private Menu m_menuOptions;

    private EditText m_editTitle;
    private EditText m_editDescription;
    private TextView m_textUnit;
    private TextView m_textAssignees;
    private TextView m_textStartDeadline;
    private TextView m_textEndDeadline;
    private BaseSpinner m_spinPriority;

    private String m_id;
    private String m_parentId;
    private String m_rootId;
    private String m_goalId;
    private boolean m_new;

    private AppTask m_givenTask;
    private Unit m_pickedUnit;
    private List<User> m_pickedAssignees;
    private Goal m_parent;
    private final Calendar m_calendar = Calendar.getInstance();

    private static final List<String> m_editingIds = new ArrayList<>();

    public static final String PARENT_GOAL_EXTRA_KEY = "task-parent";
    public static final String TASK_ID_EXTRA_KEY = "task-id";
    public static final String IS_ROOT_TASK_EXTRA_KEY = "is-root-task";
    public static final String RESULT_TASK_EXTRA_KEY = "task-child";

    ActivityResultLauncher<Intent> m_unitPicker = registerForActivityResult(
            new StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    m_pickedUnit = Unit.of((DBUnit) result.getData().getSerializableExtra(TM_UnitPickerActivity.RESULT_PICKED_EXTRA_KEY));
                    m_textUnit.setText(m_pickedUnit.getName());
                }
            }
    );

    ActivityResultLauncher<Intent> m_userPicker = registerForActivityResult(
            new StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    ArrayList<DBUser> res =
                            (ArrayList<DBUser>) result.getData().getSerializableExtra(TM_UserPickerActivity.RESULT_SELECTED_EXTRA_KEY);

                    m_pickedAssignees = res.stream().map(User::of).collect(Collectors.toList());
                    m_textAssignees.setText(m_pickedAssignees.stream().map(User::getName).collect(Collectors.joining(", ")));
                }
            }
    );


    public static List<String> getEditingIds() {
        return m_editingIds;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_editor_tm);

        m_parentId = getIntent().getStringExtra(PARENT_GOAL_EXTRA_KEY);
        m_id = getIntent().getStringExtra(TASK_ID_EXTRA_KEY);


        if (m_id == null) {
            m_id = TaskDB.getInstance().newDoc().getId();
            m_new = true;
        } else {
            m_new = false;
        }

        new Thread(() -> {

            Looper.prepare();
            if (m_parentId == null) {
                Toast.makeText(this, "Parent task not set. aborting.", Toast.LENGTH_SHORT).show();
                cancel();
                return;
            } else {
                initRoots(); // Should be called only if m_parent is set. Initialize m_rootId and m_goalId
            }

            locateViews(); // Initializes view objects. Must be called after setContentView.
            runOnUiThread(this::initViewActions); // Should be called only after m_new is set and locateViews() was called.

            if (!m_new) {
                initEdit();
            } else {
                initNew();
            }

        }).start();
    }

    @Override
    public void onBackPressed() {
        TaskUtils.discardDialouge(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tasks_navigation_options, menu);
        getMenuInflater().inflate(R.menu.menu_tasks_editor_options, menu);
        m_menuOptions = menu;
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // Uses ifs instead of switch -- see https://sites.google.com/a/android.com/tools/tips/non-constant-fields
        if (itemId == R.id.tm_task_options_meni_dashboard) {
            startActivity(new Intent(this, TM_DashboardActivity.class));

        } else if (itemId == R.id.tm_task_options_meni_view) {
            startActivity(new Intent(this, TM_TaskViewActivity.class)
                    .putExtra(TM_TaskViewActivity.TASK_ID_EXTRA_KEY, m_id)
            );

        } else if (itemId == R.id.tm_task_options_meni_view_goal) {
            startActivity(new Intent(this, TM_GoalViewActivity.class)
                    .putExtra(TM_GoalViewActivity.GOAL_ID_EXTRA_KEY, m_givenTask.getGoalId()));

        } else if (itemId == R.id.tm_task_options_meni_view_root_task) {
            startActivity(new Intent(this, TM_TaskViewActivity.class)
                    .putExtra(TM_TaskViewActivity.TASK_ID_EXTRA_KEY, m_givenTask.getRootTaskId())
            );

        } else if (itemId == R.id.tm_task_options_meni_submit) {
            trySubmit();

        } else if (itemId == R.id.tm_task_nav_options_meni_parent) {
            goToParent();
        }

        return true;
    }

    private void goToParent() {
        if (m_parentId.equals(m_goalId)) {
            startActivity(
                    new Intent(this, TM_GoalViewActivity.class)
                    .putExtra(TM_GoalViewActivity.GOAL_ID_EXTRA_KEY, m_parentId)
            );
        } else {
            startActivity(
                    new Intent(this, TM_TaskViewActivity.class)
                    .putExtra(TM_TaskViewActivity.TASK_ID_EXTRA_KEY, m_parentId)
            );
        }
    }


    private void locateViews() {

        m_editTitle = findViewById(R.id.tm_task_editor_edit_title);
        m_editDescription = findViewById(R.id.tm_task_editor_edit_description);
        m_textUnit = findViewById(R.id.tm_task_editor_text_unit);
        m_textAssignees = findViewById(R.id.tm_task_editor_text_Assignees);
        m_textStartDeadline = findViewById(R.id.tm_task_editor_text_start_deadline);
        m_textEndDeadline = findViewById(R.id.tm_task_editor_text_end_deadline);
        m_spinPriority = findViewById(R.id.tm_task_editor_spin_priority);
    }

    private void initViewActions() {

        m_spinPriority.loadOptions(new String[]{"", "A", "B", "C", "D", "E"});
        m_textStartDeadline.setOnClickListener(this::setDeadline);
        m_textEndDeadline.setOnClickListener(this::setDeadline);

        m_textAssignees.setOnClickListener(v -> {
            if (m_pickedUnit != null) {
                m_userPicker.launch(
                        new Intent(this, TM_UserPickerActivity.class)
                                .putExtra(TM_UserPickerActivity.UNIT_ID_EXTRA_KEY, m_pickedUnit.getId())
                                .putStringArrayListExtra(TM_UserPickerActivity.INIT_SELECTED_EXTRA_KEY, new ArrayList<>(m_pickedAssignees.stream().map(User::getId).collect(Collectors.toList())))
                );
            } else {
                Toast.makeText(this, "Please pick a unit before picking assignees.", Toast.LENGTH_SHORT).show();
            }
        });

        initUnitPicker();
    }

    private void initUnitPicker() {
        new Thread(() -> {
            Looper.prepare();
            Intent unitIntent = new Intent(this, TM_UnitPickerActivity.class)
                    .putExtra(TM_UnitPickerActivity.USER_IN_FILTER_EXTRA_KEY, true);
            try {
                unitIntent.putExtra(TM_UnitPickerActivity.ROOT_UNIT_EXTRA_KEY, DBUnit.of(getParentTask().getUnit()));
                runOnUiThread(() -> {
                    m_textUnit.setOnClickListener(v -> m_unitPicker.launch(unitIntent));
                });

            } catch (ExecutionException e) {
                Log.w("DB_ERROR", "Could not initialize a root unit for UnitPicker from "
                        + LoggingUtils.stringify(m_parent) + ":\n" + ExceptionUtils.getStackTrace(e));
                Toast.makeText(this, "Could not initialize root unit. Aborting.", Toast.LENGTH_SHORT).show();
                finish();

            } catch (InterruptedException e) {
                Log.w("DB_ERROR", "Canceled initializing root unit for UnitPicker from "
                        + LoggingUtils.stringify(m_parent) + ":\n" + ExceptionUtils.getStackTrace(e));
                finish();

            } catch (NoSuchDocumentException e) {
                Log.w("DB_ERROR", "Tried to get Root unit of " + LoggingUtils.stringify(m_parent)
                        + ", but it specifies a non-existent unit:\n"
                        + ExceptionUtils.getStackTrace(e));
                Toast.makeText(this, "Could not initialize root unit. Aborting.", Toast.LENGTH_SHORT).show();
                finish();

            }
        }).start();
    }

    private void initRoots() {

        try {
            if (getIntent().getBooleanExtra(IS_ROOT_TASK_EXTRA_KEY, false)) {
                m_goalId = m_parentId;
                m_rootId = m_id;
                m_parent = GoalDB.getInstance().awaitGoal(m_parentId);

                runOnUiThread(() -> {
                    m_menuOptions.removeItem(R.id.tm_task_options_meni_view_root_task);
                });
            } else {
                m_parent = TaskDB.getInstance().awaitTask(m_parentId);
                m_rootId = ((AppTask)m_parent).getRootTaskId();
                m_goalId = ((AppTask)m_parent).getGoalId();
            }
        } catch (ExecutionException e) {
            Toast.makeText(this, "Could not identify parent. aborting.", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR", "Failed to retrieve parent" + m_parentId + ";\n" + ExceptionUtils.getStackTrace(e));
            cancel();
        } catch (InterruptedException e) {
            cancel();
        } catch (NoSuchDocumentException e) {
            Toast.makeText(this, "Could not identify parent. aborting.", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR", "specified a parent that does not exist: " + ExceptionUtils.getStackTrace(e));
            cancel();
        }

    }

    private void trySubmit() {
        if (canSubmit()) {
            submit();
        } else {
            Toast.makeText(this, "Please fill out all fields before submitting", Toast.LENGTH_LONG).show();
            Log.i("TASK_FAIL", "User tried to submit task without filling details");
        }
    }

    private void submit() {

        DBTask result = DBTask.of(getTask());
        TaskDB.getInstance().getTaskRef(m_id).set(result)
        .addOnSuccessListener( aVoid -> {

            Log.i("DB_EVENT", "Subbmitted " + result.toString());
            setResult(
                    Activity.RESULT_OK,
                    new Intent().putExtra(
                            RESULT_TASK_EXTRA_KEY,
                            result
                    )
            );

            finish();
        }).addOnFailureListener(e -> {
            Log.w("DB_ERROR", "Attempted to submit " + result.toString() + ", but failed:\n"
                    + ExceptionUtils.getStackTrace(e));
            Toast.makeText(this, "Failed to submit task: Database Error", Toast.LENGTH_SHORT).show();
        }).addOnCanceledListener(() -> Log.w("DB_ERROR", "Cancelled submission of task" + result.toString()));

    }

    private boolean canSubmit() {
        return
                getTitle() != ""
                && getPriority() != -1
                && !m_textStartDeadline.getText().toString().equals("")
                && !m_textEndDeadline.getText().toString().equals("")
                && m_pickedUnit != null
                && m_pickedAssignees != null;
    }

    private AppTask getTask() {

        AppTask result = new AppTask(
                getPriority(),
                m_id,
                getTitleText(),
                getDescription(),
                getUnitId(),
                m_parentId,
                getStartDeadline(),
                getEndDeadline(),
                getAssignerId(),
                m_goalId,
                m_rootId
        );

        getAssignees().forEach(result::addAssignee);

        if (m_givenTask != null) {
            m_givenTask.getChildrenIds().forEach(result::addChildById);
        }

        return result;
    }


    private String getTitleText() {
        return m_editTitle.getText().toString();
    }

    private int getPriority() {
        return TaskUtils.priorityNum((String)m_spinPriority.getSelectedItem());
    }

    private String getDescription() {
        return m_editDescription.getText().toString();
    }

    private String getUnitId() {
        return m_pickedUnit.getId();
    }

    private String getAssignerId() {

        if (m_givenTask != null && m_givenTask.getAssignerId() != null) {
            return m_givenTask.getAssignerId();
        }

        return UserDB.getInstance().getCurrentUserId();
    }

    private List<User> getAssignees() {
        return m_pickedAssignees;
    }

    private LocalDate getStartDeadline() {
        return LocalDate.parse(m_textStartDeadline.getText(), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private LocalDate getEndDeadline() {
        return LocalDate.parse(m_textEndDeadline.getText(), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private void setDeadline(View deadlineView) {

        // creates a new date picker dialogue.
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // changes the m_calendar to save the variables.
                    m_calendar.set(year, month, dayOfMonth);
                    ((TextView)deadlineView).setText(DateFormat.format("yyyy-MM-dd", m_calendar));
                },

                // sets the current date for the date picker, so it'll show today as the default value.
                m_calendar.get(Calendar.YEAR),
                m_calendar.get(Calendar.MONTH),
                m_calendar.get(Calendar.DAY_OF_MONTH)
        );



        // shows the dialogue.
        datePicker.show();
    }

    private void showTask(AppTask task) {

        m_spinPriority.setSelection(task.getPriority() + 1);
        m_editTitle.setText(task.getTitle());
        m_editDescription.setText(task.getDescription());
        m_textStartDeadline.setText(DateFormat.format("yyyy-MM-dd", LoggingUtils.toDate(task.getStartDeadline())));
        m_textEndDeadline.setText(DateFormat.format("yyyy-MM-dd", LoggingUtils.toDate(task.getEndDeadline())));

        new Thread( () -> {
            Looper.prepare();
            try {
                String assignees = LoggingUtils.stringify(task.getAssignees(), User::getName, false);
                runOnUiThread(() -> m_textAssignees.setText(assignees));
            } catch (InterruptedException | ExecutionException e) {
                Toast.makeText(this, "Failed to retrieve task assignees", Toast.LENGTH_LONG).show();
                Log.w("DB_ERROR", "Tried to retrieve assignees for " + task.toString()
                        + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
            } catch (NoSuchDocumentException e) {
                Toast.makeText(this, "Error: failed to identify task assignees", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Tried to retrieve unit of " + task.toString() +
                        ", but it specifies assignees that do not exist:\n" + LoggingUtils.stringify(task.getAssigneesIds()));
            }
        }).start();

        new Thread( () -> {
            Looper.prepare();
            try {
                String unitName = task.getUnit().getName();
                runOnUiThread(() -> m_textUnit.setText(unitName));
            } catch (InterruptedException | ExecutionException e) {
                Toast.makeText(this, "Failed to retrieve task unit", Toast.LENGTH_LONG).show();
                Log.w("DB_ERROR", "Tried to retrieve unit for task " + task.toString()
                        + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
            } catch (NoSuchDocumentException e) {
                Toast.makeText(this, "Error: failed to identify task unit", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Tried to retrieve unit of " + task.toString() + ", but it specifies a unit that does not exist.");
            }
        }).start();

    }

    private void initTask(String id) {

        try {
            m_givenTask = TaskDB.getInstance().awaitTask(id);
            m_editingIds.add(m_givenTask.getId());
            m_pickedUnit = m_givenTask.getUnit(); // TODO actual error handling for this
            m_pickedAssignees = new ArrayList<>(m_givenTask.getAssignees()); // TODO actual error handling for this
            Log.d("TASK_EDIT_EVENT", "GIVEN " + LoggingUtils.stringify(m_givenTask));
        } catch (ExecutionException e) {
            Log.w("DB_ERROR", "Tried to edit task from id " + m_id + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
            Toast.makeText(this, "could not retrieve task, aborting", Toast.LENGTH_LONG).show();
            cancel();
        } catch (InterruptedException e) {
            Log.w("DB_ERROR", "Tried to edit task from id " + m_id + ", but the action was cancelled.");
            cancel();
        } catch (NoSuchDocumentException e) {
            Log.w("DB_ERROR", "Tried to edit task from id " + m_id + ", but there is no such document!");
            Toast.makeText(this, "Could not find the given task. Aborting.", Toast.LENGTH_SHORT).show();
            cancel();
        }

        Log.d("TASK_EDIT_EVENT", "GIVEN " + LoggingUtils.stringify(m_givenTask));
    }



    private void initNew() {
        m_pickedAssignees = new ArrayList<>();

        try {
            m_pickedUnit = getParentTask().getUnit();
            runOnUiThread(() -> m_textUnit.setText(m_pickedUnit.getName()));
        } catch (InterruptedException | ExecutionException | NoSuchDocumentException e) {
            Log.w("DB_ERROR", "Failed to retrieve parent unit of new task:\n" + ExceptionUtils.getStackTrace(e));
        }
    }

    private void initEdit() {

        runOnUiThread(() -> UIUtils.showLoading(this, R.id.tm_task_editor_constr_layout)); // Shows loading

        initTask(m_id); // Must be called after Looper.prepare()

        runOnUiThread(() -> {
            showTask(m_givenTask); // Must be called after Looper.prepare()
            UIUtils.hideLoading(this, R.id.tm_task_editor_constr_layout);
        });
    }

    private Goal getParentTask() {
        return m_parent;
    }

    @Override
    protected void onDestroy() {

        if (!m_new) {
            m_editingIds.remove(m_givenTask.getId());
        }

        super.onDestroy();
    }

    private void cancel() {

        if (getCallingActivity() != null) {
            setResult(Activity.RESULT_CANCELED);
        }

        finish();
    }
}