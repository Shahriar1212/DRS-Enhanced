package com.cqu.drs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceTest {

    @Test
    void newResourceIsNotLowStockWhenAboveReorderLevel() {
        Resource r = new Resource("Bottled water", 500, 100);
        assertFalse(r.isLowStock());
    }

    @Test
    void deductReducesQuantityAndFlagsLowStock() {
        Resource r = new Resource("Bottled water", 500, 100);

        r.deduct(420);

        assertEquals(80, r.getQuantityAvailable());
        assertTrue(r.isLowStock());
    }

    @Test
    void deductingMoreThanAvailableThrows() {
        Resource r = new Resource("First aid kits", 10, 2);
        assertThrows(IllegalArgumentException.class, () -> r.deduct(20));
    }

    @Test
    void deductingNegativeAmountThrows() {
        Resource r = new Resource("First aid kits", 10, 2);
        assertThrows(IllegalArgumentException.class, () -> r.deduct(-5));
    }
}
