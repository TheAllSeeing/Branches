package com.evergreen.treetop.architecture.utils;

import com.evergreen.treetop.architecture.data.AppTask;

import java.util.ArrayList;
import java.util.List;

public class DBTask extends DBGoal {

    private String m_parentId;
    private String m_assignerId;
    private String m_rootTaskId;
    private String m_goalId;
    private long m_startDeadlineEpoch;
    private long m_endDeadlineEpoch;
    private List<String> m_assigneeIds;


    public DBTask() {}

    public DBTask(int priority, String id, String title, String description, String unitId,
                  String parentId, long startDeadlineEpoch, long endDeadlineEpoch, String assignerId,
                  String rootTaskId, String goalId, boolean completed) {
        this(priority, id, title, description, unitId, parentId, startDeadlineEpoch, endDeadlineEpoch,
                assignerId, rootTaskId, goalId, completed, new ArrayList<>(), new ArrayList<>());
    }


    public DBTask(int priority, String id, String title, String description, String unitId,
                  String parentId, long startDeadlineEpoch, long endDeadlineEpoch, String assignerId,
                  String rootTaskId, String goalId, boolean completed, List<String> subtaskIds) {
        this(priority, id, title, description, unitId, parentId, startDeadlineEpoch, endDeadlineEpoch,
                assignerId, rootTaskId, goalId, completed, new ArrayList<>(), subtaskIds);
    }

    public DBTask(int priority, String id, String title, String description, String unitId, String parentId,
                  long startDeadlineEpoch, long endDeadlineEpoch, String assignerId, String rootTaskId,
                  String goalId, boolean completed, List<String> subtaskIds, List<String> assigneeIds) {
        super(priority, id, title, description, unitId, completed, subtaskIds);
        m_parentId = parentId;
        m_startDeadlineEpoch = startDeadlineEpoch;
        m_endDeadlineEpoch = endDeadlineEpoch;
        m_rootTaskId = rootTaskId;
        m_goalId = goalId;
        m_assignerId = assignerId;
        m_assigneeIds = assigneeIds;
    }

    public static DBTask of(AppTask task) {
        return  new DBTask(
                task.getPriority(),
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getUnitId(),
                task.getParentId(),
                task.getStartDeadline().toEpochDay(),
                task.getEndDeadline().toEpochDay(),
                task.getAssignerId(),
                task.getRootTaskId(),
                task.getGoalId(),
                task.isCompleted(),
                new ArrayList<>(task.getChildrenIds()),
                new ArrayList<>(task.getAssigneesIds())
        );
    }


    public long getStartDeadlineEpoch() {
        return m_startDeadlineEpoch;
    }

    public void setStartDeadlineEpoch(long startDeadlineEpoch) {
        m_startDeadlineEpoch = startDeadlineEpoch;
    }

    public long getEndDeadlineEpoch() {
        return m_endDeadlineEpoch;
    }

    public void setEndDeadlineEpoch(long endDeadlineEpoch) {
        m_endDeadlineEpoch = endDeadlineEpoch;
    }

    public String getAssignerId() {
        return m_assignerId;
    }

    public void setAssignerId(String assignerId) {
        m_assignerId = assignerId;
    }

    public List<String> getAssigneeIds() {
        return m_assigneeIds;
    }

    public void setAssigneeIds(List<String> assigneeIds) {
       m_assigneeIds = assigneeIds;
    }

    public String getParentId() {
        return m_parentId;
    }

    public void setParentId(String parentId) {
        m_parentId = parentId;
    }

    public String getRootTaskId() {
        return m_rootTaskId;
    }

    public void setRootTaskId(String rootTaskId) {
        m_rootTaskId = rootTaskId;
    }

    public String getGoalId() {
        return m_goalId;
    }

    public void setGoalId(String goalId) {
        m_goalId = goalId;
    }

    public boolean isRootTask() {
        return m_rootTaskId.equals(getId());
    }

    public enum TaskDBKey {
        PRIORITY(GoalDBKey.PRIORITY.getKey()),
        COMPLETED(GoalDBKey.COMPLETED.getKey()),
        ID(GoalDBKey.ID.getKey()),
        TITLE(GoalDBKey.TITLE.getKey()),
        DESCRIPTION(GoalDBKey.DESCRIPTION.getKey()),
        UNIT_ID(GoalDBKey.UNIT_ID.getKey()),
        SUBTASK_IDS(GoalDBKey.SUBTASK_IDS.getKey()),
        PARENT_ID("parentId"),
        ASSIGNER_ID("assignerId"),
        ROOT_ID("rootTaskId"),
        IS_ROOT("rootTask"),
        GOAL_ID("goalId"),
        START_DEADLINE("startDeadlineEpoch"),
        END_DEADLINE("endDeadlineEpoch"),
        ASSIGNEE_IDS("assigneeIds");


        private final String m_key;

        TaskDBKey(String key) {
            m_key = key;
        }

        public String getKey() {
            return m_key;
        }
    }

    @Override
    public String toString() {
        return "DBTask " + getId() + " (" + getTitle() + ")";
    }
}
