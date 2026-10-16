package com.cqu.drs.model;

import java.io.Serializable;

/**
 * Aggregated figures shown on the dashboard summary cards. Calculated on the
 * server with SQL aggregate queries.
 */
public class DashboardSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private int openReports;
    private int criticalReports;
    private int activeTasks;
    private int activeAlerts;
    private int peopleSheltered;
    private int shelterCapacity;
    private int lowStockResources;
    private double totalDamageCost;
    private int restorationJobsOpen;

    public int getOpenReports() {
        return openReports;
    }

    public void setOpenReports(int openReports) {
        this.openReports = openReports;
    }

    public int getCriticalReports() {
        return criticalReports;
    }

    public void setCriticalReports(int criticalReports) {
        this.criticalReports = criticalReports;
    }

    public int getActiveTasks() {
        return activeTasks;
    }

    public void setActiveTasks(int activeTasks) {
        this.activeTasks = activeTasks;
    }

    public int getActiveAlerts() {
        return activeAlerts;
    }

    public void setActiveAlerts(int activeAlerts) {
        this.activeAlerts = activeAlerts;
    }

    public int getPeopleSheltered() {
        return peopleSheltered;
    }

    public void setPeopleSheltered(int peopleSheltered) {
        this.peopleSheltered = peopleSheltered;
    }

    public int getShelterCapacity() {
        return shelterCapacity;
    }

    public void setShelterCapacity(int shelterCapacity) {
        this.shelterCapacity = shelterCapacity;
    }

    public int getLowStockResources() {
        return lowStockResources;
    }

    public void setLowStockResources(int lowStockResources) {
        this.lowStockResources = lowStockResources;
    }

    public double getTotalDamageCost() {
        return totalDamageCost;
    }

    public void setTotalDamageCost(double totalDamageCost) {
        this.totalDamageCost = totalDamageCost;
    }

    public int getRestorationJobsOpen() {
        return restorationJobsOpen;
    }

    public void setRestorationJobsOpen(int restorationJobsOpen) {
        this.restorationJobsOpen = restorationJobsOpen;
    }

    @Override
    public String toString() {
        return "DashboardSummary{openReports=" + openReports + ", critical=" + criticalReports
                + ", activeTasks=" + activeTasks + ", activeAlerts=" + activeAlerts
                + ", sheltered=" + peopleSheltered + "/" + shelterCapacity
                + ", lowStock=" + lowStockResources + ", damageCost=" + totalDamageCost + "}";
    }
}
