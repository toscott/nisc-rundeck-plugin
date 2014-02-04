package coop.nisc.plugins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.rundeck.api.MyApiCall;
import org.rundeck.api.MyApiPathBuilder;
import org.rundeck.api.RundeckApiException;
import org.rundeck.api.RundeckClient;
import org.rundeck.api.domain.RundeckExecution;
import org.rundeck.api.parser.ExecutionParser;

import java.io.IOException;

public class RundeckNotifier extends Notifier {
    private static final String APP_TAG_OPTION_NAME = "app_tag";
    private static final String TOKEN_INVALID =
            "The api token provided for rundeck is no longer valid.\n" +
                    "You can generate a new token through the profile page in rundeck.\n" +
                    "You can set the api token on the main jenkins configuration page";
    private static final String PROMOTION_KEY = "Promoting";

    private final String appTag;

    @DataBoundConstructor
    public RundeckNotifier(String appTag) {
        this.appTag = appTag;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        boolean buildSucceeded = false;
        if (build.getResult() == Result.SUCCESS) {
            RundeckDescriptor descriptor = getDescriptor();

            //TODO.. find a better way to denote promotion to staging..
            String s = FileUtils.readFileToString(build.getLogFile());
            RundeckClient rundeck;
            String jobUUID;
            if (s.length() < 5000 && s.contains(PROMOTION_KEY)) {
                listener.getLogger().println("Constructing the staging rundeck client");
                rundeck = descriptor.getStagingRundeck();
                jobUUID = descriptor.getStagingPuppetUUID();
            } else {
                listener.getLogger().println("Constructing the dev rundeck client");
                rundeck = descriptor.getDevRundeck();
                jobUUID = descriptor.getDevPuppetUUID();
            }
            buildSucceeded = notifyRundeck(listener, rundeck, jobUUID, build);
        }
        if (buildSucceeded) {
            listener.getLogger().println("Puppet runs succeeded!");
        }
        return buildSucceeded;
    }

