package com.example.vericert.service;

import com.example.vericert.dto.VarSpec;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ValidationService {
    public void validateUserVars(Map<String,Object> userVars, Map<String, VarSpec> schema) {
        // required
        for (var e : schema.entrySet()) {
            String key = e.getKey();
            VarSpec spec = e.getValue();
            if (spec.required() && (userVars == null || !userVars.containsKey(key)))
                throw new IllegalArgumentException("Campo obbligatorio mancante: " + key);
        }
        if (userVars == null) return;

        for (var e : userVars.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            VarSpec spec = schema.get(key);
            if (spec == null) continue; // extra: consentiti
            if (val == null) continue;

            switch (spec.type()) {
                case "string" -> { /* ok, qualsiasi toString */ }
                case "number" -> {
                    if (!(val instanceof Number) && !val.toString().matches("-?\\d+(\\.\\d+)?"))
                        throw new IllegalArgumentException("Campo " + key + " deve essere numerico");
                }
                case "bool","boolean" -> {
                    String s = val.toString().toLowerCase();
                    if (!s.equals("true") && !s.equals("false"))
                        throw new IllegalArgumentException("Campo " + key + " deve essere boolean");
                }
                case "date" -> {
                    // YYYY-MM-DD semplice
                    if (!val.toString().matches("\\d{4}-\\d{2}-\\d{2}"))
                        throw new IllegalArgumentException("Campo " + key + " deve essere una data YYYY-MM-DD");
                }
                default -> { /* ignora tipi custom */ }
            }
        }
    }
}
