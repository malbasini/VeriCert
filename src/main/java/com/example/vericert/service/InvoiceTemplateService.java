package com.example.vericert.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;
import java.util.Map;

@Service
public class InvoiceTemplateService {

    private final SpringTemplateEngine stringTemplateEngine;

    public InvoiceTemplateService(@Qualifier(value = "dbTemplateEngine") SpringTemplateEngine stringTemplateEngine) {
        this.stringTemplateEngine = stringTemplateEngine;
    }

    public String renderInvoiceHtml(String templateHtml,
                                    Map<String, Object> model) {
        Context ctx = new Context(Locale.ITALY);
        ctx.setVariables(model);
        // qui templateHtml è la stringa con th:text, th:each ecc.
        return stringTemplateEngine.process(templateHtml, ctx);
    }
}
