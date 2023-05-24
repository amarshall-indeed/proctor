package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import javax.annotation.Nullable;
import java.util.Objects;


/**
 * A dynamic filter that matches test name using prefix string
 * @deprecated Use {@link MetaTagsFilter}
 */
@Deprecated
@JsonTypeName("name_prefix")
public class TestNamePrefixFilter implements DynamicFilter {
    private final String prefix;

    /**
     * Construct the filter from test name prefix string
     *
     * @param prefix prefix string matches the test names
     * @throws IllegalArgumentException if prefix is empty string
     */
    public TestNamePrefixFilter(@JsonProperty("prefix") final String prefix) {
        Preconditions.checkArgument(!prefix.isEmpty(), "Prefix should be non-empty string");
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public boolean matches(@Nullable final String testName, final ConsumableTestDefinition testDefinition) {
        return !Strings.isNullOrEmpty(testName) && testName.startsWith(prefix);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TestNamePrefixFilter that = (TestNamePrefixFilter) o;
        return Objects.equals(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix);
    }
}
