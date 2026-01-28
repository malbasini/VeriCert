package com.example.vericert.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;

@Service
public class PdfSigningService {

    @Value("${vericert.tsa.url:}")
    private String tsaUrl;

    static {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] signPdf(byte[] inputPdf, byte[] p12Blob, String p12Password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(p12Blob), p12Password.toCharArray());
        String alias = ks.aliases().nextElement();

        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, p12Password.toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);

        // Verifica che la catena contenga almeno il certificato dell'end-entity
        if (chain == null || chain.length == 0) {
            throw new IllegalStateException("No certificate chain found in P12 file");
        }

        // Log della catena per debugging
        System.out.println("=== PDF Signing Certificate Chain ===");
        System.out.println("Certificate chain length: " + chain.length);
        for (int i = 0; i < chain.length; i++) {
            X509Certificate cert = (X509Certificate) chain[i];
            String subject = cert.getSubjectDN().toString();
            String issuer = cert.getIssuerDN().toString();
            System.out.println("Certificate " + i + ":");
            System.out.println("  Subject: " + subject);
            System.out.println("  Issuer:  " + issuer);
            System.out.println("  Valid:   " + cert.getNotBefore() + " to " + cert.getNotAfter());

            // Verifica se è self-signed
            if (subject.equals(issuer)) {
                System.out.println("  ⚠️  SELF-SIGNED CERTIFICATE - Users must manually trust this in Adobe");
            }
        }
        System.out.println("=====================================");

        try (PDDocument doc = PDDocument.load(inputPdf);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDSignature sig = new PDSignature();
            sig.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            sig.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);

            // Usa il CN del certificato come nome del firmatario
            X509Certificate signerCert = (X509Certificate) chain[0];
            String signerName = signerCert.getSubjectX500Principal().getName();
            sig.setName(signerName);

            sig.setReason("Certificate issuance");
            sig.setLocation("IT");
            sig.setSignDate(Calendar.getInstance());

            // Aggiungi informazioni di contatto se disponibili
            // sig.setContactInfo("info@vericert.org");

            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);

            doc.addSignature(sig, (SignatureInterface) content -> {
                try {
                    byte[] contentBytes = content.readAllBytes();
                    CMSProcessableInputStream msg;
                    msg = new CMSProcessableInputStream(contentBytes);
                    CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

                    ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                            .setProvider("BC")
                            .build(privateKey);

                    X509Certificate signingCert = (X509Certificate) chain[0];

                    gen.addSignerInfoGenerator(
                            new JcaSignerInfoGeneratorBuilder(
                                    new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
                            ).build(signer, signingCert)
                    );

                    // Aggiungi TUTTA la catena di certificati (inclusi intermedi e root)
                    // Questo è ESSENZIALE per la validazione in Adobe
                    List<X509Certificate> certs = new ArrayList<>();
                    for (Certificate c : chain) {
                        certs.add((X509Certificate) c);
                    }
                    gen.addCertificates(new JcaCertStore(certs));

                    System.out.println("Added " + certs.size() + " certificates to CMS signature");

                    CMSSignedData sd = gen.generate(msg, false);
                    return sd.getEncoded();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, options);

            doc.saveIncremental(out);
            return out.toByteArray();
        }
    }

    /**
     * Estrae il certificato pubblico dal P12 in formato PEM.
     * Questo certificato deve essere distribuito agli utenti per l'installazione in Adobe.
     */
    public String extractPublicCertificatePem(byte[] p12Blob, String p12Password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(p12Blob), p12Password.toCharArray());
        String alias = ks.aliases().nextElement();

        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN CERTIFICATE-----\n");
        pem.append(Base64.getEncoder().encodeToString(cert.getEncoded()).replaceAll("(.{64})", "$1\n"));
        pem.append("\n-----END CERTIFICATE-----\n");

        return pem.toString();
    }

    /**
     * Ottiene un timestamp da una Time Stamp Authority (TSA).
     * Questo migliora significativamente la validazione della firma in Adobe.
     */
    private byte[] getTSAResponse(byte[] signatureBytes) throws IOException, TSPException {
        if (tsaUrl == null || tsaUrl.trim().isEmpty()) {
            System.out.println("No TSA URL configured - skipping timestamp");
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signatureBytes);

            TimeStampRequestGenerator tsqGenerator = new TimeStampRequestGenerator();
            tsqGenerator.setCertReq(true);
            BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis());
            TimeStampRequest request = tsqGenerator.generate(org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_sha256, hash, nonce);

            byte[] requestBytes = request.getEncoded();

            URL url = new URL(tsaUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/timestamp-query");
            conn.setRequestProperty("Content-Length", String.valueOf(requestBytes.length));

            try (OutputStream out = conn.getOutputStream()) {
                out.write(requestBytes);
            }

            if (conn.getResponseCode() != 200) {
                System.err.println("TSA returned error: " + conn.getResponseCode());
                return null;
            }

            TimeStampResponse response = new TimeStampResponse(conn.getInputStream());
            response.validate(request);

            TimeStampToken token = response.getTimeStampToken();
            if (token != null) {
                System.out.println("TSA timestamp obtained successfully");
                return token.getEncoded();
            }
        } catch (Exception e) {
            System.err.println("Failed to get TSA timestamp: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
