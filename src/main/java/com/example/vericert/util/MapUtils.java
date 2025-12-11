package com.example.vericert.util;

import java.util.Map;

public class MapUtils {

    public static Map<String, Object> toMap(Object record) {
        try {
            var c = record.getClass();
            if (!c.isRecord()) throw new IllegalArgumentException("Not a record");
            var map = new java.util.HashMap<String,Object>();
            for (var comp : c.getRecordComponents()) {
                var accessor = comp.getAccessor();
                map.put(comp.getName(), accessor.invoke(record));
            }
            return map;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
