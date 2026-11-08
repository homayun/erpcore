package com.erp.core.security.internal.admin.web.dto;

import java.util.List;

public record PageResponse<T>(List<T> items, long total, int page, int size) {}