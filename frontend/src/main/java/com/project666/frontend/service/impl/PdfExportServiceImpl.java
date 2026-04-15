package com.project666.frontend.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.project666.frontend.service.PdfExportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfExportServiceImpl implements PdfExportService {
    private static final Logger log = LoggerFactory.getLogger(PdfExportServiceImpl.class);

    private final SpringTemplateEngine templateEngine;

    @Override
    public byte[] render(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String html = templateEngine.process(templateName, context);

            return renderPdf(templateName, html);
        } catch (Exception e) {
            String fullError = buildFullErrorMessage(templateName, e);
            log.error(fullError, e);
            throw new IllegalStateException(fullError, e);
        }
    }

    private byte[] renderPdf(String templateName, String html) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            String baseUri = new ClassPathResource("static/").getURL().toString();
            builder.withHtmlContent(html, baseUri);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        }
    }

    private String buildFullErrorMessage(String templateName, Exception exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return "Failed to generate PDF for template '" + templateName + "'\n" + stringWriter;
    }
}
