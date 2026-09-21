package com.cyys.admin.menu.dto;

public record NavigationMenu(String id,String parentId,String name,String path,int type,int sort,String icon) {}
