package @@PACKAGE@@.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.*;
import java.math.BigDecimal;
import java.time.*;

public record Create@@CLASS@@Command(
        @NotBlank @Size(max=32) String departmentId,
        @@PARENT_CREATE@@
        @@COMMAND_FIELDS@@) {}
