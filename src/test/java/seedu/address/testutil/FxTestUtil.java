package seedu.address.testutil;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javafx.application.Platform;

/**
 * Shares one JavaFX toolkit across UI tests in the same test worker.
 */
public final class FxTestUtil {

    private static final int FX_TIMEOUT_SECONDS = 15;

    private static boolean isToolkitReady;

    private FxTestUtil() {
    }

    /**
     * Initializes JavaFX once and keeps it available until the test worker exits.
     */
    public static synchronized void initializeToolkit() throws Exception {
        if (isToolkitReady) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            Platform.setImplicitExit(false);
            isToolkitReady = true;
            return;
        }

        FutureTask<Void> ready = new FutureTask<>(() -> Platform.setImplicitExit(false), null);
        try {
            Platform.startup(ready);
        } catch (IllegalStateException e) {
            // Another UI test may have initialized the toolkit before this helper was used.
            Platform.runLater(ready);
        }
        awaitTask(ready);
        isToolkitReady = true;
    }

    /**
     * Executes assertions on the JavaFX thread with a bounded wait.
     */
    public static void runOnFxThread(Runnable assertions) throws Exception {
        callOnFxThread(() -> {
            assertions.run();
            return null;
        });
    }

    /**
     * Creates or inspects a JavaFX fixture on its application thread with a bounded wait.
     */
    public static <T> T callOnFxThread(Callable<T> action) throws Exception {
        initializeToolkit();
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }

        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return awaitTask(task);
    }

    private static <T> T awaitTask(FutureTask<T> task) throws Exception {
        try {
            return task.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            task.cancel(false);
            throw e;
        }
    }
}
