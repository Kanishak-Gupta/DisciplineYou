package com.disciplineyou.model;

import java.time.LocalDate;

public class Semester {
    private int id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;

    public Semester() {}

    public Semester(String name, LocalDate startDate, LocalDate endDate, boolean active) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return name;
    }
}
