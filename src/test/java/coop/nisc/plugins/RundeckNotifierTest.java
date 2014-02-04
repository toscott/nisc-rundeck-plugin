package coop.nisc.plugins;

import hudson.model.Action;
import hudson.model.Build;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.scm.SubversionSCM;
import hudson.tasks.Shell;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonHomeLoader.CopyExisting;
import org.jvnet.hudson.test.JenkinsRule;
import org.rundeck.api.RundeckClient;
import org.rundeck.api.domain.RundeckExecution;
import org.rundeck.api.domain.RundeckExecution.ExecutionStatus;
import org.rundeck.api.domain.RundeckJob;

import java.io.File;
import java.util.Date;
import java.util.Properties;

/**
 * 
 * TODO.. make this an actual test..
 */
public class RundeckNotifierTest {

    /**
     * Verify a normal build works
     */
    @Rule public JenkinsRule j = new JenkinsRule();


    @Test
    public void testJenkinsClient() throws Exception{
        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new Shell("echo hello"));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");
        String s = FileUtils.readFileToString(build.getLogFile());
        assert(s.contains("+ echo hello"));
    }

    @Test
    public void second() throws Exception {
//        RundeckNotificationBuilder notifier = new RundeckNotificationBuilder("1", true);
//        notifier.getDescriptor().setRundeckInstance(new MockRundeckClient());
//
//        FreeStyleProject project = j.createFreeStyleProject();
//        project.getBuildersList().add(new Shell("echo hello"));
//        project.getBuildersList().add(notifier);
//        FreeStyleBuild build = project.scheduleBuild2(0).get();
//        System.out.println(build.getDisplayName() + " completed");

//        project.setScm(createScm());

//        QueueTaskFuture<FreeStyleBuild> freeStyleBuildQueueTaskFuture = project.scheduleBuild2(0);
//        FreeStyleBuild freeStyleBuild = freeStyleBuildQueueTaskFuture.get();


        // first build
//        FreeStyleBuild build = assertBuildStatusSuccess(project.scheduleBuild(0).get());
//        assertTrue(buildContainsAction(build, RundeckNotifier.RundeckExecutionBuildBadgeAction.class));
//        String s = FileUtils.readFileToString(build.getLogFile());
//        assertTrue(s.contains("Notifying RunDeck..."));
//        assertTrue(s.contains("Notification succeeded !"));

//        addScmCommit(build.getWorkspace(), "commit message");

        // second build
//        build = assertBuildStatusSuccess(project.scheduleBuild2(0).get());
//        assertTrue(buildContainsAction(build, RundeckNotifier.RundeckExecutionBuildBadgeAction.class));
//        s = FileUtils.readFileToString(build.getLogFile());
//        assertTrue(s.contains("Notifying RunDeck..."));
//        assertTrue(s.contains("Notification succeeded !"));
    }

    private SubversionSCM createScm() throws Exception {
        File emptyRepository = new CopyExisting(getClass().getResource("empty-svn-repository.zip")).allocate();
        return new SubversionSCM("file://" + emptyRepository.getPath());
    }

//    private void addScmCommit(FilePath workspace, String commitMessage) throws Exception {
//        SVNClientManager svnm = SubversionSCM.createSvnClientManager(new DefaultSVNAuthenticationManager());
//
//        FilePath newFilePath = workspace.child("new-file");
//        File newFile = new File(newFilePath.getRemote());
//        newFilePath.touch(System.currentTimeMillis());
//        svnm.getWCClient().doAdd(newFile, false, false, false, SVNDepth.INFINITY, false, false);
//        svnm.getCommitClient().doCommit(new File[] { newFile },
//                                        false,
//                                        commitMessage,
//                                        null,
//                                        null,
//                                        false,
//                                        false,
//                                        SVNDepth.EMPTY);
//    }

    /**
     * @param build
     * @param actionClass
     * @return true if the given build contains an action of the given actionClass
     */
    private boolean buildContainsAction(Build<?, ?> build, Class<?> actionClass) {
        for (Action action : build.getActions()) {
            if (actionClass.isInstance(action)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Just a mock {@link org.rundeck.api.RundeckClient} which is always successful
     */
    private static class MockRundeckClient extends RundeckClient {

        private static final long serialVersionUID = 1L;

        public MockRundeckClient() {
            super("http://localhost:4440", "admin", "admin");
        }

        @Override
        public void ping() {
            // successful
        }

        @Override
        public void testCredentials() {
            // successful
        }

        @Override
        public RundeckExecution triggerJob(String jobId, Properties options, Properties nodeFilters) {
            return initExecution(ExecutionStatus.RUNNING);
        }

        @Override
        public RundeckExecution getExecution(Long executionId) {
            return initExecution(ExecutionStatus.SUCCEEDED);
        }

        @Override
        public RundeckJob getJob(String jobId) {
            RundeckJob job = new RundeckJob();
            return job;
        }

        private RundeckExecution initExecution(ExecutionStatus status) {
            RundeckExecution execution = new RundeckExecution();
            execution.setId(1L);
            execution.setUrl("http://localhost:4440/execution/follow/1");
            execution.setStatus(status);
            execution.setStartedAt(new Date(1310159014640L));
            if (ExecutionStatus.SUCCEEDED.equals(status)) {
                Date endedAt = execution.getStartedAt();
                endedAt = DateUtils.addMinutes(endedAt, 3);
                endedAt = DateUtils.addSeconds(endedAt, 27);
                execution.setEndedAt(endedAt);
            }
            return execution;
        }

    }
}
