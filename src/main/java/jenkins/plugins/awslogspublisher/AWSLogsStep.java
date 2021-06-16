package jenkins.plugins.awslogspublisher;

import java.io.PrintStream;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.collect.ImmutableSet;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;

public class AWSLogsStep extends Step {

    private static Logger LOGGER = Logger.getLogger(AWSLogsStep.class.getName());

    private String logStreamName;
    private boolean stripANSIColor = false;
    private String logGroupName;
    private AWSLogsConfig config;

    @DataBoundConstructor
    public AWSLogsStep(String logStreamName) throws Descriptor.FormException {

        if (StringUtils.isEmpty(logStreamName)) {
            throw new Descriptor.FormException("cannot be empty", "logStreamName");
        }
        // '*' and ':' are not allowed in stream names
        this.logStreamName = logStreamName.replace('*', '-').replace(':', '-');

        this.config = new AWSLogsConfig();
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ExecutionImpl(this, context);
    }

    public String getLogStreamName() {
        return logStreamName;
    }

    @DataBoundSetter
    public void setLogStreamName(String logStreamName) {
        this.logStreamName = logStreamName;
    }

    public AWSLogsConfig getConfig() {
        return config;
    }

    public void setConfig(AWSLogsConfig config) {
        this.config = config;
    }

    public String getAwsRegion() {
        return this.config.getAwsRegion();
    }

    @DataBoundSetter
    public void setAwsRegion(String awsRegion) {
        this.config.setAwsRegion(awsRegion);
    }

    public String getAwsAccessKeyId() {
        return this.config.getAwsAccessKeyId();
    }

    @DataBoundSetter
    public void setAwsAccessKeyId(String awsAccessKeyId) {
        this.config.setAwsAccessKeyId(awsAccessKeyId);
    }

    public String getAwsSecretKey() {
        return this.config.getAwsSecretKey();
    }

    @DataBoundSetter
    public void setAwsSecretKey(String awsSecretKey) {
        this.config.setAwsSecretKey(awsSecretKey);
    }

    public String getLogGroupName() {
        return this.logGroupName;
    }

    @DataBoundSetter
    public void setLogGroupName(String logGroupName) {
        this.logGroupName = logGroupName;
    }

    @DataBoundSetter
    public void setStripANSIColor(boolean stripANSIColor) {
        this.stripANSIColor = stripANSIColor;
    }

    public boolean getStripANSIColor() {
        return this.stripANSIColor;
    }

    @Extension @Symbol("publish_cloudwatch_logs")
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "publish_cloudwatch_logs";
        }

        @Override
        public String getDisplayName() {
            return "Publish build logs to AWS CloudWatch";
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckLogStreamName(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Needs a value");
            } else {
                return FormValidation.ok();
            }
        }
    }


    /**
     * The execution of {@link AWSLogsStep}.
     */
    public static class ExecutionImpl extends SynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        private transient AWSLogsStep step;

        protected ExecutionImpl(@Nonnull AWSLogsStep step, @Nonnull StepContext context) {
            super(context);
            this.step = step;
        }

        /**
         * Merge global config into our local copy
         * @return
         */
        protected AWSLogsConfig createConfig() {
            AWSLogsConfig globalConfig = AWSLogsConfig.get();
            AWSLogsConfig config = step.getConfig();
            if (config.getAwsRegion() == null) {
                config.setAwsRegion(globalConfig.getAwsRegion());
            }
            if (config.getAwsRegion() == null) {
                config.setAwsRegion(globalConfig.getAwsRegion());
            }
            if (config.getAwsAccessKeyId() == null) {
                config.setAwsAccessKeyId(globalConfig.getAwsAccessKeyId());
            }
            if (config.getAwsSecretKey() == null) {
                config.setAwsSecretKey(globalConfig.getAwsSecretKey());
            }
            config.setLogGroupName(step.getLogGroupName());
            if (config.getLogGroupName() == null) {
                config.setLogGroupName(globalConfig.getLogGroupName());
            }
            return config;
        }

        @Override
        protected Void run() throws Exception {
            Run run = getContext().get(Run.class);
            PrintStream logger = getContext().get(TaskListener.class).getLogger();
            final AWSLogsConfig config = createConfig();

            try {
                AWSLogsHelper.publish(run, config, step.getLogStreamName(), logger, step.getStripANSIColor());
            } catch (Exception ex) {
                LOGGER.warning("failed to publish logs to cloudwatch: " + ex.getMessage());
            }

            return null;
        }
    }

}
