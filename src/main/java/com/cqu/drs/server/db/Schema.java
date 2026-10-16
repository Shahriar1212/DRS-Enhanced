package com.cqu.drs.server.db;

import java.util.List;

/**
 * DDL for every DRS-Enhanced table. The server runs these statements on
 * start-up ({@link DatabaseManager#initialise()}) so the database and tables are
 * generated programmatically; the same statements are written to
 * {@code database/drs_enhanced.sql} by {@link SqlScriptGenerator}.
 * Every table has an AUTO_INCREMENT primary key, so MySQL creates the ID of each entity.
 */
public final class Schema {

    private Schema() {
    }

    public static final String USERS = "CREATE TABLE IF NOT EXISTS users ("
            + " user_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " username VARCHAR(50) NOT NULL UNIQUE,"
            + " full_name VARCHAR(100) NOT NULL,"
            + " password_hash VARCHAR(200) NOT NULL,"
            + " role VARCHAR(20) NOT NULL,"
            + " public_key TEXT NOT NULL,"
            + " private_key_enc TEXT NOT NULL,"
            + " active BOOLEAN NOT NULL DEFAULT TRUE,"
            + " failed_attempts INT NOT NULL DEFAULT 0,"
            + " created_at DATETIME(3) NOT NULL"
            + ") ENGINE=InnoDB";

    public static final String DEPARTMENTS = "CREATE TABLE IF NOT EXISTS departments ("
            + " department_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " name VARCHAR(100) NOT NULL UNIQUE,"
            + " type VARCHAR(30) NOT NULL,"
            + " contact_info VARCHAR(100),"
            + " created_at DATETIME(3) NOT NULL"
            + ") ENGINE=InnoDB";

    public static final String RESOURCES = "CREATE TABLE IF NOT EXISTS resources ("
            + " resource_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " department_id INT NOT NULL,"
            + " name VARCHAR(100) NOT NULL,"
            + " quantity_available INT NOT NULL CHECK (quantity_available >= 0),"
            + " reorder_level INT NOT NULL DEFAULT 0,"
            + " updated_at DATETIME(3) NOT NULL,"
            + " UNIQUE KEY uq_resource (department_id, name),"
            + " CONSTRAINT fk_resource_department FOREIGN KEY (department_id) REFERENCES departments(department_id)"
            + ") ENGINE=InnoDB";

    public static final String DISASTER_REPORTS = "CREATE TABLE IF NOT EXISTS disaster_reports ("
            + " report_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " type VARCHAR(20) NOT NULL,"
            + " location VARCHAR(150) NOT NULL,"
            + " description TEXT NOT NULL,"
            + " reporter_name VARCHAR(100) NOT NULL,"
            + " reporter_phone_enc TEXT,"
            + " severity VARCHAR(20),"
            + " priority_score INT NOT NULL DEFAULT 0,"
            + " status VARCHAR(20) NOT NULL,"
            + " reported_by VARCHAR(50),"
            + " signature TEXT,"
            + " date_reported DATETIME(3) NOT NULL,"
            + " updated_at DATETIME(3) NOT NULL,"
            + " INDEX idx_report_priority (priority_score),"
            + " INDEX idx_report_status (status)"
            + ") ENGINE=InnoDB";

    public static final String VOLUNTEERS = "CREATE TABLE IF NOT EXISTS volunteers ("
            + " volunteer_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " name VARCHAR(100) NOT NULL,"
            + " phone_enc TEXT,"
            + " skill VARCHAR(100),"
            + " available BOOLEAN NOT NULL DEFAULT TRUE,"
            + " registered_at DATETIME(3) NOT NULL"
            + ") ENGINE=InnoDB";

    public static final String RESPONSE_TASKS = "CREATE TABLE IF NOT EXISTS response_tasks ("
            + " task_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " report_id INT NOT NULL,"
            + " department_id INT NOT NULL,"
            + " volunteer_id INT NULL,"
            + " description VARCHAR(255) NOT NULL,"
            + " priority INT NOT NULL,"
            + " status VARCHAR(20) NOT NULL,"
            + " created_at DATETIME(3) NOT NULL,"
            + " updated_at DATETIME(3) NOT NULL,"
            + " CONSTRAINT fk_task_report FOREIGN KEY (report_id) REFERENCES disaster_reports(report_id),"
            + " CONSTRAINT fk_task_department FOREIGN KEY (department_id) REFERENCES departments(department_id),"
            + " CONSTRAINT fk_task_volunteer FOREIGN KEY (volunteer_id) REFERENCES volunteers(volunteer_id)"
            + ") ENGINE=InnoDB";

