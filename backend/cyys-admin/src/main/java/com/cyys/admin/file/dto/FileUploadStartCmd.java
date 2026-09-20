package com.cyys.admin.file.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record FileUploadStartCmd(@NotBlank @Size(max=200) String name, @Min(1) long bytes,
        @NotEmpty @Size(max=2048) List<@NotNull @Pattern(regexp="[a-f0-9]{64}") String> hashes,
        @Size(max=32) String directoryId) {}
