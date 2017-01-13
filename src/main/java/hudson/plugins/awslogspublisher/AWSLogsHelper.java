package hudson.plugins.awslogspublisher;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.timestamper.api.TimestamperAPI;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by elifarley on 13/01/17.
 */
public final class AWSLogsHelper {

    private static final String QUERY = "time=yyyy-MM-dd.HH:mm:ss&timeZone=UTC&appendLog";
    private static final Pattern PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}\\.\\d{2}:\\d{2}:\\d{2}");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss");
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    static void publish(AbstractBuild build, final AWSLogsConfig config) {


        try {
            pushToAWSLogs(build, getAwsLogs(config), config.getLogGroupName());

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

    private static void pushToAWSLogs(AbstractBuild build, AWSLogs awsLogs, String logGroupName)
            throws IOException, InterruptedException {

        String logStreamName = build.getProject().getName() + "/" + build.getNumber();
        awsLogs.createLogStream(new CreateLogStreamRequest(logGroupName, logStreamName));

        try (BufferedReader reader = TimestamperAPI.get().read(build, QUERY)) {

            List<InputLogEvent> list = new ArrayList<>();
            String line;
            // TODO Max 10k lines
            int count = 0;
            Long timestamp = System.currentTimeMillis();
            while ((line = reader.readLine()) != null) {


                Matcher matcher = PATTERN.matcher(line);
                if (matcher.find()) {
                    timestamp = DATE_FORMAT.parse(line.substring(0, matcher.end())).getTime();
                    line = line.substring(matcher.end() + 2);

                } else {
                    if (count > 50) {
                        timestamp = System.currentTimeMillis();
                    }
                    line = line.trim();

                }

                list.add(new InputLogEvent().withTimestamp(timestamp).withMessage(line));
                count++;

            }

            PutLogEventsResult logEventsResult = awsLogs.putLogEvents(new PutLogEventsRequest(logGroupName, logStreamName, list));
            String nextSequenceToken = logEventsResult.getNextSequenceToken();

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }

}
