package com.indeed.proctor.builder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Strings;
import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorReader;
import com.indeed.proctor.store.StoreException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * @author parker
 */
class ProctorBuilderUtils {

    private static final ObjectWriter OBJECT_WRITER = Serializers
            .lenient()
            .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false).writerWithDefaultPrettyPrinter();

    static void generateArtifact(final ProctorReader proctorPersister, final Writer outputSink,
                                           final String authorOverride, final String versionOverride
    ) throws IOException, IncompatibleTestMatrixException, StoreException {
        final TestMatrixVersion currentTestMatrix = proctorPersister.getCurrentTestMatrix();
        if (currentTestMatrix == null) {
            throw new RuntimeException("Failed to load current test matrix for " + proctorPersister);
        }

        // I'm not sure if it's better for the LocalDirectoryPersister to be aware of this svn info, or for all the overrides to happen here.
        if (StringUtils.isNotBlank(authorOverride)) {
            currentTestMatrix.setAuthor(authorOverride);
        }
        if (!Strings.isNullOrEmpty(versionOverride)) {
            currentTestMatrix.setVersion(versionOverride);
        }

        final TestMatrixArtifact artifact = ProctorUtils.convertToConsumableArtifact(currentTestMatrix);

        // For each test, verify that it's internally consistent (buckets sum to 1.0, final null allocation)
        final String matrixSource = artifact.getAudit().getUpdatedBy() + "@" + artifact.getAudit().getVersion();
        for (final Map.Entry<String, ConsumableTestDefinition> td : artifact.getTests().entrySet()) {
            ProctorUtils.verifyInternallyConsistentDefinition(td.getKey(), matrixSource, td.getValue());
        }
        OBJECT_WRITER.writeValue(outputSink, artifact);
    }
}
