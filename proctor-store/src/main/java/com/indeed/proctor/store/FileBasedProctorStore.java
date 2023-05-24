package com.indeed.proctor.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.util.varexport.Export;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author ketan
 * @author parker
 */
public abstract class FileBasedProctorStore implements ProctorStore {
    private static final Logger LOGGER = LogManager.getLogger(FileBasedProctorStore.class);
    private static final String SUFFIX = ".json";
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();
    static final String TEST_METADATA_FILENAME = "metadata" + SUFFIX;
    static final String TEST_DEFINITION_FILENAME = "definition" + SUFFIX;

    public static final String DEFAULT_TEST_DEFINITIONS_DIRECTORY = "test-definitions";
    private final String testDefinitionsDirectory;

    protected final FileBasedPersisterCore core;

    protected FileBasedProctorStore(final FileBasedPersisterCore core, final String testDefinitionsDirectory) {
        this.core = core;
        this.testDefinitionsDirectory = testDefinitionsDirectory;
    }

    protected FileBasedProctorStore(final FileBasedPersisterCore core) {
        this(core, DEFAULT_TEST_DEFINITIONS_DIRECTORY);
    }

    /**
     * @return true if the file has changed
     */
    protected static <T> boolean writeIfChanged(final File f, final T newThing) throws StoreException.TestUpdateException {
        if (f.exists()) {
            try {
                final T currentThing = (T) OBJECT_MAPPER.readValue(f, (Class<T>) newThing.getClass());
                if (currentThing.equals(newThing)) {
                    return false;
                }
            } catch (final IOException e) {
                throw new StoreException.TestUpdateException("Unable to parse instance of " + newThing.getClass().getCanonicalName() + " from " + f, e);
            }
        }
        FileBasedProctorStore.writeThing(f, newThing);
        return true;
    }

    protected static <T> void writeThing(final File f, final T newThing) throws StoreException.TestUpdateException {
        try {
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(f, newThing);
        } catch (final IOException e) {
            throw new StoreException.TestUpdateException("Unable to write instance of " + newThing.getClass().getCanonicalName() + " to " + f, e);
        }
    }

    File getTestDefinitionDirectoryForTest(final String testName, final File workingDir) {
        return new File(workingDir + File.separator + getTestDefinitionsDirectory() + File.separator + testName);
    }

    String getTestDefinitionsDirectory() {
        return testDefinitionsDirectory;
    }

    @Override
    public TestMatrixVersion getCurrentTestMatrix() throws StoreException {
        return getTestMatrix(this.getLatestVersion());
    }

    @Export(name="core", doc="")
    public FileBasedPersisterCore getCore() {
        return core;
    }

    @Override
    public final TestMatrixVersion getTestMatrix(final String fetchRevision) throws StoreException {
        long start = System.currentTimeMillis();
        final TestVersionResult result = core.determineVersions(fetchRevision);
        if (LOGGER.isDebugEnabled()) {
            final long elapsed = System.currentTimeMillis() - start;
            LOGGER.debug(String.format("Took %d ms to identify %d potential tests", elapsed, result.getTests().size()));
        }
        if (result == null) {
            LOGGER.error("Unable to determine tests for " + core.toString());
            return null;
        }
        final TestMatrixVersion tmv = new TestMatrixVersion();

        final Map<String, TestDefinition> testDefinitions = Maps.newLinkedHashMap();
        start = System.currentTimeMillis();
        for (final TestVersionResult.Test testDefFile : result.getTests()) {
            final long startForTest = System.currentTimeMillis();
            final TestDefinition testDefinition = getTestDefinition(testDefFile.getTestName(), testDefFile.getRevision());
            if (LOGGER.isTraceEnabled()) {
                final long elapsed = System.currentTimeMillis() - startForTest;
                LOGGER.trace(String.format(
                        "Took %d ms to load %s (r%s) %s", elapsed, testDefFile.getTestName(), testDefFile.getRevision(), testDefinition == null ? "unsuccessfully" : "successfully"));
            }
            if (testDefinition == null) {
                LOGGER.info("Returning null TestMatrix because " + testDefFile.getTestName() + " returned null test-definition.");
                return null;
            }
            testDefinitions.put(testDefFile.getTestName(), testDefinition);
        }
        if (LOGGER.isDebugEnabled()) {
            final long elapsed = System.currentTimeMillis() - start;
            LOGGER.debug(String.format("Took %d ms to load all %d tests", elapsed, testDefinitions.size()));
        }

        final TestMatrixDefinition tmd = new TestMatrixDefinition();
        tmd.setTests(testDefinitions);

        tmv.setTestMatrixDefinition(tmd);

        tmv.setPublished(result.getPublished());
        tmv.setAuthor(result.getAuthor());
        tmv.setVersion(result.getVersion());
        tmv.setDescription(result.getDescription());

        return tmv;
    }

    @Override
    public TestDefinition getCurrentTestDefinition(final String testName) throws StoreException {
        // Get the first test history
        final List<Revision> tdvList = this.getHistory(testName, 0, 1);
        if (tdvList.size() == 1) {
            final Revision tdv = tdvList.get(0);
            return getTestDefinition(testName, tdv.getRevision());
        } else {
            LOGGER.info("Not history returned for " + testName + ", returning null");
            return null;
        }
    }

