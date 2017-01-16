package hudson.plugins.awslogspublisher;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.timestamper.api.TimestamperAPI;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by elifarley on 13/01/17.
 */
public final class AWSLogsHelper {

    private static final Logger LOGGER = Logger.getLogger(AWSLogsHelper.class.getName());

    private static final String QUERY = "time=yyyy-MM-dd.HH:mm:ss&timeZone=UTC&appendLog";
    private static final Pattern PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}\\.\\d{2}:\\d{2}:\\d{2}");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss");
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    static void publish(AbstractBuild build, final AWSLogsConfig config, BuildListener listener) {

        try {
            pushToAWSLogs(build, getAwsLogs(config), config.getLogGroupName(), listener);

        } catch (IOException | InterruptedException e) {
            build.setResult(Result.UNSTABLE);
        }
    }

    private static AWSLogs getAwsLogs(final AWSLogsConfig config) {

        AWSCredentials credentials = new AWSCredentials() {

            @Override
            public String getAWSAccessKeyId() {
                return config.getAwsAccessKeyId();
            }

            @Override
            public String getAWSSecretKey() {
                return config.getAwsSecretKey();
            }
        };

        return AWSLogsClientBuilder.standard().
                withRegion(config.getAwsRegion()).
                withCredentials(new StaticCredentialsProvider(credentials)).
                build();
    }

    private static void pushToAWSLogs(AbstractBuild build, AWSLogs awsLogs, String logGroupName, BuildListener listener)
            throws IOException, InterruptedException {

        PrintStream logger = listener.getLogger();

        String logStreamName = build.getProject().getName() + "/" + build.getNumber();
        {
            String listenerLogMsg = String.format("[AWS Logs] Creating log stream '%s:%s'...", logGroupName, logStreamName);
            LOGGER.info(listenerLogMsg);
        }

        try {
            awsLogs.createLogStream(new CreateLogStreamRequest(logGroupName, logStreamName));

        } catch (Exception e) {
            String errorMsg = String.format("Unable to create log stream '%s' in log group '%s' (%s)",logStreamName, logGroupName, e.toString());
            LOGGER.warning(errorMsg);
            e.printStackTrace(listener.error(errorMsg));
            throw new RuntimeException(errorMsg, e);
        }

        try (AWSLogsBuffer buffer = new AWSLogsBuffer(TimestamperAPI.get().read(build, QUERY), awsLogs, logGroupName, logStreamName, logger)) {

            String line;
            int count = 0;
            Long timestamp = System.currentTimeMillis();
            while ((line = buffer.readLine()) != null) {

                Matcher matcher = PATTERN.matcher(line);
                if (matcher.find()) {
                    timestamp = DATE_FORMAT.parse(line.substring(0, matcher.end())).getTime();
                    line = line.substring(matcher.end() + 2);

                } else {
                    if (count > 100) {
                        timestamp = System.currentTimeMillis();
                        count = 0;
                    }
                    line = line.trim();

                }

                buffer.add(line, timestamp);
                count++;

            }

        } catch (Exception e) {
            LOGGER.warning(e.toString());
            e.printStackTrace(listener.error(e.toString()));
            throw new RuntimeException(e);
        }

    }

}
