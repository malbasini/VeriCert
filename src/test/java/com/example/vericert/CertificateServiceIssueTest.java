package com.example.vericert;

import com.example.vericert.config.VericertProps;
import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Template;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.VerificationToken;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.service.*;
import com.example.vericert.util.PdfUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per il metodo issue(...) di CertificateService.
 *
 * ATTENZIONE:
 * - Adatta i tipi @Mock ai tuoi veri component (TemplateRepository, TemplateService, props, ecc.).
 * - I metodi htmlToPdf/savePdf devono funzionare o puoi accettare che il test
 *   faccia anche un po' di "integration" (PDF, filesystem, chiavi EdDSA).
 */
class CertificateServiceIssueTest {

    @Mock
    private PlanEnforcementService planEnforcementService;

    @Mock
    private TemplateRepository tempRepo;

    @Mock
    private TemplateService templateService;

    @Mock
    private CertificateRepository certRepo;

    @Mock
    private VerificationTokenRepository tokRepo;

    @Mock
    private TenantSettingsService tenantSettingsService;

    @Mock
    private UsageMeterService usageMeterService;

    @Mock
    private VericertProps props; // <-- sostituisci con la tua classe reale (es. AppProps/PaymentsProps)

    @InjectMocks
    private CertificateService certificateService;

    @InjectMocks
    private CertificateService certificateServiceReal;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void issue_emetteCertificatoESalvaToken_incrementandoUsage() throws Exception {

        try (MockedStatic<PdfUtil> pdfMock = mockStatic(PdfUtil.class)) {
            Long templateId = 1L;

            Tenant tenant = new Tenant();
            tenant.setId(10L);
            tenant.setName("ACME S.r.l.");

            Map<String, Object> vars = new HashMap<>();
            vars.put("nome", "Mario Rossi");

            // controllo piano OK
            doNothing().when(planEnforcementService).checkCanIssueCertificate(tenant.getId());

            // template trovato
            Template template = new Template();
            template.setId(templateId);
            when(tempRepo.findById(templateId)).thenReturn(Optional.of(template));

            // sysVars base
            when(tenantSettingsService.buildBaseSysVarsForTenant(tenant.getId()))
                    .thenReturn(new HashMap<>());

            // configurazione base verifica
            when(props.getBaseUrlVerify()).thenReturn("https://vericert.test");
            when(props.getKid()).thenReturn("kid-test");

            // HTML di prova
            when(templateService.renderHtml(eq(templateId), anyMap(), anyMap()))
                    .thenReturn("<html><body>Certificato</body></html>");

            // WHEN
            certificateService = spy(certificateServiceReal);

            pdfMock.when(() -> PdfUtil.htmlToPdf(anyString()))
                    .thenReturn("DUMMY PDF".getBytes());
            doReturn("/files/" + tenant.getId() + "/dummy.pdf")
                    .when(certificateService).savePdf(anyString(), any(), any(Tenant.class));

            // salva certificato: gli assegniamo un id fittizio
            ArgumentCaptor<Certificate> certCaptor = ArgumentCaptor.forClass(Certificate.class);
            when(certRepo.save(any(Certificate.class))).thenAnswer(invocation -> {
                Certificate c = invocation.getArgument(0);
                if (c.getId() == null) {
                    c.setId(42L);
                }
                // simuliamo un issuedAt coerente col controller
                if (c.getIssuedAt() == null) {
                    c.setIssuedAt(Instant.now());
                }
                // THEN: certificato ritornato non null
                assertThat(c).isNotNull();
                assertThat(c.getTenant()).isEqualTo(tenant);
                assertThat(c.getOwnerName()).isEqualTo("Mario Rossi");
                assertThat(c.getOwnerEmail()).isEqualTo("mario.rossi@example.com");
                assertThat(c.getPdfUrl())
                        .startsWith("/files/" + tenant.getId() + "/")
                        .endsWith(".pdf");
                return c;

            });

            // salva token: catturiamo quello reale
            ArgumentCaptor<VerificationToken> tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
            when(tokRepo.save(any(VerificationToken.class))).thenAnswer(invocation -> {
                VerificationToken t = invocation.getArgument(0);
                if (t.getCreatedAt() == null) {
                    t.setCreatedAt(Instant.now());
                }
                if (t.getExpiresAt() == null) {
                    t.setExpiresAt(Instant.now().plusSeconds(31536000));
                }
                return t;
            });

            // ---- EMISSIONE ----
            Certificate emitted = certificateService.issue(
                    templateId,
                    vars,
                    "Mario Rossi",
                    "mario.rossi@example.com",
                    tenant
            );

            // recuperiamo ciò che è stato salvato nei repo mock
            verify(certRepo, atLeastOnce()).save(certCaptor.capture());
            Certificate savedCert = certCaptor.getValue();

            verify(tokRepo).save(tokenCaptor.capture());
            VerificationToken savedToken = tokenCaptor.getValue();

            assertThat(savedCert.getId()).isNotNull();
            assertThat(savedToken.getCertificateId()).isEqualTo(savedCert.getId());
            assertThat(savedToken.getCode()).isNotBlank();

            // deve aver incrementato l'usage
            verify(usageMeterService).incrementCertsGenerated(tenant.getId());

            // il controller usa gli stessi repo, quindi li facciamo "rispondere"
            when(tokRepo.findByCode(savedToken.getCode()))
                    .thenReturn(Optional.of(savedToken));

            // il controller usa certificateRepo.getById
            when(certRepo.getById(savedCert.getId()))
                    .thenReturn(savedCert);
            // Deve essere incrementato l'usage del tenant
            verify(usageMeterService).incrementCertsGenerated(tenant.getId());

            // Deve essere stato invocato il controllo piano
            verify(planEnforcementService).checkCanIssueCertificate(tenant.getId());
        }
    }
        @Test
        void issue_faPropagareEccezioneSePianoNonConsenteEmissione () throws Exception {
            Long templateId = 1L;
            Tenant tenant = new Tenant();
            tenant.setId(10L);

            Map<String, Object> vars = Map.of();

            // planEnforcement lancia eccezione (es. piano scaduto o quota esaurita)
            doThrow(new RuntimeException("Limite certificati superato"))
                    .when(planEnforcementService).checkCanIssueCertificate(tenant.getId());

            // WHEN + THEN
            org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class,
                    () -> certificateService.issue(
                            templateId,
                            vars,
                            "Mario Rossi",
                            "mario.rossi@example.com",
                            tenant
                    )
            );

            // Non deve salvare nulla
            verifyNoInteractions(tempRepo, templateService, certRepo, tokRepo, usageMeterService);
        }
    }
