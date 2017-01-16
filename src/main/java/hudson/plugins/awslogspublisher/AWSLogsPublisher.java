package hudson.plugins.awslogspublisher;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import org.kohsuke.stapler.DataBoundConstructor;


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

        //if (!writeConsoleLog) return true;

        AWSLogsHelper.publish(build, AWSLogsConfig.get(), listener);
        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public AWSLogsPublisherDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

}
