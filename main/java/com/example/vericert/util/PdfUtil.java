package com.example.vericert.util;


import io.micrometer.core.instrument.util.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;


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

    private static String toXhtml(String html) {
        Document doc = Jsoup.parse(html);
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                .prettyPrint(false);
        return doc.outerHtml();
    }
}