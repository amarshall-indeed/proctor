package com.indeed.proctor.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.indeed.proctor.store.cache.CachingProctorStore;
import com.indeed.util.varexport.Export;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tmatesoft.svn.core.SVNURL;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author parker
 * @deprecated we consolidated different levels of cache into {@link CachingProctorStore}
 */
@Deprecated
public class CachedSvnPersisterCore implements SvnPersisterCore {
    private static final Logger LOGGER = LogManager.getLogger(CachedSvnPersisterCore.class);

    private final Cache<FileContentsKey, Object> testDefinitionCache = CacheBuilder.newBuilder()
        .maximumSize(2048)
        .expireAfterAccess(6, TimeUnit.HOURS)
        .build();

    private final Cache<Long, TestVersionResult> versionCache = CacheBuilder.newBuilder()
            .maximumSize(3)
            .expireAfterAccess(6, TimeUnit.HOURS)
            .build();

    final SvnPersisterCoreImpl core;

    public CachedSvnPersisterCore(final SvnPersisterCoreImpl core) {
        this.core = core;
    }

    @Override
    public <T> T doWithClientAndRepository(final SvnOperation<T> operation) throws StoreException {
        return core.doWithClientAndRepository(operation);
    }

    @Override
    @Export(name = "svn-url")
    public SVNURL getSvnUrl() {
        return core.getSvnUrl();
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        return core.cleanUserWorkspace(username);
    }

    @Override
    public String toString() {
        return "Cached: " + core.getSvnPath();
    }

    @Override
    public <C> C getFileContents(final Class<C> c,
                                 final String[] path,
                                 final C defaultValue,
                                 final String revision) throws StoreException.ReadException, JsonProcessingException {
        final FileContentsKey key = new FileContentsKey(c, path, core.parseRevisionOrDie(revision));
        final Object obj = testDefinitionCache.getIfPresent(key);
        if (obj == null) {
            final C x = core.getFileContents(c, path, defaultValue, revision);
            if (x != defaultValue) {
                testDefinitionCache.put(key, x);
            }
            return x;
        } else {
            if (c.isAssignableFrom(obj.getClass())) {
                return c.cast(obj);
            }
            return core.getFileContents(c, path, defaultValue, revision);
        }
    }

    @Override
    public void doInWorkingDirectory(
            final ChangeMetadata changeMetadata,
            final String previousVersion,
            final FileBasedProctorStore.ProctorUpdater updater
    ) throws StoreException.TestUpdateException {
        core.doInWorkingDirectory(changeMetadata, previousVersion, updater);
    }

    @Override
    public TestVersionResult determineVersions(final String fetchRevision) throws StoreException.ReadException {
        final Long parsedRevision = SvnPersisterCoreImpl.parseRevisionOrDie(fetchRevision);
        final TestVersionResult testVersionResult = versionCache.getIfPresent(parsedRevision);
        if (testVersionResult == null) {
            final TestVersionResult newTestVersionResult = core.determineVersions(String.valueOf(parsedRevision));
            versionCache.put(parsedRevision, newTestVersionResult);
            return newTestVersionResult;
        } else {
            return testVersionResult;
        }
    }

    @Override
    public String getAddTestRevision() {
        return core.getAddTestRevision();
    }

    public void shutdown() {
        try {
            close();
        } catch (final IOException e) {
            LOGGER.error("Ignored exception during closing of core", e);
        }
    }

    @Override
    public void close() throws IOException{
        core.close();
    }

    private static class FileContentsKey {
        final Class c;
        final String[] path;
        final long revision;

        private FileContentsKey(final Class c, final String[] path, final long revision) {
            this.c = c;
            this.path = path;
            this.revision = revision;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FileContentsKey)) {
                return false;
            }

            final FileContentsKey that = (FileContentsKey) o;

            if (revision != that.revision) {
                return false;
            }
            if (c != null ? !c.equals(that.c) : that.c != null) {
                return false;
            }
            if (!Arrays.equals(path, that.path)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = c != null ? c.hashCode() : 0;
            result = 31 * result + (path != null ? Arrays.hashCode(path) : 0);
            result = 31 * result + (int) (revision ^ (revision >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return String.join("/", path) + "@r" + revision;
        }
    }
}
