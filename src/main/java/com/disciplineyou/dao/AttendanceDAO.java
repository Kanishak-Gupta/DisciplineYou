package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.Attendance;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;

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

    /** 
     * Get legacy attendance stats for a subject: [present, total]
     * (Deprecated, use getCalculatedAttendanceStats instead)
     */
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

    /**
     * Calculates attendance dynamically from semester start to a given end date.
     * Treats past unmarked classes as absent.
     * Returns: [present_count, total_count]
     */
    public int[] getCalculatedAttendanceStats(int subjectId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return new int[]{0, 0};
        }

        int present = 0;
        int total = 0;

        // 1. Get all timetable entries for this subject
        String sqlTimetable = "SELECT id, day_of_week FROM timetable WHERE subject_id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sqlTimetable)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int timetableId = rs.getInt("id");
                    int dowInt = rs.getInt("day_of_week");
                    DayOfWeek dow = DayOfWeek.of(dowInt == 7 ? 7 : dowInt); // 1=Mon, 7=Sun

                    // Calculate expected occurrences
                    long expectedOccurrences = countOccurrences(startDate, endDate, dow);

                    // Subtract cancellations
                    long cancellations = getCancelCount(timetableId, startDate, endDate);
                    long validClasses = expectedOccurrences - cancellations;

                    if (validClasses > 0) {
                        total += validClasses;
                        // Add actual present count
                        present += getPresentCount(timetableId, startDate, endDate);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculating attendance stats", e);
        }

        // 2. Add extra classes
        String sqlExtra = "SELECT COUNT(CASE WHEN status='PRESENT' THEN 1 END) as p, COUNT(*) as t FROM extra_classes WHERE subject_id=? AND class_date >= ? AND class_date <= ?";
        try (PreparedStatement ps = getConn().prepareStatement(sqlExtra)) {
            ps.setInt(1, subjectId);
            ps.setString(2, startDate.toString());
            ps.setString(3, endDate.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    present += rs.getInt("p");
                    total += rs.getInt("t");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error calculating extra classes stats", e);
        }

        return new int[]{present, total};
    }

    private long countOccurrences(LocalDate start, LocalDate end, DayOfWeek dow) {
        if (start.isAfter(end)) return 0;
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        long weeks = days / 7;
        long count = weeks;
        LocalDate current = start.plusDays(weeks * 7);
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek() == dow) count++;
            current = current.plusDays(1);
        }
        return count;
    }

    private long getCancelCount(int timetableId, LocalDate start, LocalDate end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM timetable_overrides WHERE timetable_id=? AND override_type='CANCEL' AND override_date >= ? AND override_date <= ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, timetableId);
            ps.setString(2, start.toString());
            ps.setString(3, end.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
    }

    private long getPresentCount(int timetableId, LocalDate start, LocalDate end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance WHERE timetable_id=? AND status='PRESENT' AND class_date >= ? AND class_date <= ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, timetableId);
            ps.setString(2, start.toString());
            ps.setString(3, end.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return 0;
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
