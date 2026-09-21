package com.cyys.admin.generator.dto;

import java.util.List;

public record GeneratorPreview(String templateVersion, String sha256, List<GeneratedSource> files) {}