    private boolean notifyRundeck(BuildListener listener, RundeckClient rundeck, String jobUUID, AbstractBuild<?, ?> build) {
        boolean buildSucceeded = false;
        if (rundeck != null) {
            try {
                listener.getLogger().println("Verifying rundeck is running");
                rundeck.ping();
                listener.getLogger().println("Yay, it looks like rundeck is running. Kicking off the puppet runs now..");
                if (appTag == null || appTag.isEmpty()) {
                    listener.getLogger().println("Oops, it looks like you didn't specify an app tag. We don't know where to kick off puppet.");
                } else {
                    //TODO this is a big fat hack because using the normal way ended up with a + instead of %20
                    MyApiPathBuilder apiPath = new MyApiPathBuilder(jobUUID, appTag);
                    listener.getLogger().println("Using api path " + apiPath);
                    RundeckExecution rundeckExecution = new MyApiCall(rundeck)
                            .get(apiPath,
                                    new ExecutionParser("result/executions/execution"));
                    //TODO figure out why this splodes and make it work
//                    build.addAction(new RundeckExecutionBuildBadgeAction(rundeckExecution.getUrl()));
//                    //This is the "correct" way, but I can't figure out why what should be a space, turns into a plus
//                    OptionsBuilder optionsBuilder = new OptionsBuilder();
//                    optionsBuilder.addOption(APP_TAG_OPTION_NAME, appTag);
//                    RundeckExecution rundeckExecution = rundeck.triggerJob(descriptor.getDevPuppetUUID(), options);
                    listener.getLogger().println("Puppet runs have started.. now we just have to wait on them to finish.");
                    while (RundeckExecution.ExecutionStatus.RUNNING.equals(rundeckExecution.getStatus())) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            listener.getLogger().println("Oops, interrupted ! " + e.getMessage());
                        }
                        rundeckExecution = rundeck.getExecution(rundeckExecution.getId());
                    }
                    switch (rundeckExecution.getStatus()) {
                        case SUCCEEDED:
                            buildSucceeded = true;
                            break;
                        default:
                            buildSucceeded = false;
                    }
                }
            } catch (RundeckApiException.RundeckApiTokenException e) {
                listener.getLogger().println(TOKEN_INVALID);
            } catch (RundeckApiException e) {
                listener.getLogger().println("Rundeck is not running or the url is invalid ! " + e.getMessage());
            }
        }
        return buildSucceeded;
    }


    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public RundeckDescriptor getDescriptor() {
        return (RundeckDescriptor) super.getDescriptor();
    }

    public String getAppTag() {
        return appTag;
    }

    @Extension (ordinal = 1000)
    public static final class RundeckDescriptor extends BuildStepDescriptor<Publisher> {

        private RundeckClient devRundeck;
        private String devUrl;
        private String devApiToken;
        private String devPuppetUUID;

        private RundeckClient stagingRundeck;
        private String stagingUrl;
        private String stagingApiToken;
        private String stagingPuppetUUID;


        public RundeckDescriptor() {
            super();
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            devPuppetUUID = json.getString("devRundeckUUID");
            stagingPuppetUUID= json.getString("stagingRundeckUUID");

            devUrl = json.getString("devRundeckUrl");
            devApiToken = json.getString("devRundeckApiToken");
            stagingUrl = json.getString("stagingRundeckUrl");
            stagingApiToken = json.getString("stagingRundeckApiToken");

            try {
                devRundeck = new RundeckClient(devUrl, devApiToken);
            } catch (IllegalArgumentException e) {
                devRundeck = null;
            }
            try {
                stagingRundeck = new RundeckClient(stagingUrl, stagingApiToken);
            } catch (IllegalArgumentException e) {
                stagingRundeck = null;
            }
            save();
            return super.configure(req, json);    //To change body of overridden methods use File | Settings | File Templates.
        }

        public FormValidation doTestDevConnection(@QueryParameter("devRundeckUrl") String rundeckUrl, @QueryParameter("devRundeckApiToken") String apiToken) {
            return testRundeckConnection(rundeckUrl, apiToken);
        }

        public FormValidation doTestStagingConnection(@QueryParameter("stagingRundeckUrl") String rundeckUrl, @QueryParameter("stagingRundeckApiToken") String apiToken) {
            return testRundeckConnection(rundeckUrl, apiToken);
        }

        private FormValidation testRundeckConnection(String url, String apiToken) {
            RundeckClient rundeck = null;
            try {
                rundeck = new RundeckClient(url, apiToken);
            } catch (IllegalArgumentException e) {
                return FormValidation.error("RunDeck configuration is not valid !");
            }
            try {
                rundeck.ping();
            } catch (RundeckApiException e) {
                return FormValidation.error("We couldn't find a live RunDeck instance at %s", rundeck.getUrl());
            }
            try {
                rundeck.testAuth();
            } catch (RundeckApiException.RundeckApiTokenException e) {
                return FormValidation.error("The api token provided is not valid ! ", rundeck.getToken());
            }
            return FormValidation.ok("Your RunDeck instance is alive, and the api token is valid !");
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Puppet runs after build";
        }

        public RundeckClient getDevRundeck() {
            return devRundeck;
        }

        public RundeckClient getStagingRundeck() {
            return stagingRundeck;
        }

        public String getDevPuppetUUID() {
            return devPuppetUUID;
        }

        public String getStagingPuppetUUID() {
            return stagingPuppetUUID;
        }

        public String getDevUrl() {
            return devUrl;
        }

        public String getDevApiToken() {
            return devApiToken;
        }

        public String getStagingUrl() {
            return stagingUrl;
        }

        public String getStagingApiToken() {
            return stagingApiToken;
        }
    }

    /**
     * {@link hudson.model.BuildBadgeAction} used to display a RunDeck icon + a link to the RunDeck execution page, on the Jenkins
     * build history and build result page.
     */
    public static class RundeckExecutionBuildBadgeAction implements BuildBadgeAction {

        private final String executionUrl;

        public RundeckExecutionBuildBadgeAction(String executionUrl) {
            super();
            this.executionUrl = executionUrl;
        }

        public String getDisplayName() {
            return "RunDeck Execution Result";
        }

        public String getIconFileName() {
            return "/plugin/Run_puppet_on_web_nodes/images/rundeck-logo.png";
        }

        public String getUrlName() {
            return executionUrl;
        }

    }
}
