package com.example.vericert.repo;
import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findBySerial(String serial);

    Certificate getById(Long id);
    Page<Certificate> findByTenantId(Long tenantId, Pageable pageable);
}