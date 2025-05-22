package com.lgcns.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FeignErrorParserUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static Object extractData(String rawBody) {
        try {
            JsonNode root = mapper.readTree(rawBody);
            return mapper.treeToValue(root.path("data"), Object.class);
        } catch (Exception e) {
            return rawBody;
        }
    }
}
