package com.evergreen.treetop.architecture.data;

import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.handlers.GoalDB;
import com.evergreen.treetop.architecture.handlers.TaskDB;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.DBTask;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class AppTask extends Goal {
    private final String m_parentId;
    private LocalDate m_startDeadline;
    private LocalDate m_endDeadline;
    private final String m_assignerId;
    private final String m_goalId;
    private final String m_rootTaskId;
    private Set<String> m_assigneesIds;


    public AppTask(int priority, String id, String title, String description, String unitId, String parentId,
                   LocalDate startDeadline, LocalDate endDeadline, String assignerId, String goalId, String rootTaskId) {
        super(priority, id, title, description, unitId);
        m_parentId = parentId;
        m_startDeadline = startDeadline;
        m_endDeadline = endDeadline;
        m_assignerId = assignerId;
        m_goalId = goalId;
        m_rootTaskId = rootTaskId;
        m_assigneesIds = new HashSet<>();
    }

    public static AppTask of(DBTask task) {
        AppTask res = new AppTask(
                task.getPriority(),
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getUnitId(),
                task.getParentId(),
                LocalDate.ofEpochDay(task.getStartDeadlineEpoch()),
                LocalDate.ofEpochDay(task.getEndDeadlineEpoch()),
                task.getAssignerId(),
                task.getGoalId(),
                task.getRootTaskId()
        );

        // For testing with old data. TODO remove.
        if (task.getAssigneeIds() != null) {
            task.getAssigneeIds().forEach(res::addAssigneeById);
        }

        task.getSubtaskIds().forEach(res::addChildById);
        res.setCompleted(task.isCompleted());

        return res;
    }

    /**
     * By default {@link ArrayAdapter} uses {@link #toString} to determine how to display the
     * object as text. However, toString is conventionally used for debugging, where we might want
     * to add other details. Thus, this method is used to provide a textual representation of the
     * task to display on listViews, etc.
     *
     * @deprecated dumb and unnecessary when using recycler view anyway.
     *
     * @return a textual representation of the task to display in list views.
     */
    public String listDisplayString() {
        return getTitle();
    }

    public LocalDate getStartDeadline() {
        return m_startDeadline;
    }

    public void setStartDeadline(LocalDate startDeadline) {
        m_startDeadline = startDeadline;
    }

    public LocalDate getEndDeadline() {
        return m_endDeadline;
    }

    public void setEndDeadline(LocalDate endDeadline) {
        m_endDeadline = endDeadline;
    }

    public String getParentId() {
        return m_parentId;
    }

    public Goal getParent() throws ExecutionException, InterruptedException, NoSuchDocumentException {
        return GoalDB.getInstance().awaitGoal(m_parentId);
    }

    public String getGoalId() {
        return m_goalId;
    }

    public Goal getGoal() throws ExecutionException, InterruptedException, NoSuchDocumentException {
        return getRootTask().getParent();
    }

    public String getRootTaskId() {
        return m_rootTaskId;
    }

    public AppTask getRootTask() throws ExecutionException, InterruptedException, NoSuchDocumentException {
        return TaskDB.getInstance().awaitTask(m_rootTaskId);
    }

    public String getAssignerId() {
        return m_assignerId;
    }

    public User getAssigner() throws ExecutionException, InterruptedException, NoSuchDocumentException {
        return UserDB.getInstance().awaitUser(m_assignerId);
    }

    public Set<String> getAssigneesIds() {
        return m_assigneesIds;
    }

    public Set<User> getAssignees() throws ExecutionException, InterruptedException, NoSuchDocumentException {

        Set<User> res = new HashSet<>();
        for (String id : m_assigneesIds) {
            res.add(UserDB.getInstance().awaitUser(id));
        }

        return res;

    }

    public void addAssignee(User assignee) {
        m_assigneesIds.add(assignee.getId());
    }
    public void addAssigneeById(String id) {
        m_assigneesIds.add(id);
    }

    public void removeAssignee(User assignee) {
        m_assigneesIds.remove(assignee.getId());
    }

    public boolean isRootTask() {
        return m_rootTaskId.equals(getId());
    }



    @Override
    @NonNull
    public String toString() {
        return "Task " + getId() + " (" + getTitle() + ")";
    }
}
