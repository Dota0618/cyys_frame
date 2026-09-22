package com.cyys.admin.recycle.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record RecycleBatchCommand(@NotEmpty @Size(max=100) List<@NotNull @Valid RecycleReference> records) {}
