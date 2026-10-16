package com.cqu.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A relief resource (e.g. water, food, first aid kits) held by a {@link Department}.
 * Stock is deducted when allocated to a response task, and flagged once it falls
 * to or below its reorder level.
 */
public class Resource implements Serializable {

    private static final long serialVersionUID = 1L;

    private int resourceId;
    private int departmentId;
    private String departmentName;
    private String name;
    private int quantityAvailable;
    private int reorderLevel;
    private LocalDateTime lastUpdated;

    public Resource() {
    }

    public Resource(String name, int quantityAvailable, int reorderLevel) {
        this.name = name;
        this.quantityAvailable = quantityAvailable;
        this.reorderLevel = reorderLevel;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /** True when stock has fallen to or below the reorder threshold. */
    public boolean isLowStock() {
        return quantityAvailable <= reorderLevel;
    }

    /**
     * Deducts {@code amount} units from stock.
     * @throws IllegalArgumentException if amount is invalid or exceeds available quantity.
     */
    public void deduct(int amount) {
        if (amount <= 0 || amount > quantityAvailable) {
            throw new IllegalArgumentException("Cannot deduct " + amount + " units; only "
                    + quantityAvailable + " available");
        }
        this.quantityAvailable -= amount;
    }

    /**
     * Adds {@code amount} units to stock (restocking).
     * @throws IllegalArgumentException if amount is not positive.
     */
    public void restock(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Restock amount must be positive");
        }
        this.quantityAvailable += amount;
    }

    @Override
    public String toString() {
        return name + " (" + quantityAvailable + " left" + (isLowStock() ? ", LOW" : "") + ")";
    }
}
