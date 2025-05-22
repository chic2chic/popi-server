package com.lgcns.util;

import com.lgcns.error.exception.FeignCustomException;
import feign.FeignException;

public class FeignExceptionUtil {

    public static FeignCustomException from(FeignException e) {
        return new FeignCustomException(e.status(), e.contentUTF8());
    }
}
