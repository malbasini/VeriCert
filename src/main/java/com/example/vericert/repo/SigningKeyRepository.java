package com.example.vericert.repo;

import com.example.vericert.domain.SigningKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SigningKeyRepository extends JpaRepository<SigningKey, String> {
    @Query("SELECT k FROM SigningKey k WHERE k.status IN ('ACTIVE','RETIRED')")
    List<SigningKey> findAllUsable();
}
