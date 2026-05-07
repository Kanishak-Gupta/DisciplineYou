package com.disciplineyou.model;

public class TimetableEntry {
    private int id;
    private int semesterId;
    private int dayOfWeek;     // 1=Monday ... 7=Sunday
    private String startTime;  // "09:00"
    private String endTime;    // "10:00"
    private int subjectId;
    private String room;
    private String faculty;
    private String notes;

    // Transient display fields
    private String subjectName;
    private String subjectShortCode;

    public TimetableEntry() {}

    public TimetableEntry(int semesterId, int dayOfWeek, String startTime, String endTime,
                           int subjectId, String room, String faculty) {
        this.semesterId = semesterId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subjectId = subjectId;
        this.room = room;
        this.faculty = faculty;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSemesterId() { return semesterId; }
    public void setSemesterId(int semesterId) { this.semesterId = semesterId; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSubjectShortCode() { return subjectShortCode; }
    public void setSubjectShortCode(String subjectShortCode) { this.subjectShortCode = subjectShortCode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    /** Get a human-readable day name */
    public String getDayName() {
        switch (dayOfWeek) {
            case 1: return "Monday";
            case 2: return "Tuesday";
            case 3: return "Wednesday";
            case 4: return "Thursday";
            case 5: return "Friday";
            case 6: return "Saturday";
            case 7: return "Sunday";
            default: return "Unknown";
        }
    }

    @Override
    public String toString() {
        String subj = subjectShortCode != null ? subjectShortCode : subjectName;
        return startTime + "-" + endTime + " " + subj + " (" + room + ")";
    }
}
