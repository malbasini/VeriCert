package com.example.vericert;

import com.example.vericert.config.VericertProps;
import com.example.vericert.controller.PublicVerificationController;
import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.Template;
import com.example.vericert.domain.VerificationToken;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.service.*;
import com.example.vericert.util.PdfUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.example.vericert.util.PdfUtil.toXhtml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test "end-to-end leggero":
 * - chiama CertificateService.issue(...)
 * - poi verifica via PublicVerificationController.verifyCertificate(...)
 * sullo stesso mock di repository.
 */
class CertificateIssueAndVerifyPipelineTest {

    @Mock
    private PlanEnforcementService planEnforcementService;

    @Mock
    private TemplateRepository tempRepo;

    @Mock
    private TemplateService templateService;

    @Mock
    private CertificateRepository certRepo;

    @Mock
    private PdfUtil pdfUtil;


    @Mock
    private VerificationTokenRepository tokRepo;

    @Mock
    private TenantSettingsService tenantSettingsService;

    @Mock
    private UsageMeterService usageMeterService;

    @Mock
    private QrVerificationService qrVerificationService;

    @Mock
    private VericertProps props; // sostituisci con la tua classe di configurazione

    // useremo uno spy per poter eventualmente stubbare htmlToPdf/savePdf
    @InjectMocks
    private CertificateService certificateServiceReal;

    private CertificateService certificateService; // spy

    private PublicVerificationController publicVerificationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // spy del service reale, così se vuoi puoi fare:
        // doReturn(...).when(certificateService).htmlToPdf(anyString());
        certificateService = Mockito.spy(certificateServiceReal);

        // controller che usa gli stessi mock di certRepo / tokRepo e i servizi
        publicVerificationController = new PublicVerificationController(
                tokRepo,
                certRepo,
                qrVerificationService,
                planEnforcementService
        );
    }

    @Test
    void issueThenVerify_pipelineCompletaOk() throws Exception {

        try (MockedStatic<PdfUtil> pdfMock = mockStatic(PdfUtil.class)) {

            // se anche QR è statico:
            // try (MockedStatic<QrUtil> qrMock = mockStatic(QrUtil.class)) { ...


            Long templateId = 1L;

            Tenant tenant = new Tenant();
            tenant.setId(10L);
            tenant.setName("ACME S.r.l.");

            Map<String, Object> vars = new HashMap<>();
            vars.put("nome", "Mario Rossi");

            // ---- STUB lato emissione ----

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

            // se htmlToPdf / savePdf ti danno noia nei test, puoi scommentare:
            // doReturn("dummy.pdf".getBytes()).when(certificateService).htmlToPdf(anyString());
            // doReturn("/files/10/serial.pdf").when(certificateService).savePdf(anyString(), any(), any(Tenant.class));
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

            // ---- STUB lato verifica pubblica ----

            // il controller usa gli stessi repo, quindi li facciamo "rispondere"
            when(tokRepo.findByCode(savedToken.getCode()))
                    .thenReturn(Optional.of(savedToken));

            // il controller usa certificateRepo.getById
            when(certRepo.getById(savedCert.getId()))
                    .thenReturn(savedCert);

            // per la verifica pubblica, controllo API ok
            doNothing().when(planEnforcementService).checkCanCallApi(tenant.getId());

            // ---- VERIFICA ----
            ResponseEntity<?> resp = publicVerificationController.verifyCertificate(savedToken.getCode());

            assertThat(resp.getStatusCodeValue()).isEqualTo(200);
            assertThat(resp.getBody())
                    .isInstanceOf(PublicVerificationController.VerificationResponse.class);

            var dto = (PublicVerificationController.VerificationResponse) resp.getBody();

            assertThat(dto.code()).isEqualTo(savedToken.getCode());
            assertThat(dto.ownerName()).isEqualTo("Mario Rossi");
            assertThat(dto.ownerEmail()).isEqualTo("mario.rossi@example.com");

            // il service di tracking verifica deve essere stato chiamato
            verify(qrVerificationService).verify(
                    eq(tenant.getId()),
                    eq(savedToken.getCode()),
                    eq(QrVerificationService.Source.API),
                    eq(savedToken.getCode())
            );

        }
    }
}
