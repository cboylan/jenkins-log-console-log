package hudson.plugins.BuildLogToAWSLogs;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import hudson.Extension;
import hudson.Launcher;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Sample {@link Publisher}.
 * <p>
 * <p>
 * When the user configures the project and enables this publisher,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link BuildLogToAWSLogsPublisher} is created.
 * <p>
 * <p>
 * When a publisher is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author Kohsuke Kawaguchi
 */
public class BuildLogToAWSLogsPublisher extends Recorder {

    private final String fileName;
    private final boolean writeConsoleLog;
    private final boolean blockOnAllOutput;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public BuildLogToAWSLogsPublisher(String fileName, boolean writeConsoleLog, boolean blockOnAllOutput) {
        this.fileName = fileName;
        this.writeConsoleLog = writeConsoleLog;
        //Currently the blocking on other output is broken.
        this.blockOnAllOutput = blockOnAllOutput;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getFileName() {
        return fileName;
    }

    public boolean getWriteConsoleLog() {
        return writeConsoleLog;
    }

    public boolean getBlockOnAllOutput() {
        return blockOnAllOutput;
    }

    /**
     * Actually publish the Console Logs to the workspace.
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {

        if (!writeConsoleLog) return true;

        AWSLogs awsLogs = AWSLogsClientBuilder.standard().build();


        try {
            writeLogFile(build, awsLogs, blockOnAllOutput);

        } catch (IOException | InterruptedException e) {
            build.setResult(Result.UNSTABLE);
        }
        return true;
    }

    private void writeLogFile(AbstractBuild build, AWSLogs awsLogs, boolean block)
            throws IOException, InterruptedException {

        AnnotatedLargeText logText = build.getLogText();
        BufferedReader reader = new BufferedReader(logText.readAll());
        String line;

        int count = 0;

        List<InputLogEvent> list = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            Long timestamp = new Date().getTime();
            list.add(new InputLogEvent().withMessage(line).withTimestamp(timestamp));
        }

        PutLogEventsRequest req = new PutLogEventsRequest("/jenkins/jobs", build.getId() + "/" + build.getNumber(), list);
        awsLogs.putLogEvents(req);

    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link BuildLogToAWSLogsPublisher}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * <p>
     * <p>
     * See <tt>src/main/resources/hudson/plugins/BuildLogToAWSLogs/BuildLogToAWSLogsPublisher/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Write build log to AWS CloudWatch Logs";
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckFileName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set an output file name");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

    }
}
