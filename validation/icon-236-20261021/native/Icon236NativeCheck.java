package seedu.address.ui;

import java.nio.file.Path;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import seedu.address.logic.CanonicalLogicManager;
import seedu.address.logic.CanonicalPeopleSession;
import seedu.address.model.UserPrefs;

/** Manual probe of production window/error-dialog icon wiring from the packaged JAR. */
public final class Icon236NativeCheck {
    public static void main(String[] args) {
        Platform.startup(() -> {
            Path data = Path.of("data", "ponhub.json");
            UiManager ui = new UiManager(new CanonicalLogicManager(
                    new CanonicalPeopleSession(data), new UserPrefs()), data);
            Stage stage = new Stage();
            ui.initializeMainWindow(stage);
            Platform.runLater(() -> {
                ui.showAlertDialogAndWait(stage, AlertType.ERROR, "PonHub icon check",
                        "Application error-dialog icon verification",
                        "Disposable icon check. Close this dialog after inspection.");
                stage.close();
                Platform.exit();
            });
        });
    }
}
