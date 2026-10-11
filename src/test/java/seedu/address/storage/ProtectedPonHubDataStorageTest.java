package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.CanonicalCommandExecutor;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.PeopleView;
import seedu.address.model.PonHubData;
import seedu.address.model.PonHubDataState;
import seedu.address.model.util.PeopleSampleDataUtil;

public class ProtectedPonHubDataStorageTest {
    @TempDir
    public Path directory;

    @Test
    public void load_missingFile_startsFreshWithoutWritingAndAllowsSave() throws Exception {
        Path path = directory.resolve("new folder/canonical.json");
        ProtectedPonHubDataStorage storage = new ProtectedPonHubDataStorage(path);
        assertThrows(IOException.class, () -> storage.saveData(new PonHubData().exportState()));
        PonHubDataState state = storage.load();
        assertEquals(ProtectedPonHubDataStorage.LoadStatus.MISSING, storage.getLoadStatus());
        assertFalse(Files.exists(path));
        assertTrue(storage.getDataLoadError().isEmpty());
        storage.saveData(state);
        assertEquals(state, new ProtectedPonHubDataStorage(path).load());
    }

    @Test
    public void load_rejectedFiles_remainIntactAcrossCommandsAndDirectWrites() throws Exception {
        String valid = JsonPonHubDataCodec.encode(new PonHubData().exportState());
        List<String> rejected = List.of("{", "null", "[]", "{\"persons\":[]}",
                "{\"schemaVersion\":99,\"persons\":[]}",
                valid.replace("\"people\" : [ ]", "\"people\" : [null]"),
                valid.replace("\"STUDENT\" : 0", "\"STUDENT\" : -1"),
                valid.replace("\"lessons\" : [ ]", "\"lessons\" : [{\"id\":\"L1\"}]"),
                valid.replace("\"attendance\" : [ ]", "\"attendance\" : [{\"date\":\"2026-02-30\"}]"));
        for (int i = 0; i < rejected.size(); i++) {
            Path path = directory.resolve("rejected" + i + ".json");
            Files.writeString(path, rejected.get(i));
            byte[] original = Files.readAllBytes(path);
            ProtectedPonHubDataStorage storage = new ProtectedPonHubDataStorage(path);
            assertThrows(DataLoadingException.class, storage::load, rejected.get(i));
            assertTrue(storage.getDataLoadError().orElseThrow().contains(path.toAbsolutePath().toString()));
            assertTrue(storage.getDataLoadError().orElseThrow().contains("restart"));
            PonHubData live = new PonHubData();
            CanonicalCommandExecutor executor = new CanonicalCommandExecutor(new PeopleView(live),
                    storage::saveData, storage::getDataLoadError);
            for (String command : List.of("help", "list", "exit")) {
                CommandResult result = executor.execute((staged, view) ->
                        new CommandResult(command, command.equals("help"), command.equals("exit")));
                assertEquals(command.equals("exit"), result.isExit());
                assertArrayEquals(original, Files.readAllBytes(path));
            }
            assertThrows(CommandException.class, () -> executor.execute((staged, view) -> {
                var before = staged.exportState();
                staged.resetData(new PonHubDataState(before.people(), List.of(), List.of(), 1));
                return new CommandResult("Rejected change");
            }));
            assertEquals(new PonHubData().exportState(), live.exportState());
            assertThrows(IOException.class, () -> storage.saveData(live.exportState()));
            assertArrayEquals(original, Files.readAllBytes(path));
        }
    }

