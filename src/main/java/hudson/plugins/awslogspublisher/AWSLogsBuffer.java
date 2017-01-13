package hudson.plugins.awslogspublisher;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by elifarley on 13/01/17.
 */
public final class AWSLogsBuffer implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(AWSLogsBuffer.class.getName());
    private static final int MAX_LOG_EVENTS_IN_BATCH = 1000;
    private final BufferedReader reader;
    private final AWSLogs awsLogs;
    private final String logGroupName;
    private final String logStreamName;
    private final List<InputLogEvent> list;
    private int currentLogEventCount;
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

        if (msg == null || msg.length() == 0) {
            return;
        }

        list.add(new InputLogEvent().withTimestamp(timestamp).withMessage(msg));

        if (++currentLogEventCount >= MAX_LOG_EVENTS_IN_BATCH) {
            send();
        }

    }

    private int send() {
        PutLogEventsRequest req = new PutLogEventsRequest(logGroupName, logStreamName, list);
        if (this.nextSequenceToken != null) {
            req.setSequenceToken(this.nextSequenceToken);
        }
        this.nextSequenceToken = awsLogs.putLogEvents(req).getNextSequenceToken();
        this.sequencesSent++;
        LOGGER.info("[send] Sent sequence #" + this.sequencesSent + " with " + list.size() + " log events.");
        list.clear();
        currentLogEventCount = 0;
        return this.sequencesSent;
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
