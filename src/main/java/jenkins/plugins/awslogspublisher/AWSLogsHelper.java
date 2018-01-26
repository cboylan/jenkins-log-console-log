package jenkins.plugins.awslogspublisher;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.google.common.base.Strings;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.plugins.timestamper.api.TimestamperAPI;

import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
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

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat result = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss");
            result.setTimeZone(TimeZone.getTimeZone("UTC"));
            return result;
        }
    };

    static String publish(AbstractBuild build, final AWSLogsConfig config, String logStreamName, PrintStream logger) {

        try {
            return pushToAWSLogs(build, getAwsLogsClient(config), config.getLogGroupName(), logStreamName, logger);

        } catch (InterruptedException | IOException e) {
            build.setResult(Result.UNSTABLE);
            return null;
        }
    }

    private static AWSLogs getAwsLogsClient(final AWSLogsConfig config) {
	    ClientConfiguration AWSconfig = new ClientConfiguration()
		    .withThrottledRetries(true)
		    .withConnectionTimeout(6000)
		    // retryCondition, backoffStrategy(baseDelay, maxBackoffTime), maxErrorRetry, honorMaxErrorRetryInClientConfig
		    .withRetryPolicy(new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
					    PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY,
					    10, true));

	    if (!Strings.isNullOrEmpty(config.getAwsAccessKeyId()) && (!Strings.isNullOrEmpty(config.getAwsSecretKey()))) {
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

		    return AWSLogsClientBuilder.standard()
			    .withClientConfiguration(AWSconfig)
			    .withRegion(config.getAwsRegion())
			    .withCredentials(new AWSStaticCredentialsProvider(credentials))
			    .build();
	    }

	    // default use DefaultAWSCredentialsProviderChain()
	    return AWSLogsClientBuilder.standard()
		    .withClientConfiguration(AWSconfig)
		    .build();
    }

    private static String pushToAWSLogs(AbstractBuild build, AWSLogs awsLogsClient, String logGroupName, String logStreamName, PrintStream logger)
            throws IOException, InterruptedException {

        if (Strings.isNullOrEmpty(logStreamName)) {
            logStreamName = getBuildSpec(build);
        }

        {
            String listenerLogMsg = String.format("[AWS Logs] Creating log stream '%s:%s'...", logGroupName, logStreamName);
            LOGGER.info(listenerLogMsg);
        }

        try {
            awsLogsClient.createLogStream(new CreateLogStreamRequest(logGroupName, logStreamName));

        } catch (Exception e) {
            String errorMsg = String.format("[AWS Logs] Unable to create log stream '%s' in log group '%s' (%s)", logStreamName, logGroupName, e.toString());
            LOGGER.warning(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }

        try (AWSLogsBuffer buffer = new AWSLogsBuffer(TimestamperAPI.get().read(build, QUERY), awsLogsClient, logGroupName, logStreamName, logger)) {
            String line;
            int count = 0;
            Long timestamp = System.currentTimeMillis();
            while ((line = buffer.readLine()) != null) {
                // Insert Parameter at top of log
                if(count == 0){
                    if(build.getProject().isParameterized()) {
                        ParametersAction parameters = build.getAction(ParametersAction.class);
                        if (parameters.getParameters() != null && !parameters.getParameters().isEmpty()) {
                            buffer.add("Parameters: ", timestamp);
                            for (ParameterValue action : parameters.getParameters()) {

                                String paramLine = String.format("%s = '%s'", action.getName(),
                                        action.isSensitive() ? "*****" : action.getValue());
                                buffer.add(paramLine, timestamp);
                            }
                        }
                    }
                }

                Matcher matcher = PATTERN.matcher(line);
                if (matcher.find()) {
                    timestamp = DATE_FORMAT.get().parse(line.substring(0, matcher.end())).getTime();
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

        } catch (ParseException e) {
            String errorMsg = String.format("[AWS Logs] Unable to publish build log to '%s:%s' (%s)", logGroupName, logStreamName, e.toString());
            LOGGER.warning(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }

        return logStreamName;

    }

    public static String getBuildSpec(AbstractBuild build) {
        return build.getProject().getName() + "/" + build.getNumber();
    }
}
