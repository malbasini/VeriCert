package com.example.vericert.controller;

import com.example.vericert.domain.Template;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.service.TemplateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateRepository repo;
    private final TemplateService templateService;
    private final ObjectMapper mapper = new ObjectMapper();

    public TemplateController(TemplateRepository repo, TemplateService templateService) {
        this.repo = repo;
        this.templateService = templateService;
    }

    @GetMapping("/{id}/variables")
    public ResponseEntity<?> getVariables(@PathVariable Long id) throws Exception {
        Template tpl = repo.findById(id).orElseThrow();
        List<String> required = readRequiredVars(tpl.getVariablesJson());
        return ResponseEntity.ok(Map.of("required", required));
    }

    @PostMapping("/{id}/check-variables")
    public ResponseEntity<?> checkVariables(@PathVariable Long id, @RequestBody Map<String,Object> vars) throws Exception {
        Template tpl = repo.findById(id).orElseThrow();
        List<String> required = readRequiredVars(tpl.getVariablesJson());

        List<String> missing = new ArrayList<>();
        for (String key : required) {
            Object v = vars.get(key);
            if (v == null || (v instanceof String s && s.isBlank())) {
                missing.add(key);
            }
        }
        return missing.isEmpty()
                ? ResponseEntity.ok(Map.of("ok", true))
                : ResponseEntity.badRequest().body(Map.of("ok", false, "missing", missing));
    }

    private List<String> readRequiredVars(String variablesJson) throws Exception {
        if (variablesJson == null || variablesJson.isBlank()) return List.of();
        return mapper.readValue(variablesJson, new TypeReference<List<String>>() {});
    }
}