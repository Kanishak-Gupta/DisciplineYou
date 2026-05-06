package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.DailyLog;
import com.disciplineyou.model.StudyLog;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DailyLogDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    // ======================== DAILY LOG ========================

    public DailyLog getOrCreateForDate(LocalDate date) {
        DailyLog log = getByDate(date);
        if (log == null) {
            log = new DailyLog(date);
            insert(log);
        }
        return log;
    }

    public DailyLog getByDate(LocalDate date) {
        String sql = "SELECT * FROM daily_logs WHERE log_date=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapDailyLog(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting daily log", e);
        }
        return null;
    }

    public DailyLog insert(DailyLog log) {
        String sql = "INSERT INTO daily_logs (log_date, social_media_mins, movie_mins, college_mins, notes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getLogDate().toString());
            ps.setInt(2, log.getSocialMediaMins());
            ps.setInt(3, log.getMovieMins());
            ps.setInt(4, log.getCollegeMins());
            ps.setString(5, log.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) log.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting daily log", e);
        }
        return log;
    }

    public void update(DailyLog log) {
        String sql = "UPDATE daily_logs SET social_media_mins=?, movie_mins=?, college_mins=?, notes=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, log.getSocialMediaMins());
            ps.setInt(2, log.getMovieMins());
            ps.setInt(3, log.getCollegeMins());
            ps.setString(4, log.getNotes());
            ps.setInt(5, log.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating daily log", e);
        }
    }

    // ======================== STUDY LOGS ========================

    public void saveStudyLogs(int dailyLogId, List<StudyLog> studyLogs) {
        // Delete existing study logs for this daily log
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM study_logs WHERE daily_log_id=?")) {
            ps.setInt(1, dailyLogId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error clearing study logs", e);
        }

        // Insert new ones
        String sql = "INSERT INTO study_logs (daily_log_id, subject_id, duration_mins) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            for (StudyLog sl : studyLogs) {
                if (sl.getDurationMins() > 0) {
                    ps.setInt(1, dailyLogId);
                    ps.setInt(2, sl.getSubjectId());
                    ps.setInt(3, sl.getDurationMins());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving study logs", e);
        }
    }

    public List<StudyLog> getStudyLogs(int dailyLogId) {
        List<StudyLog> list = new ArrayList<>();
        String sql = "SELECT sl.*, s.name as subject_name FROM study_logs sl "
                   + "JOIN subjects s ON sl.subject_id = s.id WHERE sl.daily_log_id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, dailyLogId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StudyLog sl = new StudyLog();
                    sl.setId(rs.getInt("id"));
                    sl.setDailyLogId(rs.getInt("daily_log_id"));
                    sl.setSubjectId(rs.getInt("subject_id"));
                    sl.setDurationMins(rs.getInt("duration_mins"));
                    sl.setSubjectName(rs.getString("subject_name"));
                    list.add(sl);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting study logs", e);
        }
        return list;
    }

    private DailyLog mapDailyLog(ResultSet rs) throws SQLException {
        DailyLog log = new DailyLog();
        log.setId(rs.getInt("id"));
        log.setLogDate(LocalDate.parse(rs.getString("log_date")));
        log.setSocialMediaMins(rs.getInt("social_media_mins"));
        log.setMovieMins(rs.getInt("movie_mins"));
        log.setCollegeMins(rs.getInt("college_mins"));
        log.setNotes(rs.getString("notes"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) log.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        return log;
    }
}
