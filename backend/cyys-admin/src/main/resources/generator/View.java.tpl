package @@PACKAGE@@.dto;

import @@PACKAGE@@.model.@@CLASS@@;
import com.fasterxml.jackson.annotation.*;
import java.math.BigDecimal;
import java.time.*;

public record @@CLASS@@View(String id,String departmentId,@@PARENT_VIEW@@Integer version,LocalDateTime createdAt,@@VIEW_FIELDS@@) {
    public static @@CLASS@@View from(@@CLASS@@ row) {
        return new @@CLASS@@View(row.getId(),row.getOrgId(),@@PARENT_VIEW_VALUE@@row.getVersion(),row.getCreatedAt(),@@VIEW_VALUES@@);
    }
}
