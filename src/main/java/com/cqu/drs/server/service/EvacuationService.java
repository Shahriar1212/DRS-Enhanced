package com.cqu.drs.server.service;

import com.cqu.drs.model.AlertLevel;
import com.cqu.drs.model.AlertStatus;
import com.cqu.drs.model.EvacuationAlert;
import com.cqu.drs.model.Shelter;
import com.cqu.drs.model.Status;
import com.cqu.drs.model.User;
import com.cqu.drs.server.db.ReportDAO;
import com.cqu.drs.util.EvacuationPlanner;
import com.cqu.drs.util.Validation;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * NEW FEATURE 1 - Evacuation Warning &amp; Shelter Management.
 * Coordinators issue/lift tiered evacuation warnings for a zone affected by a
 * reported disaster and nominate a shelter; field officers check evacuees in
 * and out of shelters, with capacity enforced under a row lock.
 */
public class EvacuationService {

    private final ServerContext ctx;

    EvacuationService(ServerContext ctx) {
        this.ctx = ctx;
    }

    public ArrayList<Shelter> listShelters() throws SQLException {
        try (Connection c = ctx.connection()) {
            return new ArrayList<>(ctx.shelterDAO.findAllShelters(c));
        }
    }

    public Shelter addShelter(User user, String name, String location, int capacity, String contact)
            throws SQLException {
        Validation.requirePositive(capacity, "Capacity");
        Shelter s = new Shelter(Validation.requireText(name, "Shelter name", 100),
                Validation.requireText(location, "Location", 150), capacity,
                Validation.optionalText(contact, "Contact", 100));
        try (Connection c = ctx.connection()) {
            if (ctx.shelterDAO.shelterNameExists(c, s.getName())) {
                throw new IllegalArgumentException("A shelter called '" + s.getName() + "' already exists");
            }
            ctx.shelterDAO.insertShelter(c, s);
        }
        ctx.audit().record(user.getUsername(), "ADD_SHELTER", "Shelter #" + s.getShelterId() + " " + s.getName()
                + " capacity " + capacity);
        return s;
    }

    /** Checks evacuees in (positive) or out (negative) of a shelter inside one locked transaction. */
    public Shelter changeOccupancy(User user, int shelterId, int people, boolean checkIn) throws SQLException {
        Validation.requirePositive(people, "Number of evacuees");
        Connection c = ctx.connection();
        Shelter s;
        try {
            c.setAutoCommit(false);
            s = ctx.shelterDAO.findShelterForUpdate(c, shelterId);
            if (s == null) {
                throw new IllegalArgumentException("Shelter #" + shelterId + " not found");
            }
            if (checkIn) {
                s.checkIn(people);
            } else {
                s.checkOut(people);
            }
            ctx.shelterDAO.updateOccupancy(c, s);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            ServerContext.rollback(c);
            throw e;
        } finally {
            c.close();
        }
        ctx.audit().record(user.getUsername(), checkIn ? "CHECK_IN_EVACUEES" : "CHECK_OUT_EVACUEES",
                people + " evacuee(s) " + (checkIn ? "into " : "out of ") + s.getName()
                        + " (" + s.getOccupancy() + "/" + s.getCapacity() + ")");
        return s;
    }

    /** Best shelter for a group from a zone (see {@link EvacuationPlanner#recommendShelter}). */
    public Shelter recommend(String zone, int people) throws SQLException {
        Optional<Shelter> best = EvacuationPlanner.recommendShelter(listShelters(), zone, people);
        return best.orElseThrow(() -> new IllegalStateException(
                "No open shelter has room for " + people + " evacuee(s) - add or reopen a shelter"));
    }

    /**
     * Issues an alert. If no level is given the recommended level for the report's
     * severity is used; if no message is given a standard message is drafted.
     */
    public EvacuationAlert issueAlert(User user, int reportId, String zone, AlertLevel level,
                                      Integer shelterId, String message) throws SQLException {
        String z = Validation.requireText(zone, "Zone", 150);
        Connection c = ctx.connection();
        EvacuationAlert alert;
        try {
            ReportDAO.Row row = ctx.reportDAO.findById(c, reportId, false);
            if (row == null) {
                throw new IllegalArgumentException("Report #" + reportId + " not found");
            }
            if (row.report.getStatus() == Status.RESOLVED) {
                throw new IllegalStateException("Report #" + reportId + " is resolved - no alert needed");
            }
            AlertLevel lvl = level != null ? level : EvacuationPlanner.recommendAlertLevel(row.report.getSeverity());
            Shelter shelter = null;
            if (shelterId != null) {
                shelter = ctx.shelterDAO.findAllShelters(c).stream()
                        .filter(s -> s.getShelterId() == shelterId).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Shelter #" + shelterId + " not found"));
                if (shelter.isClosed() || shelter.getFreeSpaces() == 0) {
                    throw new IllegalStateException(shelter.getName() + " is " + shelter.getStatus()
                            + " - choose another shelter");
                }
            }
            String msg = Validation.isBlank(message)
                    ? EvacuationPlanner.draftMessage(row.report, lvl, z, shelter)
                    : Validation.requireText(message, "Message", 1000);
            alert = new EvacuationAlert(reportId, z, lvl, msg);
            alert.setShelterId(shelterId);
            alert.setShelterName(shelter == null ? null : shelter.getName());
            alert.setIssuedBy(user.getUsername());
            alert.setReportSummary(row.report.getSummary());
            ctx.shelterDAO.insertAlert(c, alert);
        } finally {
            c.close();
        }
        ctx.audit().record(user.getUsername(), "ISSUE_ALERT", "Alert #" + alert.getAlertId() + " "
                + alert.getLevel() + " for " + z + " (report #" + reportId + ")");
        return alert;
    }

    public EvacuationAlert liftAlert(User user, int alertId) throws SQLException {
        Connection c = ctx.connection();
        EvacuationAlert alert;
        try {
            c.setAutoCommit(false);
            alert = ctx.shelterDAO.findAlertForUpdate(c, alertId);
            if (alert == null) {
                throw new IllegalArgumentException("Alert #" + alertId + " not found");
            }
            alert.lift();
            ctx.shelterDAO.updateAlertStatus(c, alert);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            ServerContext.rollback(c);
            throw e;
        } finally {
            c.close();
        }
        ctx.audit().record(user.getUsername(), "LIFT_ALERT", "Alert #" + alertId + " lifted (" + alert.getZone() + ")");
        return alert;
    }

    /** Alerts; members of the public only see active ones. */
    public ArrayList<EvacuationAlert> listAlerts(boolean activeOnly) throws SQLException {
        try (Connection c = ctx.connection()) {
            List<EvacuationAlert> all = ctx.shelterDAO.findAllAlerts(c);
            ArrayList<EvacuationAlert> out = new ArrayList<>();
            for (EvacuationAlert a : all) {
                if (!activeOnly || a.getStatus() == AlertStatus.ACTIVE) {
                    out.add(a);
                }
            }
            return out;
        }
    }
}
