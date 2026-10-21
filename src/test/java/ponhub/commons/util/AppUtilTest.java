package ponhub.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static ponhub.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import javafx.scene.image.Image;

public class AppUtilTest {

    @Test
    public void getImage_existingApplicationIcon_decodesImage() {
        Image icon = AppUtil.getImage("/images/ponhub_icon.png");
        assertFalse(icon.isError());
        assertEquals(64, icon.getWidth());
        assertEquals(64, icon.getHeight());
    }

    @Test
    public void getImage_nullGiven_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> AppUtil.getImage(null));
    }

    @Test
    public void checkArgument_true_nothingHappens() {
        AppUtil.checkArgument(true);
        AppUtil.checkArgument(true, "");
    }

    @Test
    public void checkArgument_falseWithoutErrorMessage_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> AppUtil.checkArgument(false));
    }

    @Test
    public void checkArgument_falseWithErrorMessage_throwsIllegalArgumentException() {
        String errorMessage = "error message";
        assertThrows(IllegalArgumentException.class, errorMessage, () -> AppUtil.checkArgument(false, errorMessage));
    }
}
