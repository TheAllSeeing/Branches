package com.evergreen.treetop.architecture.handlers;

import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.data.User;
import com.evergreen.treetop.architecture.utils.DBUnit;
import com.evergreen.treetop.architecture.utils.DBUnit.UnitDBKey;
import com.evergreen.treetop.architecture.utils.DBUser.UserDBKey;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class UnitDB {

    private UnitDB() {}
    private static final UnitDB m_instance = new UnitDB();
    public static UnitDB getInstance() {
        return m_instance;
    }

    private CollectionReference m_units = FirebaseFirestore.getInstance().collection("units");

    public CollectionReference getRef() {
        return m_units;
    }

    public DocumentReference newDoc() {
        return m_units.document();
    }

    public DocumentReference getUnitRef(String id) {
        return m_units.document(id);
    }

    private List<Unit> convert(Task<QuerySnapshot> query) throws ExecutionException, InterruptedException {
        return new ArrayList<>(Tasks.await(query)
                .getDocuments().stream().map(doc -> Unit.of(doc.toObject(DBUnit.class)))
                .collect(Collectors.toList()));
    }

    public List<Unit> getByIds(List<String> ids) throws ExecutionException, InterruptedException {
        return convert(m_units.whereIn(UnitDBKey.ID.getKey(), ids).get());
    }

    public List<Unit> getAll() throws ExecutionException, InterruptedException {
        return  convert(getRef().get());
    }

    public Task<Void> update(String unitId, UnitDBKey field, Object value) {
        return getUnitRef(unitId).update(field.getKey(), value);
    }

    public Task<Void> update(String unitId, UnitDBKey field, FieldValue value) {
        return getUnitRef(unitId).update(field.getKey(), value);

    }

    public List<Unit> getUserUnits(User user) throws ExecutionException, InterruptedException {
        return getByIds(user.getUnitIds());
    }

    public List<Unit> getRootUnits() throws ExecutionException, InterruptedException {
        return convert(getRef().whereEqualTo(UnitDBKey.IS_ROOT.getKey(), true).get());
    }

    public List<Unit> getUserUnits() throws InterruptedException, ExecutionException, NoSuchDocumentException {
        return getUserUnits(UserDB.getInstance().getCurrentUser());
    }

    public Unit awaitUnit(String id) throws ExecutionException, InterruptedException, NoSuchDocumentException {
        DBUnit res = Tasks.await(m_units.document(id).get()).toObject(DBUnit.class);

        if (res == null) {
            throw new NoSuchDocumentException("Tried to retrieve unit by id " + id
                    + ", but no such unit exists!");
        }

        return Unit.of(res);
    }

    public void create(Unit unit) throws ExecutionException, InterruptedException, NoSuchDocumentException {
        Tasks.await(getUnitRef(unit.getId()).set(DBUnit.of(unit)));

        if (!unit.isRootUnit()) {
            Tasks.await(update(unit.getParentId(), UnitDBKey.CHILDREN_IDS, FieldValue.arrayUnion(unit.getId())));
        }

        UserDB.getInstance().joinUnit(unit, true);
    }

    public void update(Unit unit) throws ExecutionException, InterruptedException {
        Tasks.await(getUnitRef(unit.getId()).set(DBUnit.of(unit)));
    }

    public void delete(Unit unit) throws ExecutionException, InterruptedException {

        for (Unit child : unit.getChildren()) {
            delete(child);
        }

        for (String memberId : unit.getMemberIds()) {
            UserDB.getInstance().leaveUnit(unit, memberId);
        }


        UserDB.getInstance().update(unit.getLeaderId(), UserDBKey.LEADING_IDS, FieldValue.arrayRemove(unit.getId()));
        Tasks.await(update(unit.getParentId(), UnitDBKey.CHILDREN_IDS, FieldValue.arrayRemove(unit.getId())));
        Tasks.await(getUnitRef(unit.getId()).delete());

    }


}
