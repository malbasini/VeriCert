package com.example.vericert.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
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
    public static String formatEuroIT(BigDecimal value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.ITALY);
        nf.setCurrency(Currency.getInstance("EUR"));
        return nf.format(value);
    }
}
