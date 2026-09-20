package com.cyys.admin.release.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReleaseView(String id, String releaseNumber, LocalDate dateUpdate, String type,
                          String typeLabel, boolean typeDisabled, String content, int version,
                          LocalDateTime createdAt) {}
