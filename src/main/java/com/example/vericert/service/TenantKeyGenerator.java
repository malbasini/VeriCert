package com.example.vericert.service;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;

public class TenantKeyGenerator {

    static {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
    }

    public record Material(String publicKeyPem, String certPem, byte[] p12, Instant notBefore, Instant notAfter) {}

    public static Material generate(String tenantSlug, char[] p12Password, int validityDays) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(3072, new SecureRandom());
        KeyPair kp = kpg.generateKeyPair();

        Instant nb = Instant.now().minusSeconds(60);
        Instant na = Instant.now().plus(validityDays, java.time.temporal.ChronoUnit.DAYS);

        X500Name dn = new X500Name("C=IT,O=Vercert,OU=Tenant,CN=" + tenantSlug);
        BigInteger serial = new BigInteger(160, new SecureRandom()).abs();

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(kp.getPrivate());

        JcaX509v3CertificateBuilder b = new JcaX509v3CertificateBuilder(
                dn, serial, Date.from(nb), Date.from(na), dn, kp.getPublic()
        );

        // Solo digitalSignature per PDF signing
        b.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));

        X509CertificateHolder holder = b.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
        cert.verify(kp.getPublic());

        // PKCS12
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        String alias = "vercert-tenant-" + tenantSlug;
        ks.setKeyEntry(alias, kp.getPrivate(), p12Password, new Certificate[]{cert});

        ByteArrayOutputStream p12Out = new ByteArrayOutputStream();
        ks.store(p12Out, p12Password);

        return new Material(toPem(kp.getPublic()), toPem(cert), p12Out.toByteArray(), nb, na);
    }

    private static String toPem(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JcaPEMWriter w = new JcaPEMWriter(new OutputStreamWriter(baos))) {
            w.writeObject(obj);
        }
        return baos.toString();
    }
}
