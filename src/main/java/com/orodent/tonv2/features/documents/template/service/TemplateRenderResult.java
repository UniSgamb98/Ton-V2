package com.orodent.tonv2.features.documents.template.service;

import java.util.List;

public record TemplateRenderResult(String resolvedMarkup, String html, List<String> warnings) {
}
