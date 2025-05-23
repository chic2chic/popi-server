package com.lgcns.response;

import java.util.List;

public record SliceResponse<T>(List<T> content, boolean isLast) {}
