package com.cyys.admin.file.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record MoveFilesCommand(@NotEmpty @Size(max=100) List<@NotNull @Valid FileVersionReference> files,
                                @Size(max=32) String directoryId) {}
