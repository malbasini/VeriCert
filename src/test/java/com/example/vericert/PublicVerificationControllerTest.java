package com.example.vericert;

import com.example.vericert.controller.PublicVerificationController;
import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.VerificationToken;
import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.service.PlanEnforcementService;
import com.example.vericert.service.QrVerificationService;
import com.example.vericert.service.QrVerificationService.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test unitari per la verifica pubblica del certificato /v/{code}
 */
class PublicVerificationControllerTest {

    private VerificationTokenRepository verificationRepo;
    private CertificateRepository certificateRepo;
    private QrVerificationService qrVerificationService;
    private PlanEnforcementService planEnforcementService;

    private PublicVerificationController controller;

    @BeforeEach
    void setUp() {
        verificationRepo = mock(VerificationTokenRepository.class);
        certificateRepo = mock(CertificateRepository.class);
        qrVerificationService = mock(QrVerificationService.class);
        planEnforcementService = mock(PlanEnforcementService.class);

        controller = new PublicVerificationController(
                verificationRepo,
                certificateRepo,
                qrVerificationService,
                planEnforcementService
        );
    }

    @Test
    void verifyCertificate_returns404_whenTokenNotFound() throws Exception {
        // given
        String code = "NOT_EXIST";
        when(verificationRepo.findByCode(code)).thenReturn(Optional.empty());

        // when
        ResponseEntity<?> resp = controller.verifyCertificate(code);

        // then
        assertThat(resp.getStatusCodeValue()).isEqualTo(404);
        verifyNoInteractions(certificateRepo, qrVerificationService, planEnforcementService);
    }

    @Test
    void verifyCertificate_returns500_whenPlanEnforcementFails() throws Exception {
        String code = "ABC123";

        // token trovato
        VerificationToken token = new VerificationToken();
        token.setCode(code);
        token.setCertificateId(10L);
        when(verificationRepo.findByCode(code)).thenReturn(Optional.of(token));

        // certificato + tenant
        Tenant tenant = new Tenant();
        tenant.setId(1L);

        Certificate cert = new Certificate();
        cert.setId(10L);
        cert.setTenant(tenant);
        cert.setStatus(Stato.ISSUED);
        cert.setIssuedAt(Instant.now());
        when(certificateRepo.getById(10L)).thenReturn(cert);

        // checkCanCallApi che fallisce
        doThrow(new RuntimeException("Piano scaduto"))
                .when(planEnforcementService).checkCanCallApi(1L);

        // when
        ResponseEntity<?> resp = controller.verifyCertificate(code);

        // then
        assertThat(resp.getStatusCodeValue()).isEqualTo(500);
        assertThat(resp.getBody()).isEqualTo("Piano scaduto");

        // non deve chiamare il servizio di tracking verifica
        verifyNoInteractions(qrVerificationService);
    }

    @Test
    void verifyCertificate_returns410_whenCertificateRevoked() throws Exception {
        String code = "REV123";

        VerificationToken token = new VerificationToken();
        token.setCode(code);
        token.setCertificateId(20L);
        when(verificationRepo.findByCode(code)).thenReturn(Optional.of(token));

        Tenant tenant = new Tenant();
        tenant.setId(2L);

        Certificate cert = new Certificate();
        cert.setId(20L);
        cert.setTenant(tenant);
        cert.setStatus(Stato.REVOKED);
        cert.setIssuedAt(Instant.now());
        when(certificateRepo.getById(20L)).thenReturn(cert);

        // planEnforcement ok
        doNothing().when(planEnforcementService).checkCanCallApi(2L);

        // when
        ResponseEntity<?> resp = controller.verifyCertificate(code);

        // then
        assertThat(resp.getStatusCodeValue()).isEqualTo(410);
        assertThat(resp.getBody()).isEqualTo("❌ Certificato revocato");

        // non dovrebbe registrare una verifica riuscita
        verifyNoInteractions(qrVerificationService);
    }

    @Test
    void verifyCertificate_returns200AndDto_whenCertificateValid() throws Exception {
        String code = "OK123";

        VerificationToken token = new VerificationToken();
        token.setCode(code);
        token.setCertificateId(30L);
        when(verificationRepo.findByCode(code)).thenReturn(Optional.of(token));

        Tenant tenant = new Tenant();
        tenant.setId(3L);
        tenant.setName("ACME S.r.l.");

        Instant issuedAt = Instant.parse("2025-01-15T10:30:00Z");

        Certificate cert = new Certificate();
        cert.setId(30L);
        cert.setTenant(tenant);
        cert.setStatus(Stato.ISSUED);
        cert.setIssuedAt(issuedAt);
        cert.setOwnerName("Mario Rossi");
        cert.setOwnerEmail("mario.rossi@example.com");
        when(certificateRepo.getById(30L)).thenReturn(cert);

        doNothing().when(planEnforcementService).checkCanCallApi(3L);

        // when
        ResponseEntity<?> resp = controller.verifyCertificate(code);

        // then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).isInstanceOf(PublicVerificationController.VerificationResponse.class);

        var dto = (PublicVerificationController.VerificationResponse) resp.getBody();
        assertThat(dto.code()).isEqualTo(code);
        assertThat(dto.ownerName()).isEqualTo("Mario Rossi");
        assertThat(dto.ownerEmail()).isEqualTo("mario.rossi@example.com");

        // stessa formattazione usata nel controller
        String expectedDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(issuedAt);
        assertThat(dto.issueDate()).isEqualTo(expectedDate);

        // ha registrato la verifica sul servizio
        verify(qrVerificationService).verify(3L, code, Source.API, code);
    }
}
