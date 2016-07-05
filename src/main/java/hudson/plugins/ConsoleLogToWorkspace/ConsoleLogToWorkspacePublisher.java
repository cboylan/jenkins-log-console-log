package hudson.plugins.ConsoleLogToWorkspace;

import hudson.Extension;
import hudson.FilePath;
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

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import javax.servlet.ServletException;

/**
 * Sample {@link Publisher}.
 *
 * <p>
 * When the user configures the project and enables this publisher,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link ConsoleLogToWorkspacePublisher} is created.
 *
 * <p>
 * When a publisher is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author Kohsuke Kawaguchi
 */
public class ConsoleLogToWorkspacePublisher extends Recorder {

    private final String fileName;
    private final boolean writeConsoleLog;
    private final boolean blockOnAllOutput;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public ConsoleLogToWorkspacePublisher(String fileName, boolean writeConsoleLog, boolean blockOnAllOutput) {
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
        final FilePath workspace;
        final FilePath outFile;
        final File f;
        final OutputStream os;
        try {
            if (writeConsoleLog) {
                workspace = build.getWorkspace();
                outFile = workspace.child(fileName);
                os = outFile.write();
                writeLogFile(build, os, blockOnAllOutput);
                os.close();
            }
        } catch (IOException e) {
            build.setResult(Result.UNSTABLE);
        } catch (InterruptedException e) {
            build.setResult(Result.UNSTABLE);
        }
        return true;
    }

    private void writeLogFile(AbstractBuild build, OutputStream out, boolean block)
            throws IOException, InterruptedException {
        long pos = 0;
        long prevPos = pos;
        AnnotatedLargeText logText;
        logText = build.getLogText();
        do {
            prevPos = pos;
            pos = logText.writeLogTo(pos, out);
            if (prevPos >= pos) { // Nothing new has been written
                break;
            }
            Thread.sleep(1000);
        } while(true);
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link ConsoleLogToWorkspacePublisher}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/ConsoleLogToWorkspace/ConsoleLogToWorkspacePublisher/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Write Console Log to Workspace";
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
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
