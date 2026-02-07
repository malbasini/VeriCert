package com.example.vericert.service;

import com.example.vericert.util.CertDisplay;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class PdfSignatureValidationService {

    public record PdfSigCheck(boolean ok, String message, String signerCn, String signingTime) {}

    public PdfSigCheck validate(byte[] signedPdfBytes, X509Certificate expectedTenantCert) {
        try (PDDocument doc = PDDocument.load(signedPdfBytes)) {

            List<PDSignature> sigs = doc.getSignatureDictionaries();
            if (sigs.isEmpty()) return new PdfSigCheck(false, "Firma assente nel PDF.", null, null);

            PDSignature sig = sigs.get(sigs.size() - 1); // ultima firma

            byte[] contents = sig.getContents(signedPdfBytes);
            byte[] signedContent = sig.getSignedContent(signedPdfBytes);

            CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(signedContent), contents);
            SignerInformation signer = cms.getSignerInfos().getSigners().iterator().next();

            Store<X509CertificateHolder> certStore = cms.getCertificates();
            Collection<X509CertificateHolder> matches = certStore.getMatches(signer.getSID());
            if (matches.isEmpty()) return new PdfSigCheck(false, "Certificato firmatario non trovato nella firma.", null, null);

            X509CertificateHolder holder = matches.iterator().next();
            X509Certificate signingCert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);

            String cn = CertDisplay.commonName(signingCert);
            String signingTime = sig.getSignDate() != null ? sig.getSignDate().getTime().toString() : null;

            // vincolo tenant: deve essere esattamente il cert del tenant
            if (!Arrays.equals(signingCert.getEncoded(), expectedTenantCert.getEncoded())) {
                return new PdfSigCheck(false, "Firma non coerente con il tenant (certificato firmatario diverso).", null, null);
            }

            SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider("BC")
                    .build(holder);

            boolean ok = signer.verify(verifier);

            String Timesigning = null;
            if (sig.getSignDate() != null) {
                DateTimeFormatter fmt = DateTimeFormatter
                        .ofPattern("dd/MM/yyyy HH:mm")
                        .withZone(ZoneId.systemDefault());

                Timesigning = fmt.format(sig.getSignDate().toInstant());
            }



            return ok
                    ? new PdfSigCheck(true, "Firma valida: documento integro.",cn,Timesigning)
                    : new PdfSigCheck(false, "Firma NON valida: documento alterato o corrotto.",null,null);

        } catch (Exception e) {
            return new PdfSigCheck(false, "Errore verifica firma: " + e.getMessage(),null,null);
        }
    }
}