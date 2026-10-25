package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the real loading/playback lifecycle without requiring an audio device or audible output.
 */
public class LogoSoundPlayerTest {
    private static final URL AUDIO = LogoSoundPlayer.class.getResource("/audio/logo-drum.wav");

    @TempDir
    Path directory;

    @Test
    public void play_loadsLazilyAndReusesClipAfterCompletion() {
        TestWorker worker = new TestWorker();
        TestClip clip = new TestClip();
        AtomicInteger devices = new AtomicInteger();
        LogoSoundPlayer player = new LogoSoundPlayer(worker, AUDIO, () -> {
            devices.incrementAndGet();
            return clip.line;
        });
        try {
            assertEquals(0, devices.get());
            player.play();
            assertEquals(0, clip.starts);
            worker.runAll();
            assertEquals(1, clip.opens);
            assertEquals(1, clip.starts);
            assertTrue(clip.running);
            clip.running = false; // Audio device reports natural completion.
            player.play();
            worker.runAll();
            assertEquals(2, clip.starts);
            assertEquals(2, clip.rewinds);
            assertEquals(1, devices.get());
            assertEquals(1, clip.opens);
        } finally {
            player.close();
            worker.runAll();
        }
    }

    @Test
    public void play_repeatedRequestsWhileQueuedLoadingOrPlaying_doNotOverlap() {
        TestWorker worker = new TestWorker();
        TestClip clip = new TestClip();
        LogoSoundPlayer player = new LogoSoundPlayer(worker, AUDIO, () -> clip.line);
        try {
            clip.onOpen = () -> {
                player.play();
                player.play();
            };
            player.play();
            player.play();
            player.play();
            assertEquals(1, worker.tasks.size());
            worker.runAll();
            assertEquals(1, clip.starts);
            player.play();
            worker.runAll();
            assertEquals(1, clip.starts);
            assertEquals(1, clip.rewinds);
        } finally {
            player.close();
            worker.runAll();
        }
    }

    @Test
    public void close_beforeQueuedPlayback_rejectsPlaybackAndShutsDownOnce() {
        TestWorker worker = new TestWorker();
        TestClip clip = new TestClip();
        LogoSoundPlayer player = new LogoSoundPlayer(worker, AUDIO, () -> clip.line);
        player.play();
        player.close();
        player.close();
        player.play();
        assertTrue(worker.isShutdown());
        worker.runAll();
        assertEquals(0, clip.opens);
        assertEquals(0, clip.starts);
        assertEquals(0, clip.closes);
        assertTrue(worker.isTerminated());
    }

    @Test
    public void close_duringLoading_finishesLoadingThenReleasesWithoutPlaying() {
        TestWorker worker = new TestWorker();
        TestClip clip = new TestClip();
        LogoSoundPlayer player = new LogoSoundPlayer(worker, AUDIO, () -> clip.line);
        clip.onOpen = player::close;
        player.play();
        worker.runAll();
        assertEquals(1, clip.opens);
        assertEquals(0, clip.starts);
        assertEquals(1, clip.stops);
        assertEquals(1, clip.closes);
        assertTrue(worker.isTerminated());
    }

    @Test
    public void close_duringPlayback_stopsAndClosesOnceAndIgnoresLaterClicks() {
        TestWorker worker = new TestWorker();
        TestClip clip = new TestClip();
        LogoSoundPlayer player = new LogoSoundPlayer(worker, AUDIO, () -> clip.line);
        player.play();
        worker.runAll();
        player.close();
        assertEquals(0, clip.closes); // Cleanup is queued, not performed on the caller's thread.
        player.play();
        player.close();
        worker.runAll();
        assertFalse(clip.running);
        assertEquals(1, clip.starts);
        assertEquals(1, clip.stops);
        assertEquals(1, clip.closes);
        assertTrue(worker.isTerminated());
    }

