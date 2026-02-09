package com.example.vericert.util;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.StringReader;
import java.security.cert.X509Certificate;

public class CertificatePemUtils {
    public static X509Certificate parseX509(String certPem) throws Exception {
        try (PemReader r = new PemReader(new StringReader(certPem))) {
            byte[] der = r.readPemObject().getContent();
            X509CertificateHolder holder = new X509CertificateHolder(der);
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
        }
    }
}
