package com.cyys.admin.region.dto;

import jakarta.validation.constraints.Size;

public record ExportRegionsCommand(@Size(max = 32) String parentId, @Size(max = 20) String name) {}
