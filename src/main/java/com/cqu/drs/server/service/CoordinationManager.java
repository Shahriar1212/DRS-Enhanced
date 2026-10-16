package com.cqu.drs.server.service;

import com.cqu.drs.model.Department;
import com.cqu.drs.model.Resource;
import com.cqu.drs.model.ResponseTask;
import com.cqu.drs.model.Status;
import com.cqu.drs.model.User;
import com.cqu.drs.model.Volunteer;
import com.cqu.drs.security.AccessControl;
import com.cqu.drs.security.CryptoService;
import com.cqu.drs.server.db.VolunteerDAO;
import com.cqu.drs.util.Validation;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Coordination of departments and people: response tasks, volunteer
 * assignment, resource allocation/restocking and volunteer registration.
 * Row locks ({@code SELECT ... FOR UPDATE}) make concurrent requests from
 * different clients safe (e.g. two coordinators grabbing the same volunteer).
 */
public class CoordinationManager {

    private final ServerContext ctx;

    CoordinationManager(ServerContext ctx) {
        this.ctx = ctx;
    }

    public ArrayList<ResponseTask> listTasks() throws SQLException {
        try (Connection c = ctx.connection()) {
            return new ArrayList<>(ctx.taskDAO.findAll(c));
        }
    }

    public ArrayList<Department> listDepartments() throws SQLException {
        try (Connection c = ctx.connection()) {
            return new ArrayList<>(ctx.departmentDAO.findAllWithResources(c));
        }
    }

    public ArrayList<Resource> listResources() throws SQLException {
        try (Connection c = ctx.connection()) {
            return new ArrayList<>(ctx.departmentDAO.findAllResources(c));
        }
    }

    /** Changes a task's status; resolving a task releases its volunteer. */
    public ResponseTask updateTaskStatus(User user, int taskId, Status status) throws SQLException {
        if (status == null || status == Status.REPORTED || status == Status.ASSESSED) {
            throw new IllegalArgumentException("Task status must be ASSIGNED, IN_PROGRESS or RESOLVED");
        }
        Connection c = ctx.connection();
        ResponseTask task;
        try {
            c.setAutoCommit(false);
            task = ctx.taskDAO.findById(c, taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task #" + taskId + " not found");
            }
            if (task.getStatus() == Status.RESOLVED) {
                throw new IllegalStateException("Task #" + taskId + " is already resolved");
            }
            task.setStatus(status);
            if (status == Status.RESOLVED && task.getAssignedVolunteer() != null) {
                ctx.volunteerDAO.setAvailable(c, task.getAssignedVolunteer().getVolunteerId(), true);
            }
            ctx.taskDAO.update(c, task);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            ServerContext.rollback(c);
            throw e;
        } finally {
            c.close();
        }
        ctx.audit().record(user.getUsername(), "UPDATE_TASK_STATUS", "Task #" + taskId + " -> " + status);
        return task;
    }

    /** Assigns an available volunteer to an open task. */
    public ResponseTask assignVolunteer(User user, int taskId, int volunteerId) throws SQLException {
        Connection c = ctx.connection();
        ResponseTask task;
        try {
            c.setAutoCommit(false);
            task = ctx.taskDAO.findById(c, taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task #" + taskId + " not found");
            }
            if (task.getStatus() == Status.RESOLVED) {
                throw new IllegalStateException("Task #" + taskId + " is already resolved");
            }
            if (task.getAssignedVolunteer() != null) {
                throw new IllegalStateException("Task #" + taskId + " already has volunteer "
                        + task.getAssignedVolunteer().getName());
            }
            VolunteerDAO.Row vRow = ctx.volunteerDAO.findByIdForUpdate(c, volunteerId);
            if (vRow == null) {
                throw new IllegalArgumentException("Volunteer #" + volunteerId + " not found");
            }
            task.assignVolunteer(vRow.volunteer);      // throws if not available
            ctx.volunteerDAO.setAvailable(c, volunteerId, false);
            ctx.taskDAO.update(c, task);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            ServerContext.rollback(c);
            throw e;
        } finally {
            c.close();
        }
        ctx.audit().record(user.getUsername(), "ASSIGN_VOLUNTEER", task.getVolunteerName() + " -> task #" + taskId);
        return task;
    }

