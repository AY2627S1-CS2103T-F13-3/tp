package seedu.address.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.lesson.LessonId;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonId;
import seedu.address.model.query.AttendanceHistoryEntry;
import seedu.address.model.query.LessonView;
import seedu.address.model.query.StudentView;
import seedu.address.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder.";

    public static final String RETRIEVAL_UNAVAILABLE = "Shared-lesson retrieval is not implemented yet";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final AddressBookParser addressBookParser;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        addressBookParser = new AddressBookParser();
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        CommandResult commandResult;
        Command command = addressBookParser.parseCommand(commandText);
        commandResult = command.execute(model);

        // Help must remain available even when the operational data cannot be saved.
        if (command instanceof HelpCommand) {
            return commandResult;
        }

        try {
            storage.saveAddressBook(model.getAddressBook());
        } catch (AccessDeniedException e) {
            throw new CommandException(String.format(FILE_OPS_PERMISSION_ERROR_FORMAT, e.getMessage()), e);
        } catch (IOException ioe) {
            throw new CommandException(String.format(FILE_OPS_ERROR_FORMAT, ioe.getMessage()), ioe);
        }

        return commandResult;
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
    }

    @Override
    public ObservableList<LessonView> getLessonList() {
        throw new UnsupportedOperationException(RETRIEVAL_UNAVAILABLE);
    }

    @Override
    public ObservableList<LessonView> getFilteredLessonList() {
        throw new UnsupportedOperationException(RETRIEVAL_UNAVAILABLE);
    }

    @Override
    public Optional<LessonView> findLessonById(LessonId lessonId) {
        throw new UnsupportedOperationException(RETRIEVAL_UNAVAILABLE);
    }

    @Override
    public ObservableList<LessonView> getStudentLessons(PersonId studentId) {
        throw new UnsupportedOperationException(RETRIEVAL_UNAVAILABLE);
    }

    @Override
    public ObservableList<StudentView> getLessonRoster(LessonId lessonId) {
        throw new UnsupportedOperationException(RETRIEVAL_UNAVAILABLE);
    }

    @Override
    public ObservableList<LessonView> getTutorSchedule(PersonId tutorId) {
        throw new UnsupportedOperationException(RETRIEVAL_UNAVAILABLE);
    }

    @Override
    public ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId) {
        throw new UnsupportedOperationException(RETRIEVAL_UNAVAILABLE);
    }

    @Override
    public ObservableList<AttendanceHistoryEntry> getAttendanceHistory(PersonId studentId, LessonId lessonId) {
        throw new UnsupportedOperationException(RETRIEVAL_UNAVAILABLE);
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }
}
