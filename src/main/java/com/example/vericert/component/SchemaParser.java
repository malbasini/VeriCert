package com.example.vericert.component;

import com.example.vericert.dto.VarSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SchemaParser {
    private final ObjectMapper om = new ObjectMapper();

    public Map<String, VarSpec> parse(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            // {"field":{"type":"string","required":true,"label":"..."}}
            JsonNode root = om.readTree(json);
            Map<String, VarSpec> out = new LinkedHashMap<>();
            root.fieldNames().forEachRemaining(name -> {
                JsonNode n = root.get(name);
                out.put(name, new VarSpec(
                        n.path("type").asText("string"),
                        n.path("required").asBoolean(false),
                        n.path("label").asText(name)
                ));
            });
            return out;
        } catch (Exception e) {
            throw new IllegalArgumentException("Schema non valido", e);
        }
    }
}