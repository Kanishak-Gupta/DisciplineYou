package com.disciplineyou.model;

public class Subject {
    private int id;
    private int semesterId;
    private String name;
    private String shortCode;
    private String faculty;

    public Subject() {}

    public Subject(int semesterId, String name, String shortCode, String faculty) {
        this.semesterId = semesterId;
        this.name = name;
        this.shortCode = shortCode;
        this.faculty = faculty;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSemesterId() { return semesterId; }
    public void setSemesterId(int semesterId) { this.semesterId = semesterId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    @Override
    public String toString() {
        return shortCode != null && !shortCode.isEmpty() ? shortCode : name;
    }
}
