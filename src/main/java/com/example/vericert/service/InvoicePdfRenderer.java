package com.example.vericert.service;

import com.example.vericert.domain.Invoice;

public interface InvoicePdfRenderer {
    byte[] renderPdf(Invoice invoice);
}
