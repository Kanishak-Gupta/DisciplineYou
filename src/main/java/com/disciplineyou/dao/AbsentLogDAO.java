package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.AbsentLog;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class AbsentLogDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public AbsentLog insert(AbsentLog log) {
        String sql = "INSERT INTO absent_log (subject_id, absent_date, reason, notes) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getSubjectId());
            ps.setString(2, log.getAbsentDate().toString());
            ps.setString(3, log.getReason());
            ps.setString(4, log.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) log.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting absent log", e);
        }
        return log;
    }

    public void delete(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM absent_log WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting absent log", e);
        }
    }

    public List<AbsentLog> getAll() {
        List<AbsentLog> list = new ArrayList<>();
        String sql = "SELECT a.*, s.name as subject_name FROM absent_log a JOIN subjects s ON a.subject_id = s.id ORDER BY a.absent_date DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapLog(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error getting absent logs", e);
        }
        return list;
    }

    public List<AbsentLog> getBySubject(int subjectId) {
        List<AbsentLog> list = new ArrayList<>();
        String sql = "SELECT a.*, s.name as subject_name FROM absent_log a JOIN subjects s ON a.subject_id = s.id WHERE a.subject_id=? ORDER BY a.absent_date DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLog(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting absent logs by subject", e);
        }
        return list;
    }

    public List<AbsentLog> getByMonth(int year, int month) {
        List<AbsentLog> list = new ArrayList<>();
        String start = String.format("%04d-%02d-01", year, month);
        String end = String.format("%04d-%02d-31", year, month);
        String sql = "SELECT a.*, s.name as subject_name FROM absent_log a JOIN subjects s ON a.subject_id = s.id WHERE a.absent_date >= ? AND a.absent_date <= ? ORDER BY a.absent_date DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, start);
            ps.setString(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLog(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting absent logs by month", e);
        }
        return list;
    }

    public int getAbsenceCount(int subjectId) {
        String sql = "SELECT COUNT(*) FROM absent_log WHERE subject_id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting absences", e);
        }
        return 0;
    }

    /** Subject-wise absence count */
    public Map<String, Integer> getSubjectWiseAbsenceCount() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT s.name, COUNT(*) as cnt FROM absent_log a JOIN subjects s ON a.subject_id = s.id GROUP BY s.name ORDER BY cnt DESC";
        try (Statement stmt = getConn().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString("name"), rs.getInt("cnt"));
        } catch (SQLException e) {
            throw new RuntimeException("Error getting subject-wise absence", e);
        }
        return map;
    }

    private AbsentLog mapLog(ResultSet rs) throws SQLException {
        AbsentLog l = new AbsentLog();
        l.setId(rs.getInt("id"));
        l.setSubjectId(rs.getInt("subject_id"));
        l.setAbsentDate(LocalDate.parse(rs.getString("absent_date")));
        l.setReason(rs.getString("reason"));
        l.setNotes(rs.getString("notes"));
        try { l.setSubjectName(rs.getString("subject_name")); } catch (SQLException ex) {}
        return l;
    }
}
