package jenkins.plugins.awslogspublisher;

import java.io.PrintStream;
import java.io.Serializable;
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
import jenkins.model.Jenkins;

public class AWSLogsStep extends Step {

    private static Logger LOGGER = Logger.getLogger(AWSLogsStep.class.getName());

    private String logStreamName;
    private AWSLogsConfig config;

    @DataBoundConstructor
    public AWSLogsStep(String logStreamName) throws Descriptor.FormException {

        if (StringUtils.isEmpty(logStreamName)) {
            throw new Descriptor.FormException("cannot be empty", "logStreamName");
        }
        // '*' and ':' are not allowed in stream names
        this.logStreamName = logStreamName.replace('*', '-').replace(':', '-');

        this.config = AWSLogsConfig.get();
        if (this.config == null) {
            this.config = new AWSLogsConfig();
        }
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ExecutionImpl(this, context);
    }

    public String getLogStreamName() {
        return logStreamName;
    }

    public AWSLogsConfig getConfig() {
        return config;
    }

    @DataBoundSetter
    public void setAwsRegion(String awsRegion) {
        this.config.setAwsRegion(awsRegion);
    }

    @DataBoundSetter
    public void setAwsAccessKeyId(String awsAccessKeyId) {
        this.config.setAwsAccessKeyId(awsAccessKeyId);
    }

    @DataBoundSetter
    public void setAwsSecretKey(String awsSecretKey) {
        this.config.setAwsSecretKey(awsSecretKey);
    }

    @DataBoundSetter
    public void setLogGroupName(String logGroupName) {
        this.config.setLogGroupName(logGroupName);
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
        public FormValidation doCheckFile(@QueryParameter String value) {
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

        @Override
        protected Void run() throws Exception {
            Run run = getContext().get(Run.class);
            PrintStream logger = getContext().get(TaskListener.class).getLogger();
            final AWSLogsConfig config = step.getConfig();

            try {
                AWSLogsHelper.publish(run, config, step.getLogStreamName(), logger);
            } catch (Exception ex) {
                LOGGER.warning("failed to publish logs to cloudwatch: " + ex.getMessage());
            }

            return null;
        }
    }

}
