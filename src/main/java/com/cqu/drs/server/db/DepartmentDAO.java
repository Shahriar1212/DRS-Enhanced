package com.cqu.drs.server.db;

import com.cqu.drs.model.Department;
import com.cqu.drs.model.DepartmentType;
import com.cqu.drs.model.Resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data access for {@code departments}, {@code resources} and {@code resource_allocations}. */
public class DepartmentDAO {

    public int insert(Connection c, Department d) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO departments (name, type, contact_info, created_at) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getType().name());
            ps.setString(3, d.getContactInfo());
            ps.setObject(4, LocalDateTime.now());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                d.setDepartmentId(keys.getInt(1));
                return d.getDepartmentId();
            }
        }
    }

    public Department findByName(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM departments WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapDepartment(rs) : null;
            }
        }
    }

    /** All departments with their resources attached (one query each, joined in memory via a HashMap). */
    public List<Department> findAllWithResources(Connection c) throws SQLException {
        Map<Integer, Department> byId = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM departments ORDER BY department_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Department d = mapDepartment(rs);
                byId.put(d.getDepartmentId(), d);
            }
        }
        for (Resource r : findAllResources(c)) {
            Department d = byId.get(r.getDepartmentId());
            if (d != null) {
                d.getResources().add(r);
            }
        }
        return new ArrayList<>(byId.values());
    }

    public int insertResource(Connection c, int departmentId, Resource r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO resources (department_id, name, quantity_available, reorder_level, updated_at) VALUES (?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, departmentId);
            ps.setString(2, r.getName());
            ps.setInt(3, r.getQuantityAvailable());
            ps.setInt(4, r.getReorderLevel());
            ps.setObject(5, LocalDateTime.now());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                r.setResourceId(keys.getInt(1));
                r.setDepartmentId(departmentId);
                return r.getResourceId();
            }
        }
    }

    public boolean resourceExists(Connection c, int departmentId, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM resources WHERE department_id = ? AND name = ?")) {
            ps.setInt(1, departmentId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<Resource> findAllResources(Connection c) throws SQLException {
        List<Resource> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT r.*, d.name AS department_name FROM resources r JOIN departments d"
                        + " ON r.department_id = d.department_id ORDER BY d.name, r.name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResource(rs));
            }
        }
        return list;
    }

    /** Reads a resource and locks its row until the transaction ends (prevents double allocation). */
    public Resource findResourceForUpdate(Connection c, int resourceId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT r.*, d.name AS department_name FROM resources r JOIN departments d"
                        + " ON r.department_id = d.department_id WHERE r.resource_id = ? FOR UPDATE")) {
            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapResource(rs) : null;
            }
        }
    }

    public void updateQuantity(Connection c, Resource r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE resources SET quantity_available = ?, updated_at = ? WHERE resource_id = ?")) {
            ps.setInt(1, r.getQuantityAvailable());
            ps.setObject(2, LocalDateTime.now());
            ps.setInt(3, r.getResourceId());
            ps.executeUpdate();
        }
    }

    public void insertAllocation(Connection c, int taskId, int resourceId, int quantity, String by) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO resource_allocations (task_id, resource_id, quantity, allocated_by, allocated_at) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, taskId);
            ps.setInt(2, resourceId);
            ps.setInt(3, quantity);
            ps.setString(4, by);
            ps.setObject(5, LocalDateTime.now());
            ps.executeUpdate();
        }
    }

    private Department mapDepartment(ResultSet rs) throws SQLException {
        Department d = new Department(rs.getString("name"), DepartmentType.valueOf(rs.getString("type")),
                rs.getString("contact_info"));
        d.setDepartmentId(rs.getInt("department_id"));
        return d;
    }

    private Resource mapResource(ResultSet rs) throws SQLException {
        Resource r = new Resource(rs.getString("name"), rs.getInt("quantity_available"), rs.getInt("reorder_level"));
        r.setResourceId(rs.getInt("resource_id"));
        r.setDepartmentId(rs.getInt("department_id"));
        r.setDepartmentName(rs.getString("department_name"));
        r.setLastUpdated(rs.getObject("updated_at", LocalDateTime.class));
        return r;
    }
}
