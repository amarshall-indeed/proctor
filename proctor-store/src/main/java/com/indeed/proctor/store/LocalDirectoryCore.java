package com.indeed.proctor.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.Serializers;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author parker
 */
public class LocalDirectoryCore implements FileBasedPersisterCore {
    private static final Logger LOGGER = LogManager.getLogger(LocalDirectoryCore.class);
    private final ObjectMapper objectMapper = Serializers.lenient();

    private final File baseDir;
    private final String testDefinitionsDirectory;

    public LocalDirectoryCore(final File baseDir, final String testDefinitionsDirectory) {
        this.baseDir = baseDir;
        this.testDefinitionsDirectory = testDefinitionsDirectory;
    }

    @Override
    public <C> C getFileContents(final Class<C> c, final String[] path_parts, final C defaultValue, final String revision) throws StoreException.ReadException, JsonProcessingException {
        final String path = String.join(File.separator, path_parts);
        FileReader reader = null;

        try {
            final File file = new File(baseDir + File.separator + path);
            if (file.exists()) {
                reader = new FileReader(file);
                final C testDefinition = objectMapper.readValue(reader, c);
                return testDefinition;
            } else {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(file + " does not exists, returning defaultValue.");
                }
                return defaultValue;
            }
        } catch (final IOException e) {
            Throwables.propagateIfInstanceOf(e, JsonProcessingException.class);
            throw new StoreException.ReadException("Error reading " + path, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    LOGGER.error("Suppressing throwable thrown when closing "+reader, e);
                }
            }
        }
    }

    @Override
    public void close() {
        // no op
    }

    static class LocalRcsClient implements FileBasedProctorStore.RcsClient {
        @Override
        public void add(final File file) throws Exception {
        }

        @Override
        public void delete(final File testDefinitionDirectory) throws Exception {
            testDefinitionDirectory.delete();
        }

        @Override
        public String getRevisionControlType() {
            return null;
        }
    }


    @Override
    public void doInWorkingDirectory(final ChangeMetadata changeMetadata, final String previousVersion, final FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
        try {
            final FileBasedProctorStore.RcsClient rcsClient = new LocalRcsClient();
            final boolean thingsChanged = updater.doInWorkingDirectory(rcsClient, baseDir);
        } catch (final Exception e) {
            throw new StoreException.TestUpdateException("LocalCore: Unable to perform operation: " + e.getMessage(), e);
        } finally {
        }
    }

    @Override
    public TestVersionResult determineVersions(final String fetchRevision) throws StoreException.ReadException {
        final File testDir = new File(baseDir + File.separator + testDefinitionsDirectory);
        // List all of the directories, excluding the directories created by svn (implementation is ignoring directories named '.svn'
        final File[] testDefFiles = testDir.listFiles( (FileFilter) FileFilterUtils.makeSVNAware(FileFilterUtils.directoryFileFilter()) );
        final List<TestVersionResult.Test> tests = Lists.newArrayListWithExpectedSize(testDefFiles.length);
        for (final File testDefFile : testDefFiles) {
            final String testName = testDefFile.getName();
            tests.add(new TestVersionResult.Test(testName, fetchRevision));
        }

        return new TestVersionResult(
            tests,
            new Date(System.currentTimeMillis()),
            System.getenv("USER"),
            String.valueOf(System.currentTimeMillis()),
            ""
        );
    }

    @Override
    public String getAddTestRevision() {
        return "";
    }
}
