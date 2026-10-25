package ponhub.model.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RoomTest {

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Room(null));
    }

    @Test
    public void constructor_invalidRoom_throwsIllegalArgumentException() {
        String[] invalidRooms = {"", "   ", "Room 1", "-R1", "R1-", "R--1", "R_1", "12345678901"};

        for (String invalidRoom : invalidRooms) {
            assertThrows(IllegalArgumentException.class, () -> new Room(invalidRoom));
        }
    }

    @Test
    public void constructor_mixedCase_normalizesCase() {
        assertEquals("LAB1", new Room(" lab1 ").toString());
        assertEquals("B-01", new Room(" b-01 ").toString());
    }

    @Test
    public void isValidRoom() {
        assertThrows(NullPointerException.class, () -> Room.isValidRoom(null));

        assertFalse(Room.isValidRoom("Room 1"));
        assertFalse(Room.isValidRoom("-R1"));
        assertFalse(Room.isValidRoom("R1-"));
        assertFalse(Room.isValidRoom("R--1"));
        assertFalse(Room.isValidRoom("12345678901"));

        assertTrue(Room.isValidRoom("R1"));
        assertTrue(Room.isValidRoom("B-01"));
        assertTrue(Room.isValidRoom("lab1"));
        assertTrue(Room.isValidRoom("1234567890"));
    }

    @Test
    public void equals_usesNormalizedValue() {
        Room room = new Room("R1");

        assertEquals(room, new Room("r1"));
        assertEquals(new Room("B-01"), new Room("b-01"));
        assertNotEquals(room, new Room("R2"));
        assertNotEquals(room, null);
        assertNotEquals(room, "R1");
    }
}
