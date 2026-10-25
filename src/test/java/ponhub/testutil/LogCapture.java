package ponhub.testutil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Captures application records, including throwable/parameter formatting, after application logger setup.
 */
public final class LogCapture implements AutoCloseable {
    private final Logger logger = Logger.getLogger("ponhub");
    private final List<LogRecord> records = new ArrayList<>();
    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    };

    public LogCapture() {
        logger.addHandler(handler);
    }

    public List<LogRecord> getRecords() {
        return List.copyOf(records);
    }

    public String getFormattedText() {
        SimpleFormatter formatter = new SimpleFormatter();
        return records.stream().map(formatter::format).reduce("", String::concat);
    }

    @Override
    public void close() {
        logger.removeHandler(handler);
    }
}
