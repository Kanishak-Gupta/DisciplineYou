package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.Attendance;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public void markAttendance(int timetableId, LocalDate date, String status) {
        String sql = "INSERT INTO attendance (timetable_id, class_date, status) VALUES (?, ?, ?) "
                   + "ON CONFLICT(timetable_id, class_date) DO UPDATE SET status=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, timetableId);
            ps.setString(2, date.toString());
            ps.setString(3, status);
            ps.setString(4, status);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error marking attendance", e);
        }
    }

    public Attendance getAttendance(int timetableId, LocalDate date) {
        String sql = "SELECT * FROM attendance WHERE timetable_id=? AND class_date=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, timetableId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAttendance(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting attendance", e);
        }
        return null;
    }

    /** Get attendance stats for a subject: [present, total] */
    public int[] getSubjectAttendanceStats(int subjectId) {
        String sql = "SELECT "
                   + "COUNT(CASE WHEN a.status='PRESENT' THEN 1 END) as present_count, "
                   + "COUNT(CASE WHEN a.status IN ('PRESENT','ABSENT') THEN 1 END) as total_count "
                   + "FROM attendance a JOIN timetable t ON a.timetable_id = t.id "
                   + "WHERE t.subject_id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("present_count"), rs.getInt("total_count")};
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting attendance stats", e);
        }
        return new int[]{0, 0};
    }

    public List<Attendance> getAttendanceForDate(LocalDate date) {
        List<Attendance> list = new ArrayList<>();
        String sql = "SELECT * FROM attendance WHERE class_date=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAttendance(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting attendance for date", e);
        }
        return list;
    }

    private Attendance mapAttendance(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setId(rs.getInt("id"));
        a.setTimetableId(rs.getInt("timetable_id"));
        a.setClassDate(LocalDate.parse(rs.getString("class_date")));
        a.setStatus(rs.getString("status"));
        return a;
    }
}
