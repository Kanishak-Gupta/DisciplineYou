package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.ExtraClass;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExtraClassDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public ExtraClass insertExtraClass(ExtraClass ec) {
        String sql = "INSERT INTO extra_classes (semester_id, class_date, start_time, end_time, subject_id, room, faculty, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ec.getSemesterId());
            ps.setString(2, ec.getClassDate().toString());
            ps.setString(3, ec.getStartTime());
            ps.setString(4, ec.getEndTime());
            ps.setInt(5, ec.getSubjectId());
            ps.setString(6, ec.getRoom());
            ps.setString(7, ec.getFaculty());
            ps.setString(8, ec.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ec.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting extra class", e);
        }
        return ec;
    }

    public void deleteExtraClass(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM extra_classes WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting extra class", e);
        }
    }

    public void updateExtraClassStatus(int id, String status) {
        try (PreparedStatement ps = getConn().prepareStatement("UPDATE extra_classes SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating extra class status", e);
        }
    }

    public List<ExtraClass> getExtraClassesForDate(int semesterId, LocalDate date) {
        List<ExtraClass> list = new ArrayList<>();
        String sql = "SELECT e.*, s.name as subject_name, s.short_code as subject_short_code FROM extra_classes e JOIN subjects s ON e.subject_id = s.id WHERE e.semester_id=? AND e.class_date=? ORDER BY e.start_time";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, semesterId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapExtraClass(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting extra classes for date", e);
        }
        return list;
    }

    public List<ExtraClass> getExtraClassesForSubject(int subjectId, LocalDate startDate, LocalDate endDate) {
        List<ExtraClass> list = new ArrayList<>();
        String sql = "SELECT e.*, s.name as subject_name, s.short_code as subject_short_code FROM extra_classes e JOIN subjects s ON e.subject_id = s.id WHERE e.subject_id=? AND e.class_date >= ? AND e.class_date <= ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            ps.setString(2, startDate.toString());
            ps.setString(3, endDate.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapExtraClass(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting extra classes for subject", e);
        }
        return list;
    }

    private ExtraClass mapExtraClass(ResultSet rs) throws SQLException {
        ExtraClass ec = new ExtraClass();
        ec.setId(rs.getInt("id"));
        ec.setSemesterId(rs.getInt("semester_id"));
        ec.setClassDate(LocalDate.parse(rs.getString("class_date")));
        ec.setStartTime(rs.getString("start_time"));
        ec.setEndTime(rs.getString("end_time"));
        ec.setSubjectId(rs.getInt("subject_id"));
        ec.setRoom(rs.getString("room"));
        ec.setFaculty(rs.getString("faculty"));
        ec.setStatus(rs.getString("status"));
        try { ec.setSubjectName(rs.getString("subject_name")); } catch (SQLException ex) {}
        try { ec.setSubjectShortCode(rs.getString("subject_short_code")); } catch (SQLException ex) {}
        return ec;
    }
}
