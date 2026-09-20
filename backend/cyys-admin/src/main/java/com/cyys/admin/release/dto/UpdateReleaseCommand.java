package com.cyys.admin.release.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record UpdateReleaseCommand(
        @NotBlank @Size(max=10) String releaseNumber,
        @NotNull @JsonFormat(shape=JsonFormat.Shape.STRING,pattern="uuuu-MM-dd",lenient=OptBoolean.FALSE) LocalDate dateUpdate,
        @NotBlank @Size(max=20) String type,
        @NotBlank @Size(max=1000) String content,
        @NotNull @Size(max=10) List<@NotBlank @Size(max=32) String> attachments,
        @NotNull @Min(0) Integer version) {}
