package com.cyys.admin.generator.dto;

public record GeneratorColumn(String name, String dataType, String columnType, Long length,
                              Integer scale, boolean nullable, String keyType, String extra, String comment) {}
