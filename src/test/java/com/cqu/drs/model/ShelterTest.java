package com.cqu.drs.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for shelter capacity rules (new feature 1). */
class ShelterTest {

    @Test
    @DisplayName("TC-E01 new shelter is OPEN with all places free")
    void newShelterIsOpen() {
        Shelter s = new Shelter("Hall", "Yeppoon", 100, "07 4900 0000");
        assertEquals("OPEN", s.getStatus());
        assertEquals(100, s.getFreeSpaces());
        assertTrue(s.canAccept(100));
    }

    @Test
    @DisplayName("TC-E02 check-in up to capacity marks shelter FULL")
    void checkInToCapacityMakesFull() {
        Shelter s = new Shelter("Hall", "Yeppoon", 50, null);
        s.checkIn(30);
        s.checkIn(20);
        assertEquals(50, s.getOccupancy());
        assertEquals("FULL", s.getStatus());
        assertFalse(s.canAccept(1));
    }

    @Test
    @DisplayName("TC-E03 check-in beyond capacity is rejected and occupancy unchanged")
    void overCapacityRejected() {
        Shelter s = new Shelter("Hall", "Yeppoon", 10, null);
        s.checkIn(8);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> s.checkIn(3));
        assertTrue(ex.getMessage().contains("2 free"));
        assertEquals(8, s.getOccupancy());
    }

    @Test
    @DisplayName("TC-E04 closed shelter cannot accept evacuees")
    void closedShelterRejects() {
        Shelter s = new Shelter("Hall", "Yeppoon", 10, null);
        s.setClosed(true);
        assertEquals("CLOSED", s.getStatus());
        assertThrows(IllegalStateException.class, () -> s.checkIn(1));
    }

    @Test
    @DisplayName("TC-E05 invalid numbers are rejected")
    void invalidNumbersRejected() {
        Shelter s = new Shelter("Hall", "Yeppoon", 10, null);
        assertThrows(IllegalArgumentException.class, () -> s.checkIn(0));
        assertThrows(IllegalArgumentException.class, () -> s.checkOut(1));
        assertThrows(IllegalArgumentException.class, () -> new Shelter("X", "Y", 0, null));
    }

    @Test
    @DisplayName("TC-E06 check-out frees places")
    void checkOutFreesPlaces() {
        Shelter s = new Shelter("Hall", "Yeppoon", 10, null);
        s.checkIn(10);
        s.checkOut(4);
        assertEquals(6, s.getOccupancy());
        assertEquals("OPEN", s.getStatus());
        assertEquals(0.6, s.getOccupancyRatio(), 1e-9);
    }
}
