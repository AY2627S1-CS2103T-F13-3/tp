package ponhub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Checks which launch arguments PonHub accepts before any startup path is inspected or written.
 */
public class MainTest {
    @Test
    public void parseLaunchArguments_noArguments_returnsNoNamedParameters() {
        assertEquals(Map.of(), Main.parseLaunchArguments());
    }

    @Test
    public void parseLaunchArguments_dataOption_returnsSelectedPath() {
        assertEquals(Map.of("data", "separate store/ponhub.json"),
                Main.parseLaunchArguments("--data=separate store/ponhub.json"));
        // A blank value is passed on so that the existing path validation reports it.
        assertEquals(Map.of("data", ""), Main.parseLaunchArguments("--data="));
    }

    @Test
    public void parseLaunchArguments_macProcessSerialNumber_ignored() {
        assertEquals(Map.of("data", "other.json"),
                Main.parseLaunchArguments("-psn_0_12345", "--data=other.json"));
    }

    @Test
    public void parseLaunchArguments_unrecognizedArgument_throwsActionableError() {
        // Partitions: option without "=", misspelled option, wrong case, stray positional value and empty text.
        String[][] invalidLaunches = {
            {"--data", "test/other.json"},
            {"--dta=other.json"},
            {"--DATA=other.json"},
            {"other.json"},
            {""},
            {"--data=other.json", "--verbose"},
        };
        for (String[] launch : invalidLaunches) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                    Main.parseLaunchArguments(launch));
            assertTrue(error.getMessage().startsWith("Unrecognized launch argument \""), error.getMessage());
            assertTrue(error.getMessage().contains("--data=PATH"), error.getMessage());
        }
    }

    @Test
    public void parseLaunchArguments_lineBreakInArgument_reportsOneLine() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                Main.parseLaunchArguments("--dat\na=x.json"));
        assertEquals(1, error.getMessage().lines().count(), error.getMessage());
    }
}
