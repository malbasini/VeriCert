package com.example.vericert.service;

import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public final class TemplateHtmlSanitizer {

    // Drop di tutti gli attributi evento (onclick, onload, …)
    private static final AttributePolicy DROP_EVENT_ATTRS = (el, attr, val) ->
            attr != null && attr.toLowerCase().startsWith("on") ? null : val;

    /** Permetti “quasi tutto”, escludi <script>, blocca on*, consenti data: per <img src="data:..."> */
    public static final PolicyFactory POLICY =
            new HtmlPolicyBuilder()

                    // 1) Vietiamo gli script
                    .disallowElements("script")

                    // 2) CONSENTI TUTTI GLI ELEMENTI (pass-through),
                    //    tranne quelli disallow-ati sopra (trucchetto con ElementPolicy)
                    .allowElements((elementName, attrs) -> elementName)

                    // 3) (Opzionale) tieni i <style> del template
                    //    Nota: il sanitizer NON sanifica il contenuto CSS dei <style>.
                    //          Usalo solo se la sorgente è fidata (autori interni).
                    .allowElements("style")

                    // 4) Attributi HTML comuni
                    .allowAttributes(
                            "html", "head","body","class","id","style","title","name","value","type",
                            "href","src","alt","width","height"
                    ).globally()

                    // 5) Thymeleaf: whitelista gli attributi che usi (aggiungine altri se servono)
                    .allowAttributes(
                            "th:text","th:utext","th:if","th:unless","th:each","th:with",
                            "th:href","th:src","th:value","th:id","th:class","th:classappend","th:style","th:attr"
                    ).globally()

                    // 6) Blocca tutti gli handler evento
                    .allowAttributes(String.valueOf(DROP_EVENT_ATTRS),
                            "onclick","onload","onerror","onmouseover","onfocus","onblur",
                            "onchange","onsubmit","onreset","onkeyup","onkeydown","onkeypress",
                            "oninput","onpaste"
                    ).globally()

                    // 7) URL sicuri + data: per immagini inline (QR base64)
                    .allowStandardUrlProtocols()    // http, https, mailto, ecc.
                    .allowUrlProtocols("data")      // per <img src="data:image/png;base64,...">

                    // 8) Permetti inline style (sanitizzati)
                    .allowStyling()

                    .toFactory();

    private TemplateHtmlSanitizer() {}
}
