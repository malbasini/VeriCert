package com.example.vericert.controller;

import com.example.vericert.domain.Template;
import com.example.vericert.dto.TemplateDto;
import com.example.vericert.dto.TemplateUpsert;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.TemplateAdminService;
import com.example.vericert.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/templates")
@PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
public class TemplateRestAdminController {

    private final TemplateRepository repo;
    private final TemplateAdminService service;
    private final TemplateService renderService;

    public TemplateRestAdminController(TemplateRepository repo,
                                   TemplateAdminService service,
                                   TemplateService renderService) {
        this.repo = repo;
        this.service = service;
        this.renderService = renderService;
    }

    @GetMapping("/admin/templates")
    public String list(@AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam(required = false) String q,
                       @PageableDefault(size=10, sort="updatedAt", direction=Sort.Direction.DESC) Pageable pageable,
                       Model model) {

        Long tenantId = user.getTenantId(); // oppure risolto da TenantResolver

        Page<Template> page = (q == null || q.isBlank())
                ? repo.findAllByTenantId(tenantId, pageable)
                : repo.searchByName(tenantId, q, pageable);

        model.addAttribute("page", page);
        model.addAttribute("q", q);
        return "admin/templates/list";
    }


    @GetMapping("/{id}")
    public TemplateDto getOne(@PathVariable Long id) {
        Long tenantId = currentTenantId();
        var t = repo.findByTenantIdAndId(tenantId, id).orElseThrow();
        return TemplateMapper.toDto(t);
    }

    @PostMapping("/new")
    public ResponseEntity<?> create(@Valid @RequestBody TemplateUpsert req, BindingResult br) {
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            fe -> fe.getField(),
                            Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message","Validation failed","errors",errors));
        }
        var t = service.create(req);

        return ResponseEntity.ok(new TemplateRestAdminController.VerificationResponse(
                t.getId().toString(),
                t.getName(),
                t.getVersion(),
                t.isActive(),
                t.getTenant().getName()
        ));
    }
    // DTO interno alla risposta
    record VerificationResponse(
            String id,
            String name,
            String version,
            boolean active,
            String tenant
    ) {}

    @PutMapping("/{id}/edit")
    public ResponseEntity<?>  update(@PathVariable(name = "id") Long id,
                              @Valid @RequestBody TemplateUpsert req,
                             BindingResult br) {
        com.example.vericert.domain.Template t = null;
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message","Validation failed","errors",errors));

        }
        try {
           t  = service.update(id, req);
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(new TemplateRestAdminController.VerificationUpdateResponse(
                t.getId().toString(),
                t.getName(),
                t.getVersion(),
                t.isActive(),
                t.getTenant().getName()
        ));
    }
    // DTO interno alla risposta
    record VerificationUpdateResponse(
            String id,
            String name,
            String version,
            boolean active,
            String tenant
    ) {}

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        service.activate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/preview")
    public Map<String, String> preview(@PathVariable Long id, @RequestBody Map<String,Object> vars) {
        Map<String,Object> sys = Map.of(
                "serial","PREVIEW-XXXX",
                "verifyUrl","https://example/preview",
                "qrBase64",""
        );
        String html = renderService.renderHtml(id, vars, sys);
        return Map.of("html", html);
    }

    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
}
