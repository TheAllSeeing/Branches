package com.evergreen.treetop.architecture.utils;

import com.evergreen.treetop.architecture.data.Goal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DBGoal implements Serializable {
    private int m_priority;
    private boolean m_completed;
    private String m_id;
    private String m_title;
    private String m_description;
    private String m_unitId;
    private List<String> m_subtaskIds;

    public DBGoal() {}

    public DBGoal(int priority, String id, String title, String description, String unitId) {
        this(priority, id, title, description, unitId, false, new ArrayList<>());
    }

    public DBGoal(int priority, String id, String title, String description, String unitId, boolean completed) {
        this(priority, id, title, description, unitId, completed, new ArrayList<>());
    }

    public DBGoal(int priority, String id, String title, String description, String unitId, boolean completed, List<String> subtaskIds) {
        m_priority = priority;
        m_id = id;
        m_title = title;
        m_description = description;
        m_unitId = unitId;
        m_completed = completed;
        m_subtaskIds = subtaskIds;
    }

    public static DBGoal of(Goal goal) {
        DBGoal result = new DBGoal(
              goal.getPriority(),
              goal.getId(),
              goal.getTitle(),
              goal.getDescription(),
              goal.getUnitId()
        );

        goal.getChildrenIds().forEach(result::addSubtaskId);
        return result;
    }

    public int getPriority() {
        return m_priority;
    }

    public void setPriority(int priority) {
        m_priority = priority;
    }

    public String getId() {
        return m_id;
    }

    public void setId(String id) {
        m_id = id;
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

    public boolean isCompleted() {
        return m_completed;
    }

    public void setCompleted(boolean completed) {
        m_completed = completed;
    }

    public void setUnitId(String unitId) {
        m_unitId = unitId;
    }

    public List<String> getSubtaskIds() {
        return m_subtaskIds;
    }

    public void setSubtaskIds(List<String> subtaskIds) {
        m_subtaskIds = subtaskIds;
    }

    public void addSubtaskId(String subtaskId) {
        m_subtaskIds.add(subtaskId);
    }

    public enum GoalDBKey {
        PRIORITY("priority"),
        COMPLETED("completed"),
        ID("id"),
        TITLE("title"),
        DESCRIPTION("description"),
        UNIT_ID("unitId"),
        SUBTASK_IDS("subtaskIds");

        private String m_key;

        GoalDBKey(String key) {
            m_key = key;
        }

        public String getKey() {
            return m_key;
        }
    }

}
