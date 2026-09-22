package com.cyys.admin.recycle.dto;

public record RecycleResourceView(String code, String name, String searchLabel, String description,
                                  boolean scopeRequired, boolean canRestore, boolean canPurge) {}
