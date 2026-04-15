package com.project666.frontend.service;

import java.util.Map;

public interface PdfExportService {
    byte[] render(String templateName, Map<String, Object> variables);
}
