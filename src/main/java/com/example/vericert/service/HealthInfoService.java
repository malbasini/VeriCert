package com.example.vericert.service;

import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class HealthInfoService {

    private final HealthEndpoint healthEndpoint;

    public HealthInfoService(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    public Map<String,Object> currentHealthInfo() {
        HealthComponent root = healthEndpoint.health(); // <-- niente cast diretto
        Map<String,Object> out = new HashMap<>();

        // stato complessivo (UP / DOWN / etc.)
        out.put("status", root.getStatus().getCode());

        // dettagli ricorsivi
        Map<String,Object> components = new HashMap<>();
        fillComponents(components, root);
        out.put("components", components);

        return out;
    }

    /**
     * Riempie "components" con roba tipo:
     * db -> { status: "UP", details:{...} }
     * diskSpace -> { status: "UP", details:{free:...,threshold:...} }
     */
    private void fillComponents(Map<String,Object> target, HealthComponent hc) {

        if (hc instanceof CompositeHealth composite) {
            // CompositeHealth ha più figli (es. "db","diskSpace",...)
            composite.getComponents().forEach((name, childComp) -> {
                Map<String,Object> childMap = new HashMap<>();
                childMap.put("status", childComp.getStatus().getCode());

                // se foglia vera, aggiungi "details"
                if (childComp instanceof Health leaf) {
                    childMap.put("details", leaf.getDetails());
                }

                // se a sua volta composto, ricorsione (per sicurezza)
                if (childComp instanceof CompositeHealth) {
                    Map<String,Object> nested = new HashMap<>();
                    fillComponents(nested, childComp);
                    childMap.put("components", nested);
                }

                target.put(name, childMap);
            });

        } else if (hc instanceof Health leaf) {
            // Singolo health (raro come root ma può succedere)
            Map<String,Object> self = new HashMap<>();
            self.put("status", leaf.getStatus().getCode());
            self.put("details", leaf.getDetails());
            target.put("self", self);
        }
    }
}