    @Override
    public TestDefinition getTestDefinition(final String testName, final String fetchRevision) throws StoreException {
        try {
            return getFileContents(TestDefinition.class, new String[] { getTestDefinitionsDirectory(), testName, TEST_DEFINITION_FILENAME }, null, fetchRevision);
        } catch (final JsonProcessingException e) {
            throw new StoreException(String.format("Unable to deserialize JSON for %s r%s", testName, fetchRevision), e);
        }
    }

    public void shutdown() {
        try {
            close();
        } catch (final IOException e) {
            LOGGER.error("Ignored exception during closing", e);
        }
    }

    @Override
    public void close() throws IOException {
        core.close();
    }

    private final <C> C getFileContents(final Class<C> c, final String[] path, final C defaultValue, final String revision) throws StoreException.ReadException, JsonProcessingException {
        return core.getFileContents(c, path, defaultValue, revision);
    }

    protected final <T> boolean updateThing(final FileBasedProctorStore.RcsClient rcsClient, final File file, final T thing) throws Exception {
        final boolean thingExisted = file.exists();
        final boolean thingChanged = FileBasedProctorStore.writeIfChanged(file, thing);
        if (!thingExisted || rcsClient.getRevisionControlType().equals("git")) {
            rcsClient.add(file);
        }
        return thingChanged;
    }

    @Override
    public final void updateTestDefinition(
            final ChangeMetadata changeMetadata,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata
    ) throws StoreException.TestUpdateException {
        LOGGER.info(String.format("Update Test Definition: username=%s author=%s testName=%s previousVersion=r%s",
                changeMetadata.getUsername(), changeMetadata.getAuthor(), testName, previousVersion));
        core.doInWorkingDirectory(changeMetadata, previousVersion, new ProctorUpdater() {
            @Override
            public boolean doInWorkingDirectory(final RcsClient rcsClient, final File workingDir) throws Exception {
                final File testDefinitionDirectory = getTestDefinitionDirectoryForTest(testName, workingDir);
                final File testDefinitionFile = new File(testDefinitionDirectory + File.separator + TEST_DEFINITION_FILENAME);

                if (!testDefinitionFile.exists()) {
                    throw new StoreException.TestUpdateException("Attempting to update non-existent test " + testName);
                }
                //  this is easier than trying to get svnKit to do a useful diff
                boolean thingsChanged = updateThing(rcsClient, testDefinitionFile, testDefinition);

                final File metaDataFile = new File(testDefinitionDirectory + File.separator + TEST_METADATA_FILENAME);
                thingsChanged = updateThing(rcsClient, metaDataFile, metadata) || thingsChanged;
                if (!thingsChanged) {
                    throw new StoreException.TestUpdateException("Attempting to save test definition without changes for test " + testName);
                }
                return thingsChanged;
            }
        });
    }

    @Override
    public final void addTestDefinition(
            final ChangeMetadata changeMetadata,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata
    ) throws StoreException.TestUpdateException {
        LOGGER.info(String.format("Add Test Definition: %s %s", changeMetadata.getUsername(), testName));
        core.doInWorkingDirectory(changeMetadata, core.getAddTestRevision(), new ProctorUpdater() {
            @Override
            public boolean doInWorkingDirectory(final RcsClient rcsClient, final File workingDir) throws Exception {
                final File testDefinitionDirectory = getTestDefinitionDirectoryForTest(testName, workingDir);
                final File testDefinitionFile = new File(testDefinitionDirectory + File.separator + TEST_DEFINITION_FILENAME);
                final File metaDataFile = new File(testDefinitionDirectory + File.separator + TEST_METADATA_FILENAME);

                if (testDefinitionFile.exists() || metaDataFile.exists()) {
                    throw new StoreException.TestUpdateException("Supposedly new test '" + testName + "' already exists");
                }

                testDefinitionDirectory.mkdirs();

                writeThing(testDefinitionFile, testDefinition);
                rcsClient.add(testDefinitionFile);

                writeThing(metaDataFile, metadata);
                rcsClient.add(metaDataFile);

                return true;
            }
        });
    }

    @Override
    public final void deleteTestDefinition(
            final ChangeMetadata changeMetadata,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition
    ) throws StoreException.TestUpdateException {
        LOGGER.info(String.format("Delete Test Definition: %s %s r%s ", changeMetadata.getUsername(), testName, previousVersion));
        core.doInWorkingDirectory(changeMetadata, previousVersion, new ProctorUpdater() {
            @Override
            public boolean doInWorkingDirectory(final RcsClient rcsClient, final File workingDir) throws Exception {
                final File testDefinitionDirectory = getTestDefinitionDirectoryForTest(testName, workingDir);

                if (!testDefinitionDirectory.exists()) {
                    throw new StoreException.TestUpdateException("Unable to delete non-existent test " + testName);
                }
                rcsClient.delete(testDefinitionDirectory);

                return true;
            }
        });
    }

    public interface ProctorUpdater {
        boolean doInWorkingDirectory(FileBasedProctorStore.RcsClient rcsClient, File workingDir) throws Exception;
    }

    public interface RcsClient {
        void add(File file) throws Exception;

        void delete(File testDefinitionDirectory) throws Exception;

        String getRevisionControlType();
    }

}
