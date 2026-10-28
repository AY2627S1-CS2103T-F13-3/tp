package ponhub.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.AWTError;
import java.awt.EventQueue;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks optional branding without changing the native taskbar or depending on installed desktop support.
 */
public class DockIconTest {
    private static final String ICON = "/images/ponhub_icon.png";

    @TempDir
    Path directory;

    @Test
    public void install_defaultBindingsWithMissingResource_remainsNonfatal() throws Exception {
        assertDoesNotThrow(() -> DockIcon.install("/missing-dock-icon.png"));
        // Drain the real scheduler on Mac; this requests no icon change on any platform.
        if (System.getProperty("os.name", "").startsWith("Mac")) {
            EventQueue.invokeAndWait(() -> { });
        }
    }

    @Test
    public void install_nonMac_skipsSchedulerAndNativeOperations() {
        for (String os : List.of("Windows 11", "Linux", "")) {
            Harness harness = new Harness();
            harness.install(os);
            assertTrue(harness.queue.isEmpty());
            assertTrue(harness.calls.isEmpty());
        }
    }

    @Test
    public void install_mac_schedulesAndDecodesBundledArtwork() throws Exception {
        Harness harness = new Harness();
        harness.install("mAc OS X");
        assertEquals(1, harness.queue.size());
        assertTrue(harness.calls.isEmpty());
        harness.drain();
        assertEquals(List.of("supported", "taskbar", "feature", "resource", "set"), harness.calls);
        BufferedImage expected = ImageIO.read(DockIconTest.class.getResource(ICON));
        assertNotNull(harness.image);
        assertEquals(expected.getWidth(), harness.image.getWidth());
        assertEquals(expected.getHeight(), harness.image.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), harness.image.getRGB(x, y));
            }
        }
    }

    @Test
    public void apply_taskbarUnsupported_stopsBeforeAcquisition() {
        Harness harness = new Harness();
        harness.supported = () -> false;
        harness.run();
        assertEquals(List.of("supported"), harness.calls);
    }

    @Test
    public void apply_iconFeatureUnsupported_stopsBeforeResourceLoading() {
        Harness harness = new Harness();
        harness.featureSupported = false;
        harness.run();
        assertEquals(List.of("supported", "taskbar", "feature"), harness.calls);
    }

    @Test
    public void apply_missingOrUndecodableArtwork_doesNotSetIcon() throws Exception {
        Path invalid = directory.resolve("not-an-image.txt");
        Files.writeString(invalid, "not an image");
        for (URL resource : new URL[] {null, invalid.toUri().toURL()}) {
            Harness harness = new Harness();
            harness.resources = path -> resource;
            harness.run();
            assertFalse(harness.calls.contains("set"));
        }
    }

    @Test
    public void apply_unreadableArtwork_isNonfatal() throws Exception {
        URL missing = directory.resolve("missing.png").toUri().toURL();
        Harness harness = new Harness();
        harness.resources = path -> missing;
        harness.run();
        assertFalse(harness.calls.contains("set"));
    }

    @Test
    public void install_schedulerFailure_isNonfatal() {
        for (Throwable failure : failures()) {
            assertDoesNotThrow(() -> DockIcon.install("Mac OS X", ICON,
                    task -> raise(failure), null, null));
        }
    }

    @Test
    public void apply_supportOrAcquisitionFailure_isNonfatal() {
        for (Throwable failure : failures()) {
            Harness unsupported = new Harness();
            unsupported.supported = () -> {
                raise(failure);
                return true;
            };
            unsupported.run();
            assertEquals(List.of("supported"), unsupported.calls);
            Harness unavailable = new Harness();
            unavailable.acquire = () -> {
                raise(failure);
                return unavailable.taskbar;
            };
            unavailable.run();
            assertEquals(List.of("supported", "taskbar"), unavailable.calls);
        }
    }

    @Test
    public void apply_setIconFailure_isNonfatal() {
        for (Throwable failure : failures()) {
            Harness harness = new Harness();
            harness.setFailure = failure;
            harness.run();
            assertEquals(List.of("supported", "taskbar", "feature", "resource", "set"), harness.calls);
        }
    }

    private static List<Throwable> failures() {
        return List.of(new UnsupportedOperationException("unsupported"), new SecurityException("denied"),
                new UnsatisfiedLinkError("native unavailable"), new AWTError("desktop unavailable"));
    }

    private static void raise(Throwable failure) {
        if (failure instanceof RuntimeException runtime) {
            throw runtime;
        }
        throw (Error) failure;
    }

    /**
     * Records call order and delays execution independently from any native desktop.
     */
    private static class Harness {
        private final Object taskbar = new Object();
        private final Queue<Runnable> queue = new ArrayDeque<>();
        private final List<String> calls = new ArrayList<>();
        private BooleanSupplier supported = () -> true;
        private Supplier<Object> acquire = () -> taskbar;
        private boolean featureSupported = true;
        private Function<String, URL> resources = DockIconTest.class::getResource;
        private Throwable setFailure;
        private BufferedImage image;

        void install(String os) {
            DockIcon.install(os, ICON, queue::add, new DockIcon.DesktopAccess<>(() -> {
                calls.add("supported");
                return supported.getAsBoolean();
            }, () -> {
                calls.add("taskbar");
                return acquire.get();
            }, (target, feature) -> {
                assertSame(taskbar, target);
                assertEquals(Taskbar.Feature.ICON_IMAGE, feature);
                calls.add("feature");
                return featureSupported;
            }, (target, artwork) -> {
                assertSame(taskbar, target);
                calls.add("set");
                if (setFailure != null) {
                    raise(setFailure);
                }
                image = artwork;
            }), path -> {
                assertEquals(ICON, path);
                calls.add("resource");
                return resources.apply(path);
            });
        }

        void drain() {
            while (!queue.isEmpty()) {
                assertDoesNotThrow(queue.remove()::run);
            }
        }

        void run() {
            install("Mac OS X");
            drain();
        }
    }
}