    @Test
    public void load_classifiesLegacyUnsupportedCorruptAndUnreadable() throws Exception {
        Path path = directory.resolve("data.json");
        for (var entry : java.util.Map.of("{\"persons\":[]}", ProtectedPonHubDataStorage.LoadStatus.LEGACY,
                "{\"schemaVersion\":99}", ProtectedPonHubDataStorage.LoadStatus.UNSUPPORTED_VERSION,
                "null", ProtectedPonHubDataStorage.LoadStatus.CORRUPT).entrySet()) {
            Files.writeString(path, entry.getKey());
            ProtectedPonHubDataStorage storage = new ProtectedPonHubDataStorage(path);
            assertThrows(DataLoadingException.class, storage::load);
            assertEquals(entry.getValue(), storage.getLoadStatus());
        }
        byte[] before = Files.readAllBytes(path);
        ProtectedPonHubDataStorage denied = new ProtectedPonHubDataStorage(path, file -> {
            throw new AccessDeniedException(file.toString());
        });
        assertThrows(DataLoadingException.class, denied::load);
        assertEquals(ProtectedPonHubDataStorage.LoadStatus.UNREADABLE, denied.getLoadStatus());
        assertThrows(IOException.class, () -> denied.saveData(new PonHubData().exportState()));
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    public void load_repairedFile_requiresNewSessionAndLoadsEditedSupportedData() throws Exception {
        Path path = directory.resolve("data.json");
        Files.writeString(path, "broken");
        ProtectedPonHubDataStorage rejected = new ProtectedPonHubDataStorage(path);
        assertThrows(DataLoadingException.class, rejected::load);
        String repaired = JsonPonHubDataCodec.encode(new PonHubData().exportState())
                .replace("\"STUDENT\" : 0", "\"STUDENT\" : 7");
        Files.writeString(path, repaired);
        assertThrows(IllegalStateException.class, rejected::load);
        assertThrows(IOException.class, () -> rejected.saveData(new PonHubData().exportState()));
        assertEquals(repaired, Files.readString(path));
        ProtectedPonHubDataStorage restarted = new ProtectedPonHubDataStorage(path);
        assertEquals(7L, restarted.load().people().getLastAllocatedSequences()
                .get(seedu.address.model.person.PersonRole.STUDENT));
        assertEquals(ProtectedPonHubDataStorage.LoadStatus.LOADED, restarted.getLoadStatus());
        assertTrue(restarted.getDataLoadError().isEmpty());
    }

    @Test
    public void load_duplicateRecordsAndInvalidIdentities_rejectsBeforeInstallation() throws Exception {
        PonHubDataState sample = new PonHubDataState(PeopleSampleDataUtil.getSamplePeopleRegistry().exportState(),
                List.of(), List.of(), 0);
        String valid = JsonPonHubDataCodec.encode(sample);
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (int variant = 0; variant < 3; variant++) {
            var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(valid);
            var people = (com.fasterxml.jackson.databind.node.ArrayNode) json.get("people");
            if (variant == 0) {
                people.add(people.get(0).deepCopy());
            } else if (variant == 1) {
                var record = (com.fasterxml.jackson.databind.node.ObjectNode) people.get(0);
                record.put("id", "S0");
            } else {
                var counters = (com.fasterxml.jackson.databind.node.ObjectNode) json.get("personCounters");
                counters.put("STUDENT", 0);
            }
            Path path = directory.resolve("invalid-records" + variant + ".json");
            Files.writeString(path, mapper.writeValueAsString(json));
            byte[] before = Files.readAllBytes(path);
            ProtectedPonHubDataStorage storage = new ProtectedPonHubDataStorage(path);
            assertThrows(DataLoadingException.class, storage::load);
            assertEquals(ProtectedPonHubDataStorage.LoadStatus.CORRUPT, storage.getLoadStatus());
            assertThrows(IOException.class, () -> storage.saveData(sample));
            assertArrayEquals(before, Files.readAllBytes(path));
        }
    }

    @Test
    public void load_missingExceptionForExistingPath_doesNotStartFresh() throws Exception {
        Path path = directory.resolve("existing.json");
        Files.writeString(path, "preserve me");
        ProtectedPonHubDataStorage storage = new ProtectedPonHubDataStorage(path, file -> {
            throw new NoSuchFileException(file.toString());
        });
        assertThrows(DataLoadingException.class, storage::load);
        assertEquals(ProtectedPonHubDataStorage.LoadStatus.UNREADABLE, storage.getLoadStatus());
        assertEquals("preserve me", Files.readString(path));
    }

    @Test
    public void load_removedRejectedFile_doesNotUnlockSession() throws Exception {
        Path path = directory.resolve("data.json");
        Files.writeString(path, "broken");
        ProtectedPonHubDataStorage rejected = new ProtectedPonHubDataStorage(path);
        assertThrows(DataLoadingException.class, rejected::load);
        Files.delete(path);
        assertThrows(IOException.class, () -> rejected.saveData(new PonHubData().exportState()));
        assertFalse(Files.exists(path));
    }

    @Test
    public void load_unrelatedProgrammingFailure_isNotReportedAsCorruptData() {
        ProtectedPonHubDataStorage storage = new ProtectedPonHubDataStorage(directory.resolve("data.json"), file -> {
            throw new IllegalStateException("Programming defect");
        });
        assertThrows(IllegalStateException.class, storage::load);
        assertThrows(IOException.class, () -> storage.saveData(new PonHubData().exportState()));
    }
}
