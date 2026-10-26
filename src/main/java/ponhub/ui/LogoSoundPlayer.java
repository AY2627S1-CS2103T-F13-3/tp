package ponhub.ui;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import ponhub.commons.core.LogsCenter;

/**
 * Plays the optional logo sound off the UI thread, with at most one live audio clip per window.
 * The worker owns the clip; unavailable audio never prevents normal application use.
 */
final class LogoSoundPlayer implements AutoCloseable {
    private static final Logger logger = LogsCenter.getLogger(LogoSoundPlayer.class);
    private static final String RESOURCE = "/audio/logo-drum.wav";

    private final ExecutorService worker;
    private final URL resource;
    private final ClipFactory clipFactory;
    private final AtomicBoolean pending = new AtomicBoolean();
    private Clip clip;
    private boolean unavailable;
    private volatile boolean closed;

    LogoSoundPlayer() {
        this(Executors.newSingleThreadExecutor(
                Thread.ofPlatform().daemon().name("ponhub-logo-audio").factory()),
                LogoSoundPlayer.class.getResource(RESOURCE), AudioSystem::getClip);
    }

    /**
     * Supplies the audio device and worker independently for deterministic, silent tests.
     */
    LogoSoundPlayer(ExecutorService worker, URL resource, ClipFactory clipFactory) {
        this.worker = worker;
        this.resource = resource;
        this.clipFactory = clipFactory;
    }

    /**
     * Requests one playback; extra requests during loading or playback are ignored.
     */
    synchronized void play() {
        if (closed || !pending.compareAndSet(false, true)) {
            return;
        }
        worker.execute(() -> {
            try {
                if (unavailable || closed) {
                    return;
                }
                if (clip == null) {
                    clip = openClip();
                }
                if (!closed && !clip.isRunning()) {
                    clip.setFramePosition(0);
                    clip.start();
                }
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException
                    | IllegalArgumentException | SecurityException failure) {
                unavailable = true;
                releaseClip();
                logger.warning("Optional logo audio unavailable: " + failure.getClass().getSimpleName());
            } finally {
                pending.set(false);
            }
        });
    }

    /**
     * Loads the bundled PCM recording only after an explicit click.
     */
    private Clip openClip() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        if (resource == null) {
            throw new IOException("Bundled logo audio is missing");
        }
        Clip opened = null;
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(resource)) {
            opened = clipFactory.create();
            opened.open(audio);
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException
                | IllegalArgumentException | SecurityException failure) {
            if (opened != null) {
                opened.close();
            }
            throw failure;
        }
        return opened;
    }

    /**
     * Releases the sound line on its owning worker.
     */
    private void releaseClip() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    /**
     * Rejects future clicks and releases audio after any in-flight loading request, without blocking the UI.
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            worker.execute(this::releaseClip);
            worker.shutdown();
        }
    }

    /**
     * Creates a sound line without requiring tests to access an installed audio device.
     */
    @FunctionalInterface
    interface ClipFactory {
        Clip create() throws LineUnavailableException;
    }
}
