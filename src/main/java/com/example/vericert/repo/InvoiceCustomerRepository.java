package com.example.vericert.repo;

import com.example.vericert.domain.InvoiceCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceCustomerRepository extends JpaRepository<InvoiceCustomer, Long> {}
