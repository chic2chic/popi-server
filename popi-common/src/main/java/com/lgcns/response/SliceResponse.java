package com.lgcns.response;

import java.util.List;
import org.springframework.data.domain.Slice;

public record SliceResponse<T>(List<T> content, boolean isLast) {
    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<>(slice.getContent(), slice.isLast());
    }
}
