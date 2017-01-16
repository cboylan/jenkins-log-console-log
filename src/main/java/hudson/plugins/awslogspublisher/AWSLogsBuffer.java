package hudson.plugins.awslogspublisher;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by elifarley on 13/01/17.
 */
public final class AWSLogsBuffer implements Closeable {

    private static final int MAX_LOG_EVENTS_IN_BATCH = 1000;

    private static final Logger LOGGER = Logger.getLogger(AWSLogsBuffer.class.getName());

    // ------------------ Do not change this block ------------------
    private static final int AWS_MAX_LOG_EVENTS_IN_BATCH = 10000;
    public static final int AWS_LOGS_MAX_BATCH_SIZE = 1048576;
    public static final int AWS_LOG_EVENT_OVERHEAD = 26;
    static {
        if (MAX_LOG_EVENTS_IN_BATCH > AWS_MAX_LOG_EVENTS_IN_BATCH) {
            throw new IllegalArgumentException("MAX_LOG_EVENTS_IN_BATCH > AWS_MAX_LOG_EVENTS_IN_BATCH");
        }
    }
    // ------------------ Do not change this block ------------------

    private final BufferedReader reader;
    private final AWSLogs awsLogs;
    private final String logGroupName;
    private final String logStreamName;
    private final List<InputLogEvent> list;
    private int currentLogEventCount;
    private int currentLogEventTotalSize;
    private String nextSequenceToken;
    private int sequencesSent;

    public AWSLogsBuffer(BufferedReader reader, AWSLogs awsLogs, String logGroupName, String logStreamName) {
        this.reader = reader;
        this.awsLogs = awsLogs;
        this.logGroupName = logGroupName;
        this.logStreamName = logStreamName;
        this.list = new ArrayList<>();
    }

    public String readLine() throws IOException {
        return this.reader.readLine();
    }

    public void add(String msg, Long timestamp) {

        if (msg == null || msg.trim().length() == 0) {
            return;
        }

        int newLogEventTotalSize;
        while ((newLogEventTotalSize = currentLogEventTotalSize + computeLogEventSize(msg)) >= AWS_LOGS_MAX_BATCH_SIZE) {

            if (currentLogEventCount != 0) {
                LOGGER.info("Log event batch size would exceed maximum value. Sending buffer now...");
                send();
                continue;
            }

            LOGGER.warning("Log event message will be truncated: " + msg);
            msg = msg.substring(0, AWS_LOGS_MAX_BATCH_SIZE - AWS_LOG_EVENT_OVERHEAD - 6) + "[...]";

        }

        currentLogEventTotalSize += newLogEventTotalSize;

        list.add(new InputLogEvent().withTimestamp(timestamp).withMessage(msg));

        if (++currentLogEventCount >= MAX_LOG_EVENTS_IN_BATCH) {
            LOGGER.info("Log event count would exceed maximum value of " +MAX_LOG_EVENTS_IN_BATCH + ". Sending buffer now...");
            send();
        }

    }

    private int computeLogEventSize(String msg) {
        try {
            return AWS_LOG_EVENT_OVERHEAD + msg.getBytes("UTF-8").length;

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private int send() {
        LOGGER.info(String.format("[%s:%s] Sending sequence #%s with %s log events...", logGroupName, logStreamName, this.sequencesSent + 1, list.size()));
        PutLogEventsRequest req = new PutLogEventsRequest(logGroupName, logStreamName, list);
        if (this.nextSequenceToken != null) {
            req.setSequenceToken(this.nextSequenceToken);
        }
        this.nextSequenceToken = awsLogs.putLogEvents(req).getNextSequenceToken();
        list.clear();
        currentLogEventCount = 0;
        currentLogEventTotalSize = 0;
        return ++this.sequencesSent;
    }

    @Override
    public void close() throws IOException {
        try {
            reader.close();

        } finally {
            if (! this.list.isEmpty()) send();
        }

    }

}
