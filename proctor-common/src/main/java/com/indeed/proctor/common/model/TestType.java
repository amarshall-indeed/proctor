package com.indeed.proctor.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Value class that captures most of the enum flavor while allowing
 * library users to extend the supported types of test.
 */
public final class TestType implements JsonSerializable {
    private static final ConcurrentMap<String, TestType> TYPES = Maps.newConcurrentMap();
    @Nonnull
    private final String name;
    @Nonnull
    private final Set<String> allowedDependencies = new HashSet<>();
    // Use the factory
    private TestType(@Nonnull final String id) {
        this.name = id;
        allowedDependencies.add(id);
    }

    @Nonnull
    public static TestType register(@Nonnull final String name) {
        final TestType testType = new TestType(name);
        final TestType previous = TYPES.putIfAbsent(testType.name(), testType);
        return previous != null ? previous : testType;
    }

    public void addAllowedDependency(@Nonnull final TestType dependency) {
        allowedDependencies.add(dependency.name);
    }

    public boolean isAllowedDependency(@Nonnull final TestType dependency) {
        return allowedDependencies.contains(dependency.name);
    }

    public String allowedDependenciesToString() {
        return allowedDependencies.stream().sorted().collect(Collectors.joining(", "));
    }

    // Emulate enum
    public String name() {
        return name;
    }

    // Emulate enum
    public static TestType[] values() {
        final Collection<TestType> allRegistered = all();
        return allRegistered.toArray(new TestType[allRegistered.size()]);
    }

    // Equivalent to values()
    public static Collection<TestType> all() {
        return ImmutableList.copyOf(TYPES.values());
    }

    // Emulate enum
    @Override
    public void serializeWithType(
            final JsonGenerator jsonGenerator,
            final SerializerProvider serializerProvider,
            final TypeSerializer typeSerializer
    ) throws IOException {
        jsonGenerator.writeString(name);
    }

    @Override
    public void serialize(
            @Nonnull final JsonGenerator jsonGenerator,
            @Nonnull final SerializerProvider serializerProvider
    ) throws IOException {
        jsonGenerator.writeString(name);
    }

    // Emulate enum
    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TestType)) {
            return false;
        }

        final TestType testType = (TestType) o;

        return name.equals(testType.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public static final TestType ANONYMOUS_USER = register("USER");
    /**
     * @deprecated Use the more descriptive {@link #ANONYMOUS_USER} instead
     */
    public static final TestType USER = ANONYMOUS_USER;

    public static final TestType AUTHENTICATED_USER = register("ACCOUNT");
    /**
     * @deprecated Use the more descriptive {@link #AUTHENTICATED_USER} instead
     */
    public static final TestType ACCOUNT = AUTHENTICATED_USER;

    public static final TestType EMAIL_ADDRESS = register("EMAIL");
    /**
     * @deprecated Use the more descriptive {@link #EMAIL_ADDRESS} instead
     */
    public static final TestType EMAIL = EMAIL_ADDRESS;
    public static final TestType RANDOM = register("RANDOM");

    /**
     * @deprecated Legacy from migration to github
     */
    public static final TestType PAGE = register("PAGE");
    /**
     * @deprecated Legacy from migration to github
     */
    public static final TestType COMPANY = register("COMPANY");

    @JsonCreator
    public static TestType getTestType(final String value) {
        if (TYPES.containsKey(value)) {
            return TYPES.get(value);
        } else {
            return new TestType(value);
        }
    }
}
