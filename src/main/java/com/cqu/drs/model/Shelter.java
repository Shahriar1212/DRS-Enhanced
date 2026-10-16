package com.cqu.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * An evacuation centre (shelter) with a fixed capacity. Part of the
 * "Evacuation Warning &amp; Shelter Management" feature of DRS-Enhanced.
 */
public class Shelter implements Serializable {

    private static final long serialVersionUID = 1L;

    private int shelterId;
    private String name;
    private String location;
    private int capacity;
    private int occupancy;
    private String contact;
    private boolean closed;
    private LocalDateTime lastUpdated;

    public Shelter() {
    }

    public Shelter(String name, String location, int capacity, String contact) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.occupancy = 0;
        this.contact = contact;
        this.closed = false;
    }

    public int getShelterId() {
        return shelterId;
    }

    public void setShelterId(int shelterId) {
        this.shelterId = shelterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(int occupancy) {
        this.occupancy = occupancy;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /** Number of places still free. */
    public int getFreeSpaces() {
        return Math.max(0, capacity - occupancy);
    }

    /** Occupancy as a fraction between 0 and 1 (used by the GUI progress bar). */
    public double getOccupancyRatio() {
        return capacity == 0 ? 0 : (double) occupancy / capacity;
    }

    /** OPEN, FULL or CLOSED. */
    public String getStatus() {
        if (closed) {
            return "CLOSED";
        }
        return occupancy >= capacity ? "FULL" : "OPEN";
    }

    /** True when the shelter is open and can take at least {@code people} more evacuees. */
    public boolean canAccept(int people) {
        return !closed && people > 0 && getFreeSpaces() >= people;
    }

    /**
     * Checks evacuees into the shelter.
     * @throws IllegalArgumentException if the number is not positive
     * @throws IllegalStateException    if the shelter is closed or lacks space
     */
    public void checkIn(int people) {
        if (people <= 0) {
            throw new IllegalArgumentException("Number of evacuees must be positive");
        }
        if (closed) {
            throw new IllegalStateException(name + " is closed");
        }
        if (getFreeSpaces() < people) {
            throw new IllegalStateException(name + " has only " + getFreeSpaces() + " free places");
        }
        occupancy += people;
    }

    /**
     * Checks evacuees out of the shelter (e.g. when they return home).
     * @throws IllegalArgumentException if the number is invalid
     */
    public void checkOut(int people) {
        if (people <= 0 || people > occupancy) {
            throw new IllegalArgumentException("Cannot check out " + people + " evacuees; "
                    + occupancy + " currently sheltered");
        }
        occupancy -= people;
    }

    @Override
    public String toString() {
        return name + " (" + location + ") " + occupancy + "/" + capacity + " " + getStatus();
    }
}
