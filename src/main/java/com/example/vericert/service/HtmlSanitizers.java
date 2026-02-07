package com.example.vericert.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlSanitizers {

    // CONSENTE data: per il QR base64
    private static final String[] URL_PROTO = {"http", "https", "data"};

    // Se i template contengono ancora attributi Thymeleaf (th:*),
    // devi consentirli, altrimenti vengono rimossi.
    private static final String[] TH_ATTRS = {
            "th:text","th:utext","th:if","th:each","th:classappend",
            "th:src","th:href","th:replace","th:insert","th:remove"
    };

    public static final PolicyFactory TEMPLATE_POLICY =
            new HtmlPolicyBuilder()
                    // Tag usati nel certificato
                    .allowElements("html","head","meta","title","body",
                            "header","section","footer","div","span","small","strong","em",
                            "h1","h2","h3","h4","h5","h6","p","ul","ol","li",
                            "img","br","hr","style")
                    // Attributi globali
                    .allowAttributes("class","id","style").onElements("*")
                    // Meta (charset, viewport, ecc.)
                    .allowAttributes("charset","content","http-equiv","name").onElements("meta")
                    // IMG con data: URL (per QR base64)
                    .allowAttributes("src","alt","width","height").onElements("img")
                    .allowUrlProtocols(URL_PROTO)
                    // (Facoltativo) consenti attributi Thymeleaf se presenti nel DB
                    .allowAttributes(TH_ATTRS).onElements("*")
                    // Niente script attivo
                    .disallowElements("script")
                    // (Opz.) stile inline: se vuoi restringere, togli "style" sopra
                    .toFactory();
}
