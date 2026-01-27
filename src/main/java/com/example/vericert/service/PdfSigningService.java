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
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Service
public class PdfSigningService {

    static {
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] signPdf(byte[] inputPdf, byte[] p12Blob, String p12Password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(p12Blob), p12Password.toCharArray());
        String alias = ks.aliases().nextElement();

        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, p12Password.toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);

        try (PDDocument doc = PDDocument.load(inputPdf);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDSignature sig = new PDSignature();
            sig.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            sig.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            sig.setName("Vercert Tenant Signature");
            sig.setReason("Certificate issuance");
            sig.setLocation("IT");
            sig.setSignDate(Calendar.getInstance());

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

                    List<X509Certificate> certs = new ArrayList<>();
                    for (Certificate c : chain) certs.add((X509Certificate) c);
                    gen.addCertificates(new JcaCertStore(certs));

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
}
