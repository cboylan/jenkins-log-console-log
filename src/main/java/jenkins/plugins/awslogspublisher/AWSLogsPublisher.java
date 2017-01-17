package jenkins.plugins.awslogspublisher;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.ProminentProjectAction;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;


/**
 * When a publisher is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author Elifarley Cruz
 */
public class AWSLogsPublisher extends Recorder {

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final AWSLogsPublisherDescriptor DESCRIPTOR = new AWSLogsPublisherDescriptor(AWSLogsPublisher.class);

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public AWSLogsPublisher() {
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * Actually publish the Console Logs to the workspace.
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        showStreamInfo(build, listener);
        return true;
    }

    private static void showStreamInfo(AbstractBuild build, BuildListener listener) {

        final AWSLogsConfig config = AWSLogsConfig.get();
        PrintStream logger = listener.getLogger();
        final String logStreamName = AWSLogsHelper.publish(build, config, logger);
        if (logStreamName == null) {
            return;
        }

        final String url = String.format("https://console.aws.amazon.com/cloudwatch/home?region=%s#logEventViewer:group=%s;stream=%s",
                config.getAwsRegion(), config.getLogGroupName(), logStreamName);

        addProminentAWSLogsLink(build, url);

        try {
            logger.print("[AWS Logs] Build log published at ");
            listener.hyperlink(url, String.format("%s:%s", config.getLogGroupName(), logStreamName));
            logger.println();

        } catch (IOException e) {
            throw new RuntimeException("[AWS Logs] Unable to write url " + url, e);
        }
    }

    private static void addProminentAWSLogsLink(AbstractBuild build, final String url) {
        build.addAction(new ProminentProjectAction() {
            @Override
            public String getIconFileName() {
                return "notepad.png";
            }

            @Override
            public String getDisplayName() {
                return "AWS CloudWatch Logs";
            }

            @Override
            public String getUrlName() {
                return url;
            }
        });
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public AWSLogsPublisherDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

}
