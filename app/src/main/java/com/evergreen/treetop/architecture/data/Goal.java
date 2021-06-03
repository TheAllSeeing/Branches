package com.evergreen.treetop.architecture.data;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.evergreen.treetop.R;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.handlers.TaskDB;
import com.evergreen.treetop.architecture.handlers.UnitDB;
import com.evergreen.treetop.architecture.utils.DBGoal;
import com.evergreen.treetop.architecture.utils.TaskUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class Goal {
    private int m_priority;
    private boolean m_completed;
    private String m_id;
    private String m_title;
    private String m_description;
    private Set<String> m_subtaskIds;
    private String m_unitId;

    public final static String COMPLETE_ICON = "✓"; // ✔
    public final static String INCOMPLETE_ICON = "";

    public static final Comparator<Goal> PRIORITY_COMPARATOR =
            (goal1, goal2) -> Integer.compare(goal1.getPriority(), goal2.getPriority());

    public static Goal of(DBGoal goal)  {
        Goal result = new Goal(
                goal.getPriority(),
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getUnitId()
        );
        
        goal.getSubtaskIds().forEach(result::addChildById);
        result.setCompleted(goal.isCompleted());

        return result;
        
    }

    public Goal(int priority, String id, String title, String description, String unitId) {
        m_priority = priority;
        m_id = id;
        m_title = title;
        m_description = description;
        m_unitId = unitId;
        m_subtaskIds = new HashSet<>();
        m_completed = false;
    }

    public int getPriority() {
        return m_priority;
    }
    public void setPriority(int priority) {
        m_priority = priority;
    }

    public boolean isCompleted() {
        return m_completed;
    }
    public void setCompleted(boolean completed) {
        this.m_completed = completed;
    }

    public String getTitle() {
        return m_title;
    }
    public void setTitle(String title) {
        m_title = title;
    }

    public String getDescription() {
        return m_description;
    }
    public void setDescription(String description) {
        m_description = description;
    }

    public String getUnitId() {
        return m_unitId;
    }

    public void setUnitId(String unitId) {
        m_unitId = unitId;
    }

    public Unit getUnit() throws ExecutionException, InterruptedException, NoSuchDocumentException {
        return UnitDB.getInstance().awaitUnit(m_unitId);
    }
    public void setUnit(Unit unit) {
        m_unitId = unit.getId();
    }

    public String getId() {
        return m_id;
    }

    public List<AppTask> getChildren() throws ExecutionException, InterruptedException, NoSuchDocumentException {
        List<AppTask> res = new ArrayList<>();

        for (String id : getChildrenIds()) {
            res.add(TaskDB.getInstance().awaitTask(id));
        }

        return res;
    }

    public Set<String> getChildrenIds() {
        return m_subtaskIds;
    }

    public void addChild(AppTask subtask) {
        m_subtaskIds.add(subtask.getId());
    }

    public void addChildById(String taskId) {
        m_subtaskIds.add(taskId);
    }

    public void removeSubtask(AppTask subtask) {
        m_subtaskIds.remove(subtask.getId());
    }

    public int getCompletedCount() throws InterruptedException, ExecutionException, NoSuchDocumentException {
        return (int) getChildren().stream().filter(AppTask::isCompleted).count();
    }

    public int getChildCount() {
        return m_subtaskIds.size();
    }

    public boolean hasChildren() {
        return getChildCount() > 0;
    }

    public String priorityChar() {
        return TaskUtils.priorityChar(getPriority());
    }

    public String getIcon() {
        return isCompleted() ? COMPLETE_ICON : INCOMPLETE_ICON;
    }

    @Override
    @NonNull
    public String toString() {
        return "Goal " + getId() + " (" + getTitle() + ")";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Goal && ((Goal) o).m_id.equals(m_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_priority, m_completed, m_id, m_title, m_description, m_subtaskIds, m_unitId);
    }

    public int getPriorityColor(Context context) {
        int colorId;

        switch (getPriority()) {
            case 0:
                colorId = R.color.priority_a;
                break;
            case 1:
                colorId = R.color.priority_b;
                break;
            case 2:
                colorId = R.color.priority_c;
                break;
            case 3:
                colorId = R.color.priority_d;
                break;
            case 4:
                colorId = R.color.priority_e;
                break;
            default:
                throw new IllegalStateException("Illegal Priority Value: " + getPriority());
        }

        return context.getColor(colorId);
    }

    public Drawable getCompletedDrawable(Context context) {
        if (isCompleted()) {
            return ContextCompat.getDrawable(context, R.drawable.circle);
        } else {
            return ContextCompat.getDrawable(context, R.drawable.ring);
        }
    }

}

