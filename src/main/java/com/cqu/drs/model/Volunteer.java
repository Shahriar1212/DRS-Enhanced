package com.cqu.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A registered volunteer who can be assigned to a {@link ResponseTask}.
 * The phone number is stored AES-encrypted in the database.
 */
public class Volunteer implements Serializable {

    private static final long serialVersionUID = 1L;

    private int volunteerId;
    private String name;
    private String phone;
    private String skill;
    private boolean available;
    private LocalDateTime registeredAt;

    public Volunteer() {
        this.available = true;
    }

    public Volunteer(String name, String phone, String skill) {
        this();
        this.name = name;
        this.phone = phone;
        this.skill = skill;
    }

    public int getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(int volunteerId) {
        this.volunteerId = volunteerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String toString() {
        return name + (skill == null || skill.isBlank() ? "" : " - " + skill);
    }
}
