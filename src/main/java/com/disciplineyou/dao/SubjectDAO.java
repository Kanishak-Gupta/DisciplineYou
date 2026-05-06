package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.Semester;
import com.disciplineyou.model.Subject;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SubjectDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    // ======================== SEMESTER OPERATIONS ========================

    public Semester insertSemester(Semester semester) {
        String sql = "INSERT INTO semesters (name, start_date, end_date, is_active) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, semester.getName());
            ps.setString(2, semester.getStartDate().toString());
            ps.setString(3, semester.getEndDate().toString());
            ps.setInt(4, semester.isActive() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) semester.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting semester", e);
        }
        return semester;
    }

    public void updateSemester(Semester semester) {
        String sql = "UPDATE semesters SET name=?, start_date=?, end_date=?, is_active=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, semester.getName());
            ps.setString(2, semester.getStartDate().toString());
            ps.setString(3, semester.getEndDate().toString());
            ps.setInt(4, semester.isActive() ? 1 : 0);
            ps.setInt(5, semester.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating semester", e);
        }
    }

    public void setActiveSemester(int semesterId) {
        try {
            // Deactivate all
            try (PreparedStatement ps = getConn().prepareStatement("UPDATE semesters SET is_active=0")) {
                ps.executeUpdate();
            }
            // Activate the selected one
            try (PreparedStatement ps = getConn().prepareStatement("UPDATE semesters SET is_active=1 WHERE id=?")) {
                ps.setInt(1, semesterId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error setting active semester", e);
        }
    }

    public Semester getActiveSemester() {
        String sql = "SELECT * FROM semesters WHERE is_active = 1 LIMIT 1";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return mapSemester(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Error getting active semester", e);
        }
        return null;
    }

    public List<Semester> getAllSemesters() {
        List<Semester> list = new ArrayList<>();
        String sql = "SELECT * FROM semesters ORDER BY start_date DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapSemester(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error getting semesters", e);
        }
        return list;
    }

    public void deleteSemester(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM semesters WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting semester", e);
        }
    }

    private Semester mapSemester(ResultSet rs) throws SQLException {
        Semester s = new Semester();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setStartDate(LocalDate.parse(rs.getString("start_date")));
        s.setEndDate(LocalDate.parse(rs.getString("end_date")));
        s.setActive(rs.getInt("is_active") == 1);
        return s;
    }

    // ======================== SUBJECT OPERATIONS ========================

    public Subject insertSubject(Subject subject) {
        String sql = "INSERT INTO subjects (semester_id, name, short_code, faculty) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, subject.getSemesterId());
            ps.setString(2, subject.getName());
            ps.setString(3, subject.getShortCode());
            ps.setString(4, subject.getFaculty());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) subject.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting subject", e);
        }
        return subject;
    }

    public void updateSubject(Subject subject) {
        String sql = "UPDATE subjects SET name=?, short_code=?, faculty=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, subject.getName());
            ps.setString(2, subject.getShortCode());
            ps.setString(3, subject.getFaculty());
            ps.setInt(4, subject.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating subject", e);
        }
    }

    public List<Subject> getSubjectsBySemester(int semesterId) {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT * FROM subjects WHERE semester_id=? ORDER BY name";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, semesterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSubject(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting subjects", e);
        }
        return list;
    }

    public Subject getSubjectById(int id) {
        String sql = "SELECT * FROM subjects WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapSubject(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting subject", e);
        }
        return null;
    }

    public void deleteSubject(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM subjects WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting subject", e);
        }
    }

    private Subject mapSubject(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setId(rs.getInt("id"));
        s.setSemesterId(rs.getInt("semester_id"));
        s.setName(rs.getString("name"));
        s.setShortCode(rs.getString("short_code"));
        s.setFaculty(rs.getString("faculty"));
        return s;
    }
}
