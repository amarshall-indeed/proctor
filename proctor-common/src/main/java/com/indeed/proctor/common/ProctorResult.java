package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.emptySortedMap;

/**
 * Return value from {@link Proctor#determineTestGroups(Identifiers, java.util.Map, java.util.Map)}
 * @author ketan
 *
 */
public class ProctorResult {
    public static final ProctorResult EMPTY = new ProctorResult(
            Audit.EMPTY_VERSION,
            emptySortedMap(),
            emptySortedMap(),
            emptyMap()
    );

    private final String matrixVersion;
    /**
     * maps from testname to bucket
     */
    @Nonnull
    private final SortedMap<String, TestBucket> buckets;
    /**
     * maps from testname to allocation
     */
    @Nonnull
    private final SortedMap<String, Allocation> allocations;
    /**
     * maps from testname to TestDefinition
     */
    @Nonnull
    private final Map<String, ConsumableTestDefinition> testDefinitions;

    /**
     * Create a ProctorResult with copies of the provided collections
     * @deprecated this constructor creates copies of all inputs
     */
    @Deprecated
    public ProctorResult(
            @Nonnull final int matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            // allowing null for historical reasons
            @Nullable final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        this(Integer.toString(matrixVersion), buckets, emptyMap(), testDefinitions);
    }

    /**
     * Create a ProctorResult with copies of the provided collections
     * @deprecated this constructor creates copies of all inputs
     */
    @Deprecated
    public ProctorResult(
            @Nonnull final String matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            // allowing null for historical reasons
            @Nullable final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        this(matrixVersion, buckets, emptyMap(), testDefinitions);
    }

    /**
     * Create a ProctorResult with copies of the provided collections
     * @param matrixVersion any string, used for debugging
     * @param buckets the resolved bucket for each test
     * @param allocations the determined allocation for each test
     * @param testDefinitions the original test definitions
     * @deprecated this constructor creates copies of all input collections
     */
    @Deprecated
    public ProctorResult(
            final String matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            @Nonnull final Map<String, Allocation> allocations,
            // allowing null for historical reasons
            @Nullable final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        // Potentially client applications might need to build ProctorResult instances in each request, and some apis
        // have large proctorResult objects, so if teams use this constructor, this may have a noticeable
        // impact on latency and GC, so ideally clients should avoid this constructor.
        this(
                matrixVersion,
                new TreeMap<>(buckets),
                new TreeMap<>(allocations),
                (testDefinitions == null) ? emptyMap() : new HashMap<>(testDefinitions)
        );
    }

    /**
     * Plain constructor, not creating TreeMaps.
     *
     * @param matrixVersion any string, used for debugging
     * @param buckets the resolved bucket for each test
     * @param allocations the determined allocation for each test
     * @param testDefinitions the original test definitions
     */
    public ProctorResult(
            @Nonnull final String matrixVersion,
            @Nonnull final SortedMap<String, TestBucket> buckets,
            @Nonnull final SortedMap<String, Allocation> allocations,
            @Nonnull final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        this.matrixVersion = matrixVersion;
        this.buckets = buckets;
        this.allocations = allocations;
        this.testDefinitions = testDefinitions;
    }

    /**
     * @return a new Proctor Result, which does not allow modifying the contained collections.
     * The result's fields are views of the original fields, to reduce memory allocation effort.
     */
    public static ProctorResult unmodifiableView(final ProctorResult proctorResult) {
        return new ProctorResult(
                proctorResult.matrixVersion,
                // using fields directly because methods do not expose SortedMap type
                Collections.unmodifiableSortedMap(proctorResult.buckets),
                Collections.unmodifiableSortedMap(proctorResult.allocations),
                Collections.unmodifiableMap(proctorResult.testDefinitions)
        );
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getMatrixVersion() {
        return matrixVersion;
    }

    /**
     * @return a SortedMap (should be ordered by testname)
     */
    @Nonnull
    // returning Map instead of SortedMap for historic reasons (changing breaks compiled libraries)
    public Map<String, TestBucket> getBuckets() {
        return buckets;
    }

    /**
     * @return a SortedMap (should be ordered by testname)
     */
    @Nonnull
    // returning Map instead of SortedMap for historic reasons (changing breaks compiled libraries)
    public Map<String, Allocation> getAllocations() {
        return allocations;
    }

    @Nonnull
    public Map<String, ConsumableTestDefinition> getTestDefinitions() {
        return testDefinitions;
    }
}
