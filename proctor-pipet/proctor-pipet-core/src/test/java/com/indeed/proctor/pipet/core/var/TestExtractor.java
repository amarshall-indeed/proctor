package com.indeed.proctor.pipet.core.var;

import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.pipet.core.config.ExtractorSource;
import com.indeed.proctor.pipet.core.web.BadRequestException;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** @author parker */
public class TestExtractor {

    private static final String DEFAULT_COUNTRY = "US";
    private static final String COUNTRY_QUERY_PARAM = "ctx.co";
    private static final String LANGUAGE_HEADER_NAME = "X-Lang";
    private static final String USER_IDENTIFIER_QUERY_PARAM = "id.user";

    @Test
    public void testExtractWithAllDataProvided() {
        final Extractor extractor = getBasicExtractor();

        final String country = "CA";
        final String language = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(COUNTRY_QUERY_PARAM, country);
        request.addHeader(LANGUAGE_HEADER_NAME, language);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);

        final RawParameters parameters = extractor.extract(request);
        final Map<String, String> context = parameters.getContext();
        assertEquals(country, context.get("country"));
        assertEquals(language, context.get("language"));
        assertEquals(2, context.size());

        final Map<TestType, String> ids = parameters.getIdentifiers();
        assertEquals(1, ids.size());
        assertEquals(userId, ids.get(TestType.ANONYMOUS_USER));

        assertNull(parameters.getTest());
        assertEquals("", parameters.getForceGroups());
    }

    @Test
    public void testDefaultValues() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);

        final RawParameters parameters = extractor.extract(request);
        final Map<String, String> context = parameters.getContext();
        assertEquals(DEFAULT_COUNTRY, context.get("country"));
        assertEquals("fr", context.get("language"));
        assertEquals(2, context.size());

        final Map<TestType, String> ids = parameters.getIdentifiers();
        assertEquals(1, ids.size());
        assertEquals(userId, ids.get(TestType.ANONYMOUS_USER));

        assertNull(parameters.getTest());
        assertEquals("", parameters.getForceGroups());
    }

    @Test
    public void testForceGroupsProvided() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);


        request.setParameter("prforceGroups", "mytestbucket1");

        final RawParameters parameters = extractor.extract(request);


        assertNull(parameters.getTest());
        assertEquals("mytestbucket1", parameters.getForceGroups());
    }

    @Test
    public void testEmptyFilterTestsProvided() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);


        request.setParameter("test", "");

        final RawParameters parameters = extractor.extract(request);


        assertNotNull(parameters.getTest());
        assertTrue(parameters.getTest().isEmpty());
    }

    @Test
    public void testFilterTestsProvided() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);


        request.setParameter("test", "firsttest,,secondtest,thirdtest,,");

        final RawParameters parameters = extractor.extract(request);


        assertNotNull(parameters.getTest());
        assertEquals(3, parameters.getTest().size());
        assertTrue(parameters.getTest().contains("firsttest"));
        assertTrue(parameters.getTest().contains("secondtest"));
        assertTrue(parameters.getTest().contains("thirdtest"));
    }


    @Test(expected = BadRequestException.class)
    public void testExtractWithMissingIdentifier() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);

        final RawParameters parameters = extractor.extract(request);
    }

    @Test(expected = BadRequestException.class)
    public void testExtractWithMissingContextParameter() {
        final Extractor extractor = getBasicExtractor();

        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);

        final RawParameters parameters = extractor.extract(request);
    }

    @Test
    public void testExtractWithExtraQueryParameters() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);


        request.setParameter("id.blah", "True");
        request.setParameter("ctx.blah", "True");
        request.setParameter("blah", "True");

        final RawParameters parameters = extractor.extract(request);
        // No exception even though unrecognized parameters were passed.
    }

    private Extractor getBasicExtractor() {
        // default country
        final ContextVariable country =
            ContextVariable.newBuilder()
                .setVarName("country")
                .setSourceKey("co")
                .setDefaultValue(DEFAULT_COUNTRY)
                .setConverter(ValueConverters.stringValueConverter())
                .build();
        // no default langauge
        final ContextVariable language =
                    ContextVariable.newBuilder()
                        .setVarName("language")
                        .setSourceKey("X-Lang")
                        .setSource(ExtractorSource.HEADER)
                        .setConverter(ValueConverters.stringValueConverter())
                        .build();
        final Identifier user = Identifier.forTestType(ExtractorSource.QUERY, TestType.ANONYMOUS_USER);
        return new Extractor(ImmutableList.of(country, language), ImmutableList.of(user));
    }
}
