package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.TypicalPersons.ALICE;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.AddressBook;
import seedu.address.model.person.Person;
import seedu.address.model.person.Remark;
import seedu.address.testutil.PersonBuilder;

public class RemarkStorageTest {
    @TempDir
    public Path tempDir;

    @Test
    public void saveAndRead_remark_roundTrips() throws Exception {
        Person person = new PersonBuilder(ALICE).withRemark("Likes baseball; 日本語 \"quoted\"").build();
        AddressBook book = new AddressBook();
        book.addPerson(person);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(tempDir.resolve("addressbook.json"));
        storage.saveAddressBook(book);
        assertEquals(book, storage.readAddressBook().orElseThrow());
    }

    @Test
    public void read_oldFileWithoutRemark_defaultsToEmpty() throws Exception {
        JsonAddressBookStorage storage = new JsonAddressBookStorage(
                Path.of("src", "test", "data", "JsonSerializableAddressBookTest", "typicalPersonsAddressBook.json"));
        for (Person person : storage.readAddressBook().orElseThrow().getPersonList()) {
            assertEquals(new Remark(""), person.getRemark());
        }
    }
}
