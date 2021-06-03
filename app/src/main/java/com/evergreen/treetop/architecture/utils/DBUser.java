package com.evergreen.treetop.architecture.utils;

import com.evergreen.treetop.architecture.data.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DBUser implements Serializable {

    String m_id;
    String m_name;
    List<String> m_unitIds;
    List<String> m_leadingIds;

    public DBUser() {}

    public DBUser(String id, String name) {
        this(id, name, new ArrayList<>(), new ArrayList<>());
    }

    public DBUser(String id, String name, List<String> unitIds, List<String> leadingIds) {
        m_id = id;
        m_name = name;
        m_unitIds = unitIds;
        m_leadingIds = leadingIds;
    }

    public static DBUser of(User user) {
        return new DBUser(
                user.getId(),
                user.getName(),
                user.getUnitIds(),
                user.getUnitIds()
        );
    }

    public String getId() {
        return m_id;
    }

    public void setId(String id) {
        m_id = id;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public List<String> getUnitIds() {
        return m_unitIds;
    }

    public void setUnitIds(List<String> unitIds) {
        m_unitIds = unitIds;
    }

    public List<String> getLeadingIds() {
        return m_leadingIds;
    }

    public void setLeadingIds(List<String> leadingIds) {
        m_leadingIds = leadingIds;
    }

    public enum UserDBKey {
        ID("id"),
        NAME("name"),
        UNIT_IDS("unitIds"),
        LEADING_IDS("leadingIds");


        String m_key;

        UserDBKey(String key) {
            m_key = key;
        }

        public String getKey() {
            return m_key;
        }
    }
}