    /**
     * Allocates stock from the task's own department to the task.
     * @return the updated resource (check {@link Resource#isLowStock()} for a warning)
     */
    public Resource allocateResource(User user, int taskId, int resourceId, int quantity) throws SQLException {
        Validation.requirePositive(quantity, "Quantity");
        Connection c = ctx.connection();
        Resource resource;
        try {
            c.setAutoCommit(false);
            ResponseTask task = ctx.taskDAO.findById(c, taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task #" + taskId + " not found");
            }
            if (task.getStatus() == Status.RESOLVED) {
                throw new IllegalStateException("Task #" + taskId + " is already resolved");
            }
            resource = ctx.departmentDAO.findResourceForUpdate(c, resourceId);
            if (resource == null) {
                throw new IllegalArgumentException("Resource #" + resourceId + " not found");
            }
            if (resource.getDepartmentId() != task.getDepartment().getDepartmentId()) {
                throw new IllegalArgumentException(resource.getName() + " belongs to " + resource.getDepartmentName()
                        + ", not to " + task.getDepartmentName());
            }
            resource.deduct(quantity);
            ctx.departmentDAO.updateQuantity(c, resource);
            ctx.departmentDAO.insertAllocation(c, taskId, resourceId, quantity, user.getUsername());
            c.commit();
        } catch (SQLException | RuntimeException e) {
            ServerContext.rollback(c);
            throw e;
        } finally {
            c.close();
        }
        ctx.audit().record(user.getUsername(), "ALLOCATE_RESOURCE",
                quantity + " x " + resource.getName() + " -> task #" + taskId);
        return resource;
    }

    /** Adds stock to a resource. */
    public Resource restock(User user, int resourceId, int quantity) throws SQLException {
        Validation.requirePositive(quantity, "Quantity");
        Connection c = ctx.connection();
        Resource resource;
        try {
            c.setAutoCommit(false);
            resource = ctx.departmentDAO.findResourceForUpdate(c, resourceId);
            if (resource == null) {
                throw new IllegalArgumentException("Resource #" + resourceId + " not found");
            }
            resource.restock(quantity);
            ctx.departmentDAO.updateQuantity(c, resource);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            ServerContext.rollback(c);
            throw e;
        } finally {
            c.close();
        }
        ctx.audit().record(user.getUsername(), "RESTOCK_RESOURCE", "+" + quantity + " " + resource.getName()
                + " (" + resource.getDepartmentName() + ")");
        return resource;
    }

    /** Registers a volunteer (phone stored encrypted). */
    public Volunteer registerVolunteer(User user, String name, String phone, String skill) throws SQLException {
        Volunteer v = new Volunteer(Validation.requireText(name, "Name", 100),
                Validation.optionalPhone(phone), Validation.optionalText(skill, "Skill", 100));
        try (Connection c = ctx.connection()) {
            ctx.volunteerDAO.insert(c, v, ctx.getCrypto().encrypt(v.getPhone()));
        }
        ctx.audit().record(user.getUsername(), "REGISTER_VOLUNTEER", "Volunteer #" + v.getVolunteerId() + " " + v.getName());
        return v;
    }

    /** Volunteers; phone numbers decrypted for coordinators/admins and masked for everyone else. */
    public ArrayList<Volunteer> listVolunteers(User viewer) throws SQLException {
        boolean showPhone = AccessControl.canViewPersonalData(viewer.getRole());
        ArrayList<Volunteer> list = new ArrayList<>();
        try (Connection c = ctx.connection()) {
            for (VolunteerDAO.Row row : ctx.volunteerDAO.findAll(c)) {
                String phone;
                try {
                    phone = ctx.getCrypto().decrypt(row.phoneEncrypted);
                } catch (SecurityException e) {
                    phone = "[unreadable]";
                }
                row.volunteer.setPhone(showPhone ? phone : CryptoService.mask(phone));
                list.add(row.volunteer);
            }
        }
        return list;
    }
}
