package com.indeed.proctor.webapp.tags;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.Serializers;

import java.io.IOException;

public final class JSONFunctions {

    private static final ObjectMapper OBJECT_MAPPER = Serializers.strict();

    public static String prettyPrintJSON(final Object o)
            throws IOException, JsonGenerationException, JsonMappingException {
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(o);
    }
}
