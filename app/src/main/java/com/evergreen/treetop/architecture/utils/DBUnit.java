package com.evergreen.treetop.architecture.utils;

import com.evergreen.treetop.architecture.data.Unit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DBUnit implements Serializable {

    String m_id;
    String m_name;
    String m_description = "";
    String m_leaderId;
    String m_parentId = null;
    List<String> m_childrenIds = new ArrayList<>();
    List<String> m_memberIds = new ArrayList<>();

    public DBUnit() {}

    public DBUnit(String id, String name, String description, String leaderId, String parentId, List<String> childrenIds, List<String> memberIds) {
        m_name = name;
        m_description = description;
        m_id = id;
        m_leaderId = leaderId;
        m_parentId = parentId;
        m_childrenIds = childrenIds;
        m_memberIds = memberIds != null? memberIds : Collections.singletonList(leaderId);
    }

    public static DBUnit of(Unit unit) {
        return new DBUnit(
                unit.getId(),
                unit.getName(),
                unit.getDescription(),
                unit.getLeaderId(),
                unit.getParentId(),
                unit.getChildrenIds(),
                unit.getMemberIds()
        );
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public String getId() {
        return m_id;
    }

    public void setId(String id) {
        m_id = id;
    }

    public String getDescription() {
        return m_description;
    }

    public void setDescription(String description) {
        m_description = description;
    }

    public String getLeaderId() {
        return m_leaderId;
    }

    public void setLeaderId(String leaderId) {
        m_leaderId = leaderId;
    }

    public String getParentId() {
        return m_parentId;
    }

    public void setParentId(String parentId) {
        m_parentId = parentId;
    }

    public List<String> getChildrenIds() {
        return m_childrenIds;
    }

    public void setChildrenIds(List<String> childrenIds) {
        m_childrenIds = childrenIds;
    }

    public List<String> getMemberIds() {
        return m_memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        m_memberIds = memberIds;
    }

    public boolean isRootUnit() {
        return m_parentId == null;
    }

    public enum UnitDBKey {
        NAME("name"),
        ID("id"),
        PARENT_ID("parentId"),
        IS_ROOT("rootUnit"),
        CHILDREN_IDS("childrenIds"),
        MEMBER_IDS("memberIds");

        private final String m_key;

        UnitDBKey(String key) {
            m_key = key;
        }

        public String getKey() {
            return m_key;
        }
    }

    @Override
    public String toString() {
        return "DBUnit " + m_id + " (" + m_name + ")";
    }
}
