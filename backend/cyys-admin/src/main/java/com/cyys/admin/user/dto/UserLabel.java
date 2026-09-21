package com.cyys.admin.user.dto;

/** 用户展示资料；active 仅用于界面状态提示，不作为登录或保存校验依据。 */
public record UserLabel(String id, String name, boolean active) {}
