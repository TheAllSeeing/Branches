package com.evergreen.treetop.architecture.data;

import androidx.annotation.NonNull;

import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.handlers.UnitDB;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.DBUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Unit {

    private final String m_id;
    private String m_name;
    private String m_description;
    private final String m_parentId;
    private final String m_leaderId;
    private final List<String> m_childrenIds;
    private final List<String> m_memberIds;

    public static final Unit ROOT = new Unit(
            "root",
            "Evergreen #7112",
            "",
            ""

    );

    public Unit(String id, String name, String description, String leaderId) {
        this(id, name, description, leaderId, null);
    }

    public Unit(String id, String name, String description, String leaderId, String parentId) {
        m_description = description;
        m_name = name;
        m_id = id;
        m_leaderId = leaderId;
        m_parentId = parentId;
        m_childrenIds = new ArrayList<>();
        m_memberIds = new ArrayList<>();

        m_memberIds.add(leaderId);
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public String getDescription() {
        return m_description;
    }

    public void setDescription(String description) {
        m_description = description;
    }

    public String getId() {
        return m_id;
    }

    public String getLeaderId() {
        return m_leaderId;
    }

    public User getLeader() throws InterruptedException, ExecutionException, NoSuchDocumentException {
        return UserDB.getInstance().awaitUser(m_leaderId);
    }

    public String getParentId() {
        return m_parentId;
    }

    public Unit getParent() throws InterruptedException, ExecutionException, NoSuchDocumentException {
        return UnitDB.getInstance().awaitUnit(getParentId());
    }

    public List<String> getChildrenIds() {
        return m_childrenIds;
    }

    public List<Unit> getChildren() throws ExecutionException, InterruptedException {
        if (getChildrenIds().size() == 0) return new ArrayList<>();
        return UnitDB.getInstance().getByIds(getChildrenIds());
    }

    public boolean hasChildren() {
        return m_childrenIds.size() > 0;
    }

    public void addChild(String id) {
        m_childrenIds.add(id);
    }

    public void removeChild(String id) {
        m_childrenIds.remove(id);
    }

    public List<String> getMemberIds() {
        return m_memberIds;
    }

    public List<User> getMembers() throws InterruptedException, ExecutionException, NoSuchDocumentException {

        List<User> res = new ArrayList<>();

        for (String id : getMemberIds()) {
            res.add(UserDB.getInstance().awaitUser(id));
        }

        return res;
    }

    public void addMember(String userId) {
        if (!m_memberIds.contains(userId)) {
            m_memberIds.add(userId);
        }
    }

    public void removeMember(String userId) {
        m_memberIds.remove(userId);
    }


    public boolean isRootUnit() {
        return m_parentId == null;
    }

    public static Unit of(DBUnit dbUnit) {
        Unit res = new Unit(
                dbUnit.getId(),
                dbUnit.getName(),
                dbUnit.getDescription(),
                dbUnit.getLeaderId(),
                dbUnit.getParentId()
        );

        if (dbUnit.getChildrenIds() != null) {
            dbUnit.getChildrenIds().forEach(res::addChild);
        }

        dbUnit.getMemberIds().forEach(res::addMember);

        return res;
    }

    @Override
    @NonNull
    public String toString() {
        return "Unit " + getId() + " (" + getName() + ")";
    }
}
