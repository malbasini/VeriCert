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