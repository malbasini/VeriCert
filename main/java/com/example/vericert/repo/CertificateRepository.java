package com.example.vericert.repo;
import com.example.vericert.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findBySerial(String serial);

    Certificate getById(Long id);
}