package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import seedu.address.commons.core.LogsCenter;

public class UiManagerTest {

    private final RecordingLogHandler logHandler = new RecordingLogHandler();
    private UiManagerStub uiManager;
    private Logger logger;

    @BeforeEach
    public void setUp() {
        uiManager = new UiManagerStub();
        logger = LogsCenter.getLogger(UiManager.class);
        logger.addHandler(logHandler);
    }

    @AfterEach
    public void tearDown() {
        logger.removeHandler(logHandler);
    }

    @Test
    public void start_protectedSession_showsRecoveryWarningWithoutShutdown() {
        uiManager = new UiManagerStub(Optional.of("Preserve the original and restart after recovery."));
        uiManager.start(null);
        assertEquals(List.of("initialize", "alert"), uiManager.events);
        assertEquals(AlertType.WARNING, uiManager.alertType);
        assertEquals("Data file protected", uiManager.alertTitle);
        assertEquals("Preserve the original and restart after recovery.", uiManager.alertContent);
    }

    @Test
    public void start_success_doesNotShowAlertOrShutdown() {
        uiManager.start(null);

        assertEquals(List.of("initialize"), uiManager.events);
        assertEquals(List.of(), logHandler.severeRecords);
    }

    @Test
    public void start_initializationException_showsAlertAndShutsDown() {
        RuntimeException failure = new IllegalStateException("Unable to initialize the window");
        uiManager.initializationException = failure;

        uiManager.start(null);

        assertEquals(List.of("initialize", "alert", "shutdown"), uiManager.events);
        assertFatalAlert(failure);
        assertEquals(1, logHandler.severeRecords.size());
        assertSame(failure, logHandler.severeRecords.get(0).getThrown());
    }

    @Test
    public void start_initializationError_showsAlertAndShutsDown() {
        Error failure = new AssertionError("Unable to load the window");
        uiManager.initializationError = failure;

        uiManager.start(null);

        assertEquals(List.of("initialize", "alert", "shutdown"), uiManager.events);
        assertFatalAlert(failure);
        assertEquals(1, logHandler.severeRecords.size());
        assertSame(failure, logHandler.severeRecords.get(0).getThrown());
    }

    @Test
    public void start_alertError_logsBothFailuresAndShutsDown() {
        RuntimeException initializationFailure = new IllegalStateException("Unable to initialize the window");
        Error alertFailure = new AssertionError("Unable to show the alert");
        uiManager.initializationException = initializationFailure;
        uiManager.alertFailure = alertFailure;

        uiManager.start(null);

        assertEquals(List.of("initialize", "alert", "shutdown"), uiManager.events);
        assertFatalAlert(initializationFailure);
        assertEquals(2, logHandler.severeRecords.size());
        assertSame(initializationFailure, logHandler.severeRecords.get(0).getThrown());
        assertSame(alertFailure, logHandler.severeRecords.get(1).getThrown());
    }

    private void assertFatalAlert(Throwable failure) {
        assertNull(uiManager.alertOwner);
        assertEquals(AlertType.ERROR, uiManager.alertType);
        assertEquals("Fatal error during initializing", uiManager.alertTitle);
        assertEquals(failure.getMessage(), uiManager.alertHeader);
        assertEquals(failure.toString(), uiManager.alertContent);
    }

    /**
     * Records startup actions without creating JavaFX controls or exiting the test process.
     */
    private static class UiManagerStub extends UiManager {

        private final List<String> events = new ArrayList<>();
        private RuntimeException initializationException;
        private Error initializationError;
        private Error alertFailure;
        private Stage alertOwner;
        private AlertType alertType;
        private String alertTitle;
        private String alertHeader;
        private String alertContent;

        UiManagerStub() {
            this(Optional.empty());
        }

        UiManagerStub(Optional<String> error) {
            super(null, null, error);
        }

        @Override
        void initializeMainWindow(Stage primaryStage) {
            events.add("initialize");
            if (initializationException != null) {
                throw initializationException;
            }
            if (initializationError != null) {
                throw initializationError;
            }
        }

        @Override
        void showAlertDialogAndWait(Stage owner, AlertType type, String title, String headerText,
                String contentText) {
            events.add("alert");
            alertOwner = owner;
            alertType = type;
            alertTitle = title;
            alertHeader = headerText;
            alertContent = contentText;
            if (alertFailure != null) {
                throw alertFailure;
            }
        }

        @Override
        void shutdown() {
            events.add("shutdown");
        }
    }

    /**
     * Retains severe log records so tests can check the original failure objects.
     */
    private static class RecordingLogHandler extends Handler {

        private final List<LogRecord> severeRecords = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().equals(Level.SEVERE)) {
                severeRecords.add(record);
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }
}
