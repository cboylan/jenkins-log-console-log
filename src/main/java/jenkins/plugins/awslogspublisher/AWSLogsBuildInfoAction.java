package jenkins.plugins.awslogspublisher;

import hudson.model.ProminentProjectAction;

/**
 * Created by elifarley on 18/01/17.
 */
public class AWSLogsBuildInfoAction implements ProminentProjectAction {

    private final String groupName;
    private final String streamName;
    private final String url;

    public AWSLogsBuildInfoAction(String groupName, String streamName, String url) {
        this.groupName = groupName;
        this.streamName = streamName;
        this.url = url;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getStreamName() {
        return streamName;
    }

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

}
