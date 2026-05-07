package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.TimetableEntry;
import com.disciplineyou.model.TimetableOverride;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TimetableDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public TimetableEntry insertEntry(TimetableEntry entry) {
        String sql = "INSERT INTO timetable (semester_id, day_of_week, start_time, end_time, subject_id, room, faculty, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entry.getSemesterId());
            ps.setInt(2, entry.getDayOfWeek());
            ps.setString(3, entry.getStartTime());
            ps.setString(4, entry.getEndTime());
            ps.setInt(5, entry.getSubjectId());
            ps.setString(6, entry.getRoom());
            ps.setString(7, entry.getFaculty());
            ps.setString(8, entry.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) entry.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting timetable entry", e);
        }
        return entry;
    }

    public void updateEntry(TimetableEntry entry) {
        String sql = "UPDATE timetable SET day_of_week=?, start_time=?, end_time=?, subject_id=?, room=?, faculty=?, notes=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, entry.getDayOfWeek());
            ps.setString(2, entry.getStartTime());
            ps.setString(3, entry.getEndTime());
            ps.setInt(4, entry.getSubjectId());
            ps.setString(5, entry.getRoom());
            ps.setString(6, entry.getFaculty());
            ps.setString(7, entry.getNotes());
            ps.setInt(8, entry.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating timetable entry", e);
        }
    }

    public void deleteEntry(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM timetable WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting timetable entry", e);
        }
    }

    public void clearSemesterTimetable(int semesterId) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM timetable WHERE semester_id=?")) {
            ps.setInt(1, semesterId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error clearing timetable", e);
        }
    }

    public List<TimetableEntry> getEntriesForDay(int semesterId, int dayOfWeek) {
        List<TimetableEntry> list = new ArrayList<>();
        String sql = "SELECT t.*, s.name as subject_name, s.short_code as subject_short_code FROM timetable t JOIN subjects s ON t.subject_id = s.id WHERE t.semester_id=? AND t.day_of_week=? ORDER BY t.start_time";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, semesterId);
            ps.setInt(2, dayOfWeek);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapEntry(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting timetable entries", e);
        }
        return list;
    }

    public List<TimetableEntry> getAllEntries(int semesterId) {
        List<TimetableEntry> list = new ArrayList<>();
        String sql = "SELECT t.*, s.name as subject_name, s.short_code as subject_short_code FROM timetable t JOIN subjects s ON t.subject_id = s.id WHERE t.semester_id=? ORDER BY t.day_of_week, t.start_time";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, semesterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapEntry(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting all timetable entries", e);
        }
        return list;
    }

    public TimetableOverride insertOverride(TimetableOverride ov) {
        String sql = "INSERT INTO timetable_overrides (timetable_id, override_date, override_type, new_subject_id, new_room, new_faculty, note) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ov.getTimetableId());
            ps.setString(2, ov.getOverrideDate().toString());
            ps.setString(3, ov.getOverrideType());
            if (ov.getNewSubjectId() != null) ps.setInt(4, ov.getNewSubjectId()); else ps.setNull(4, Types.INTEGER);
            ps.setString(5, ov.getNewRoom());
            ps.setString(6, ov.getNewFaculty());
            ps.setString(7, ov.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ov.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting override", e);
        }
        return ov;
    }

    public void deleteOverride(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM timetable_overrides WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting override", e);
        }
    }

    public List<TimetableOverride> getOverridesForDate(LocalDate date) {
        List<TimetableOverride> list = new ArrayList<>();
        String sql = "SELECT * FROM timetable_overrides WHERE override_date=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapOverride(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting overrides", e);
        }
        return list;
    }

    private TimetableEntry mapEntry(ResultSet rs) throws SQLException {
        TimetableEntry e = new TimetableEntry();
        e.setId(rs.getInt("id"));
        e.setSemesterId(rs.getInt("semester_id"));
        e.setDayOfWeek(rs.getInt("day_of_week"));
        e.setStartTime(rs.getString("start_time"));
        e.setEndTime(rs.getString("end_time"));
        e.setSubjectId(rs.getInt("subject_id"));
        e.setRoom(rs.getString("room"));
        e.setFaculty(rs.getString("faculty"));
        try { e.setNotes(rs.getString("notes")); } catch (SQLException ex) {}
        try { e.setSubjectName(rs.getString("subject_name")); } catch (SQLException ex) {}
        try { e.setSubjectShortCode(rs.getString("subject_short_code")); } catch (SQLException ex) {}
        return e;
    }

    private TimetableOverride mapOverride(ResultSet rs) throws SQLException {
        TimetableOverride o = new TimetableOverride();
        o.setId(rs.getInt("id"));
        o.setTimetableId(rs.getInt("timetable_id"));
        o.setOverrideDate(LocalDate.parse(rs.getString("override_date")));
        o.setOverrideType(rs.getString("override_type"));
        int newSubId = rs.getInt("new_subject_id");
        o.setNewSubjectId(rs.wasNull() ? null : newSubId);
        o.setNewRoom(rs.getString("new_room"));
        o.setNewFaculty(rs.getString("new_faculty"));
        o.setNote(rs.getString("note"));
        return o;
    }
}
