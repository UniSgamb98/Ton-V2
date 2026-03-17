package com.orodent.tonv2.core.documents.template;

import java.util.List;

public record TemplateRenderResult(String resolvedMarkup, String html, List<String> warnings) {
}
