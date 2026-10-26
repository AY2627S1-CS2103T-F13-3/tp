package ponhub.ui;

import java.awt.AWTError;
import java.awt.EventQueue;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
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
        try {
            if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac")) {
                return;
            }
            EventQueue.invokeLater(() -> apply(resourcePath));
        } catch (RuntimeException | LinkageError | AWTError e) {
            logger.log(Level.FINE, "Optional Dock icon unavailable", e);
        }
    }

    private static void apply(String resourcePath) {
        try {
            if (!Taskbar.isTaskbarSupported()) {
                return;
            }
            Taskbar taskbar = Taskbar.getTaskbar();
            if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                return;
            }
            URL resource = DockIcon.class.getResource(resourcePath);
            if (resource == null) {
                logger.fine("Optional Dock icon resource unavailable");
                return;
            }
            BufferedImage image = ImageIO.read(resource);
            if (image != null) {
                taskbar.setIconImage(image);
            }
        } catch (IOException | RuntimeException | LinkageError | AWTError e) {
            logger.log(Level.FINE, "Optional Dock icon unavailable", e);
        }
    }
}
