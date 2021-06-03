package com.evergreen.treetop.architecture;

import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.data.User;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LoggingUtils {

    public static String stringify(Object obj) {
        return (obj == null? "null" : obj.toString());
    }

    public static String stringify(FirebaseUser user) {
        return user == null? "null" : "user " + user.getUid() + " (" + user.getDisplayName() + ")";
    }

    public static <T> String stringify(Collection<T> list, Function<T, Object> propertyMapper, boolean brackets) {
        return (brackets? "[" : "") + list.stream().map(propertyMapper).map(Object::toString).collect(Collectors.joining(", ")) + (brackets? "]" : "");
    }

    public static <T> String stringify(Collection<T> list, Function<T, Object> propertyMapper) {
        return stringify(list, propertyMapper, true);
    }

    public static <T> String stringify(List<T> list) {
        return  stringify(list, obj -> obj);
    }

    public static <T> String stringify(T[] arr) {
        return stringify(Arrays.asList(arr));
    }

    public static <K, V> String stringify(Map<K, V> map) {
        return "{" +
                map.entrySet().stream()
                        .map(entry -> entry.getKey().toString() + ": " + entry.getValue().toString())
                        .collect(Collectors.joining(","))
                + "}";
    }


    public static final PlaceholderObject PLACEHOLDER_OBJECT = new PlaceholderObject();
    private static class PlaceholderObject {
        public final boolean exists = true;
        private PlaceholderObject() {}
    }



    public static User dummyUser(String id) {
        return new User(id, id);
    }

    public static Unit dummyUnit(String id) {
        return new Unit(id, id, id, id);
    }
    public static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }



}
