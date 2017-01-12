package hudson.plugins.awslogspublisher;

/*
 * The MIT License
 *
 * Copyright (c) 2016 Elifarley Cruz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.Descriptor;
import jenkins.YesNoMaybe;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;

/**
 * Global configuration for the AWS Logs Publisher plug-in, as shown on the Jenkins
 * Configure System page.
 *
 * @author Elifarley Cruz
 */
@Extension(dynamicLoadable = YesNoMaybe.YES)
public final class AWSLogsConfig extends GlobalConfiguration {

    public static final String DEFAULT_LOG_GROUP = "/jenkins/jobs";

    /**
     * @return the AWS Logs Publisher configuration, or {@code null} if Jenkins has been
     * shut down
     */
    public static AWSLogsConfig get() {

        Jenkins jenkins = Jenkins.getInstance();

        if (jenkins == null) {
            return null;
        }

        AWSLogsConfig config = jenkins
                .getDescriptorByType(AWSLogsConfig.class);

        return config;

    }

    @CheckForNull
    private String awsAccessKeyId;

    @CheckForNull
    private String awsSecretKey;

    @CheckForNull
    private String awsRegion;

    @CheckForNull
    private String logGroupName;

    public AWSLogsConfig() {
        load();
    }

    public String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }

    public void setAwsAccessKeyId(@CheckForNull String awsAccessKeyId) {
        this.awsAccessKeyId = awsAccessKeyId;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(@CheckForNull String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(@CheckForNull String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public String getLogGroupName() {
        if (Strings.isNullOrEmpty(logGroupName)) return AWSLogsConfig.DEFAULT_LOG_GROUP;
        return logGroupName;
    }

    public void setLogGroupName(@CheckForNull String logGroupName) {
        this.logGroupName = logGroupName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject json)
            throws Descriptor.FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }
}