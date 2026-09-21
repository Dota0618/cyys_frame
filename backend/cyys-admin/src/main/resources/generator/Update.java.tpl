package @@PACKAGE@@.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.*;
import java.math.BigDecimal;
import java.time.*;

public record Update@@CLASS@@Command(
        @NotNull @Min(0) Integer version,
        @@COMMAND_FIELDS@@) {}
