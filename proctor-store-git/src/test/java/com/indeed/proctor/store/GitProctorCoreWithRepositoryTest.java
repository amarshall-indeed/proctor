package com.indeed.proctor.store;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

public class GitProctorCoreWithRepositoryTest extends RepositoryTestCase {

    private static final String GIT_USERNAME = "username";
    private static final String GIT_PASSWORD = "password";
    private static final String TEST_DEFINITION_DIRECTORY = "matrices/test-definitions";
    private static final String COMMIT_MESSAGE = "Initial commit";
    private static final String TEST_FILE_NAME = "GitProctorCoreTest.txt";

    private Git remoteGit;

    private String gitUrl;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        remoteGit = new Git(db);

        gitUrl = "file://" + remoteGit.getRepository().getWorkTree().getAbsolutePath();

        writeTrashFile(TEST_FILE_NAME, "GitProctorCoreTest");
        remoteGit.add().addFilepattern(TEST_FILE_NAME).call();
        remoteGit.commit().setMessage(COMMIT_MESSAGE).call();
    }

    @Test
    public void testCloneRepository() throws Exception {
        final File workingDir = temporaryFolder.newFolder("testCloneRepository");

        // Run cloneRepository
        final GitProctorCore gitProctorCore = new GitProctorCore(
                gitUrl,
                GIT_USERNAME,
                GIT_PASSWORD,
                TEST_DEFINITION_DIRECTORY,
                workingDir
        );

        final Git git = gitProctorCore.getGit();

        assertNotNull(git);
        assertThat(git.getRepository().getBranch()).isEqualTo("master");

        final StoredConfig config = git.getRepository().getConfig();
        assertThat(config.getBoolean("pack", "singlePack", false)).isTrue();

        final String localCommitMessage = git.log().call().iterator().next().getFullMessage();
        assertThat(localCommitMessage).isEqualTo(COMMIT_MESSAGE);
    }

    /**
     * clone repo having master and test branch with only fetching refs of test branch
     */
    @Test
    public void testCloneRepositoryWithSingleBranch() throws Exception {
        final File workingDir = temporaryFolder.newFolder("testCloneRepositoryWithSingleBranch");
        final String branchName = "test";

        /* create branch test with one commit off master ***/
        remoteGit.checkout().setCreateBranch(true).setName(branchName).call();

        writeTrashFile(TEST_FILE_NAME, "test");
        remoteGit.add().addFilepattern(TEST_FILE_NAME).call();

        final String commitMessage = "Create a new branch";
        remoteGit.commit().setMessage(commitMessage).call();

        // Run cloneRepository with single branch
        final GitProctorCore gitProctorCore = new GitProctorCore(
                gitUrl,
                GIT_USERNAME,
                GIT_PASSWORD,
                TEST_DEFINITION_DIRECTORY,
                workingDir,
                branchName
        );

        final Git git = gitProctorCore.getGit();

        assertNotNull(git);
        final List<Ref> branchList = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        // should not contain master
        assertThat(branchList.stream().map(Ref::getName))
                .containsExactlyInAnyOrder("refs/heads/" + branchName, "refs/remotes/origin/test");

        final String localCommitMessage = git.log().call().iterator().next().getFullMessage();
        assertThat(localCommitMessage).isEqualTo(commitMessage);

        // should not throw any exceptions
        gitProctorCore.checkoutBranch(branchName);
    }
}
