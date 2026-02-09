package com.example.vericert.util;


import com.example.vericert.config.VericertProps;
import com.example.vericert.domain.Tenant;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Locale;


public class PdfUtil {
    @Value("${vericert.base-url}") String baseUrl;
    @Value("${vericert.storage.local-path}") String storagePath;
    @Value("${vericert.public-base-url:/files/certificates}")
    static String publicBaseUrl;
    @Value("${vericert.public-base-url-verify}") String publicBaseUrlVerify;
    private static final VericertProps props;

    public PdfUtil(VericertProps props){
        this.props = props;
    }
    public static byte[] htmlToPdf(String fullHtml) throws Exception {
            String xhtml = toXhtml(fullHtml);
            try (var out = new java.io.ByteArrayOutputStream()) {
                var b = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
                b.withHtmlContent(xhtml, null); // ora è XHTML valido
                b.toStream(out);
                b.run();
                return out.toByteArray();
            }
        }

    public static String toXhtml(String html) {
        Document doc = Jsoup.parse(html);
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                .prettyPrint(false);
        return doc.outerHtml();
    }

    public static String savePdf(String serial, byte[] pdf, Tenant tenant) {
        try {
            Path baseDir = Paths.get(props.getStorageLocalPath(), tenant.getId().toString());
            Files.createDirectories(baseDir);
            if (!Files.isWritable(baseDir)) {
                throw new IOException("Storage directory is not writable: " + baseDir.toAbsolutePath());
            }
            Path p = baseDir.resolve(serial + ".pdf");
            Files.write(p, pdf);
            // Costruisci URL pubblico coerente
            String publicUrl = publicBaseUrl.endsWith("/")
                    ? publicBaseUrl + serial + ".pdf"
                    : publicBaseUrl + "/" + serial + ".pdf";
            return publicUrl;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }





    public static String formatCents(long cents) {
        BigDecimal eur = BigDecimal.valueOf(cents).movePointLeft(2); // divide per 100 senza perdita
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.ITALY);
        return fmt.format(eur); // es. 2990 -> "€ 29,90"
    }

    public final class MoneyFmt {
        private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.ITALY);
        public static String euroFromMinor(long minor) {
            return EUR.format(minor / 100.0);
        }
    }

}