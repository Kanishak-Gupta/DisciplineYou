package com.disciplineyou.model;

import java.time.LocalDate;

public class TimetableOverride {
    private int id;
    private int timetableId;
    private LocalDate overrideDate;
    private String overrideType;  // CANCEL, ROOM_CHANGE, SUBSTITUTE
    private Integer newSubjectId;
    private String newRoom;
    private String newFaculty;
    private String note;

    public TimetableOverride() {}

    public TimetableOverride(int timetableId, LocalDate overrideDate, String overrideType) {
        this.timetableId = timetableId;
        this.overrideDate = overrideDate;
        this.overrideType = overrideType;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTimetableId() { return timetableId; }
    public void setTimetableId(int timetableId) { this.timetableId = timetableId; }

    public LocalDate getOverrideDate() { return overrideDate; }
    public void setOverrideDate(LocalDate overrideDate) { this.overrideDate = overrideDate; }

    public String getOverrideType() { return overrideType; }
    public void setOverrideType(String overrideType) { this.overrideType = overrideType; }

    public Integer getNewSubjectId() { return newSubjectId; }
    public void setNewSubjectId(Integer newSubjectId) { this.newSubjectId = newSubjectId; }

    public String getNewRoom() { return newRoom; }
    public void setNewRoom(String newRoom) { this.newRoom = newRoom; }

    public String getNewFaculty() { return newFaculty; }
    public void setNewFaculty(String newFaculty) { this.newFaculty = newFaculty; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
