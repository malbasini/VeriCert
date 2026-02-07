package com.example.vericert.repo;

import com.example.vericert.domain.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
    List<InvoiceLine> findByInvoiceIdOrderBySortOrderAsc(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}