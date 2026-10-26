package ponhub.ui;

import java.awt.AWTError;
import java.awt.EventQueue;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

import ponhub.commons.core.LogsCenter;

/** Applies the bundled artwork to the running macOS application when supported. */
final class DockIcon {
    private static final Logger logger = LogsCenter.getLogger(DockIcon.class);

    private DockIcon() {
    }

    /**
     * Schedules optional Dock branding on the AWT event thread. Unsupported platforms and
     * unavailable artwork leave the platform's default icon without preventing startup.
     */
    static void install(String resourcePath) {
        install(System.getProperty("os.name", ""), resourcePath, EventQueue::invokeLater,
                new DesktopAccess<>(Taskbar::isTaskbarSupported, Taskbar::getTaskbar,
                        Taskbar::isSupported, Taskbar::setIconImage), DockIcon.class::getResource);
    }

    /** Supplies native boundaries independently so guards can be checked without altering the real Dock. */
    static <T> void install(String osName, String resourcePath, Consumer<Runnable> schedule,
            DesktopAccess<T> desktop, Function<String, URL> resources) {
        try {
            if (!osName.toLowerCase(Locale.ROOT).startsWith("mac")) {
                return;
            }
            schedule.accept(() -> apply(resourcePath, desktop, resources));
        } catch (RuntimeException | LinkageError | AWTError e) {
            logger.log(Level.FINE, "Optional Dock icon unavailable", e);
        }
    }

    private static <T> void apply(String resourcePath, DesktopAccess<T> desktop, Function<String, URL> resources) {
        try {
            if (!desktop.supported().getAsBoolean()) {
                return;
            }
            T taskbar = desktop.taskbar().get();
            if (!desktop.featureSupported().test(taskbar, Taskbar.Feature.ICON_IMAGE)) {
                return;
            }
            URL resource = resources.apply(resourcePath);
            if (resource == null) {
                logger.fine("Optional Dock icon resource unavailable");
                return;
            }
            BufferedImage image = ImageIO.read(resource);
            if (image != null) {
                desktop.setIcon().accept(taskbar, image);
            }
        } catch (IOException | RuntimeException | LinkageError | AWTError e) {
            logger.log(Level.FINE, "Optional Dock icon unavailable", e);
        }
    }

    /** Direct standard-library bindings in production; controlled native operations in tests. */
    record DesktopAccess<T>(BooleanSupplier supported, Supplier<T> taskbar,
            BiPredicate<T, Taskbar.Feature> featureSupported, BiConsumer<T, BufferedImage> setIcon) {
    }
}