    public static final String RESOURCE_ALLOCATIONS = "CREATE TABLE IF NOT EXISTS resource_allocations ("
            + " allocation_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " task_id INT NOT NULL,"
            + " resource_id INT NOT NULL,"
            + " quantity INT NOT NULL,"
            + " allocated_by VARCHAR(50) NOT NULL,"
            + " allocated_at DATETIME(3) NOT NULL,"
            + " CONSTRAINT fk_alloc_task FOREIGN KEY (task_id) REFERENCES response_tasks(task_id),"
            + " CONSTRAINT fk_alloc_resource FOREIGN KEY (resource_id) REFERENCES resources(resource_id)"
            + ") ENGINE=InnoDB";

    public static final String SHELTERS = "CREATE TABLE IF NOT EXISTS shelters ("
            + " shelter_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " name VARCHAR(100) NOT NULL UNIQUE,"
            + " location VARCHAR(150) NOT NULL,"
            + " capacity INT NOT NULL CHECK (capacity > 0),"
            + " occupancy INT NOT NULL DEFAULT 0,"
            + " contact VARCHAR(100),"
            + " closed BOOLEAN NOT NULL DEFAULT FALSE,"
            + " updated_at DATETIME(3) NOT NULL"
            + ") ENGINE=InnoDB";

    public static final String EVACUATION_ALERTS = "CREATE TABLE IF NOT EXISTS evacuation_alerts ("
            + " alert_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " report_id INT NOT NULL,"
            + " zone VARCHAR(150) NOT NULL,"
            + " level VARCHAR(30) NOT NULL,"
            + " message TEXT NOT NULL,"
            + " shelter_id INT NULL,"
            + " status VARCHAR(20) NOT NULL,"
            + " issued_by VARCHAR(50) NOT NULL,"
            + " issued_at DATETIME(3) NOT NULL,"
            + " lifted_at DATETIME(3) NULL,"
            + " CONSTRAINT fk_alert_report FOREIGN KEY (report_id) REFERENCES disaster_reports(report_id),"
            + " CONSTRAINT fk_alert_shelter FOREIGN KEY (shelter_id) REFERENCES shelters(shelter_id)"
            + ") ENGINE=InnoDB";

    public static final String DAMAGE_ASSESSMENTS = "CREATE TABLE IF NOT EXISTS damage_assessments ("
            + " assessment_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " report_id INT NOT NULL,"
            + " category VARCHAR(30) NOT NULL,"
            + " damage_level VARCHAR(20) NOT NULL,"
            + " estimated_cost DECIMAL(14,2) NOT NULL,"
            + " affected_people INT NOT NULL,"
            + " notes TEXT,"
            + " assessed_by VARCHAR(50) NOT NULL,"
            + " assessed_at DATETIME(3) NOT NULL,"
            + " CONSTRAINT fk_damage_report FOREIGN KEY (report_id) REFERENCES disaster_reports(report_id)"
            + ") ENGINE=InnoDB";

    public static final String RESTORATION_JOBS = "CREATE TABLE IF NOT EXISTS restoration_jobs ("
            + " job_id INT AUTO_INCREMENT PRIMARY KEY,"
            + " assessment_id INT NOT NULL,"
            + " department_id INT NOT NULL,"
            + " description VARCHAR(255) NOT NULL,"
            + " priority INT NOT NULL,"
            + " progress INT NOT NULL DEFAULT 0,"
            + " status VARCHAR(20) NOT NULL,"
            + " estimated_days INT NOT NULL,"
            + " created_at DATETIME(3) NOT NULL,"
            + " updated_at DATETIME(3) NOT NULL,"
            + " CONSTRAINT fk_job_assessment FOREIGN KEY (assessment_id) REFERENCES damage_assessments(assessment_id),"
            + " CONSTRAINT fk_job_department FOREIGN KEY (department_id) REFERENCES departments(department_id)"
            + ") ENGINE=InnoDB";

    public static final String AUDIT_LOG = "CREATE TABLE IF NOT EXISTS audit_log ("
            + " audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + " ts DATETIME(3) NOT NULL,"
            + " username VARCHAR(50) NOT NULL,"
            + " action VARCHAR(40) NOT NULL,"
            + " details TEXT NOT NULL,"
            + " prev_hash CHAR(64) NOT NULL,"
            + " entry_hash CHAR(64) NOT NULL,"
            + " signature TEXT NOT NULL"
            + ") ENGINE=InnoDB";

    /** All tables in dependency order (parents before children). */
    public static final List<String> ALL_TABLES = List.of(USERS, DEPARTMENTS, RESOURCES, DISASTER_REPORTS,
            VOLUNTEERS, RESPONSE_TASKS, RESOURCE_ALLOCATIONS, SHELTERS, EVACUATION_ALERTS,
            DAMAGE_ASSESSMENTS, RESTORATION_JOBS, AUDIT_LOG);

    /** Table names in dependency order. */
    public static final List<String> TABLE_NAMES = List.of("users", "departments", "resources",
            "disaster_reports", "volunteers", "response_tasks", "resource_allocations", "shelters",
            "evacuation_alerts", "damage_assessments", "restoration_jobs", "audit_log");
}
