package com.example.vericert.repo;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.MembershipId;
import com.example.vericert.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, MembershipId> {
    Membership findByUser(User user);
}
