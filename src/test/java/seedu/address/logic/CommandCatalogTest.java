package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.parser.AddressBookParser;

public class CommandCatalogTest {
    @Test
    public void getCommandWords_onlyActiveCommandsAndImmutable() {
        assertEquals(List.of("add", "delete", "list", "help", "exit"), CommandCatalog.getCommandWords());
        assertThrows(UnsupportedOperationException.class, () -> CommandCatalog.getCommandWords().add("clear"));
        assertTrue(CommandCatalog.getUsage("clear").isEmpty());
        assertTrue(CommandCatalog.getUsage("ADD").isEmpty());
    }

    @Test
    public void registeredGuidance_examplesParseThroughActualRouter() throws Exception {
        AddressBookParser parser = new AddressBookParser();
        for (String word : CommandCatalog.getCommandWords()) {
            String usage = CommandCatalog.getUsage(word).orElseThrow();
            assertTrue(CommandCatalog.getOverview().contains("\n" + word + " - "));
            assertFalse(usage.isBlank());
            assertEquals(new HelpCommand(word), parser.parseCommand("help " + word));
            String example = word.equals("help") ? "help add"
                    : usage.substring(usage.indexOf("Example: ") + "Example: ".length());
            assertTrue(parser.parseCommand(example) != null);
        }
    }
}