    @Test
    public void close_whileRealWorkerIsLoading_returnsWithoutWaitingAndReleasesLine() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor(
                Thread.ofPlatform().daemon().name("logo-audio-test").factory());
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch finishLoading = new CountDownLatch(1);
        TestClip clip = new TestClip();
        Thread caller = Thread.currentThread();
        clip.onOpen = () -> {
            assertFalse(Thread.currentThread() == caller);
            loading.countDown();
            try {
                assertTrue(finishLoading.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        };
        LogoSoundPlayer player = new LogoSoundPlayer(worker, AUDIO, () -> clip.line);
        try {
            player.play();
            assertTrue(loading.await(5, TimeUnit.SECONDS));
            player.close();
            assertTrue(worker.isShutdown());
            assertFalse(worker.isTerminated());
            player.play();
        } finally {
            finishLoading.countDown();
            player.close();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(0, clip.starts);
        assertEquals(1, clip.closes);
    }

    @Test
    public void play_missingResource_disablesAudioWithoutRequestingDevice() {
        assertUnavailable(null);
    }

    @Test
    public void play_unreadableResource_disablesAudioWithoutRequestingDevice() throws Exception {
        assertUnavailable(directory.resolve("missing.wav").toUri().toURL());
    }

    @Test
    public void play_unsupportedResource_disablesAudioWithoutRequestingDevice() throws Exception {
        Path invalid = directory.resolve("invalid.wav");
        Files.writeString(invalid, "This is not a WAV file.");
        assertUnavailable(invalid.toUri().toURL());
    }

    @Test
    public void play_unavailableDevice_disablesFutureAttempts() {
        TestWorker worker = new TestWorker();
        AtomicInteger attempts = new AtomicInteger();
        LogoSoundPlayer player = new LogoSoundPlayer(worker, AUDIO, () -> {
            attempts.incrementAndGet();
            throw new LineUnavailableException("No test audio device");
        });
        player.play();
        worker.runAll();
        player.play();
        worker.runAll();
        assertEquals(1, attempts.get());
        player.close();
        worker.runAll();
    }

    @Test
    public void play_openFailure_closesPartiallyOpenedLineAndDisablesFutureAttempts() {
        TestWorker worker = new TestWorker();
        TestClip clip = new TestClip();
        clip.onOpen = () -> {
            throw new IllegalArgumentException("Unsupported device format");
        };
        LogoSoundPlayer player = new LogoSoundPlayer(worker, AUDIO, () -> clip.line);
        player.play();
        worker.runAll();
        player.play();
        worker.runAll();
        player.close();
        worker.runAll();
        assertEquals(1, clip.opens);
        assertEquals(0, clip.starts);
        assertEquals(1, clip.closes);
    }

    @Test
    public void play_startFailure_releasesLineAndDisablesFutureAttempts() {
        TestWorker worker = new TestWorker();
        TestClip clip = new TestClip();
        clip.onStart = () -> {
            throw new SecurityException("Audio permission denied");
        };
        LogoSoundPlayer player = new LogoSoundPlayer(worker, AUDIO, () -> clip.line);
        player.play();
        worker.runAll();
        player.play();
        worker.runAll();
        player.close();
        worker.runAll();
        assertEquals(1, clip.starts);
        assertEquals(1, clip.stops);
        assertEquals(1, clip.closes);
    }

    private void assertUnavailable(URL resource) {
        TestWorker worker = new TestWorker();
        AtomicInteger attempts = new AtomicInteger();
        LogoSoundPlayer player = new LogoSoundPlayer(worker, resource, () -> {
            attempts.incrementAndGet();
            throw new AssertionError("Invalid resources must not request an audio device");
        });
        player.play();
        worker.runAll();
        player.play();
        worker.runAll();
        player.close();
        worker.runAll();
        assertEquals(0, attempts.get());
        assertTrue(worker.isTerminated());
    }

    /**
     * Explicit task advancement makes loading, queued clicks and shutdown deterministic.
     */
    static class TestWorker extends AbstractExecutorService {
        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private boolean shutdown;

        int taskCount() {
            return tasks.size();
        }

        void runAll() {
            while (!tasks.isEmpty()) {
                tasks.remove().run();
            }
        }

        @Override
        public void execute(Runnable task) {
            if (shutdown) {
                throw new RejectedExecutionException();
            }
            tasks.add(task);
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown();
            List<Runnable> remaining = List.copyOf(tasks);
            tasks.clear();
            return remaining;
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown && tasks.isEmpty();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return isTerminated();
        }
    }

    /**
     * Records sound-line calls while the production loader still decodes the bundled WAV.
     */
    static class TestClip {
        final Clip line = (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(),
                new Class<?>[] {Clip.class}, this::invoke);

        private int opens;
        private int starts;
        private int rewinds;
        private int stops;
        private int closes;
        private boolean running;
        private Runnable onOpen = () -> { };
        private Runnable onStart = () -> { };


        private Object invoke(Object proxy, Method method, Object[] args) throws IOException {
            switch (method.getName()) {
                case "open":
                    opens++;
                    AudioInputStream audio = (AudioInputStream) args[0];
                    assertNotNull(AUDIO);
                    assertEquals(44100, audio.getFormat().getSampleRate());
                    assertEquals(2, audio.getFormat().getChannels());
                    assertTrue(audio.read(new byte[4]) > 0);
                    onOpen.run();
                    return null;
                case "isRunning":
                    return running;
                case "setFramePosition":
                    assertEquals(0, args[0]);
                    rewinds++;
                    return null;
                case "start":
                    starts++;
                    onStart.run();
                    running = true;
                    return null;
                case "stop":
                    stops++;
                    running = false;
                    return null;
                case "close":
                    closes++;
                    return null;
                default:
                    throw new AssertionError("Unexpected sound-line call: " + method.getName());
            }
        }
    }
}
