package com.example.vericert.repo;

import com.example.vericert.domain.SigningKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface SigningKeyRepository extends JpaRepository<SigningKeyEntity, String> {
    @Query("SELECT k FROM SigningKeyEntity k WHERE k.status IN ('ACTIVE','RETIRED')")
    List<SigningKeyEntity> findAllUsable();
}
