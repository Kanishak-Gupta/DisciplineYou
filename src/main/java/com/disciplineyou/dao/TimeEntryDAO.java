package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.TimeEntry;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class TimeEntryDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public TimeEntry insert(TimeEntry entry) {
        String sql = "INSERT INTO time_entries (activity_name, duration_mins, entry_date, category) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entry.getActivityName());
            ps.setInt(2, entry.getDurationMins());
            ps.setString(3, entry.getEntryDate().toString());
            ps.setString(4, entry.getCategory());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) entry.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting time entry", e);
        }
        return entry;
    }

    public void delete(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM time_entries WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting time entry", e);
        }
    }

    public List<TimeEntry> getEntriesForDate(LocalDate date) {
        List<TimeEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM time_entries WHERE entry_date=? ORDER BY id";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapEntry(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting time entries for date", e);
        }
        return list;
    }

    public List<TimeEntry> getEntriesForDateRange(LocalDate start, LocalDate end) {
        List<TimeEntry> list = new ArrayList<>();
        String sql = "SELECT * FROM time_entries WHERE entry_date >= ? AND entry_date <= ? ORDER BY entry_date, id";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, start.toString());
            ps.setString(2, end.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapEntry(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting time entries for range", e);
        }
        return list;
    }

    /** Get total minutes by activity for pie chart */
    public Map<String, Integer> getTotalByActivity(LocalDate start, LocalDate end) {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT activity_name, SUM(duration_mins) as total FROM time_entries WHERE entry_date >= ? AND entry_date <= ? GROUP BY activity_name ORDER BY total DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, start.toString());
            ps.setString(2, end.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("activity_name"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting activity totals", e);
        }
        return map;
    }

    /** Get total minutes by category */
    public Map<String, Integer> getTotalByCategory(LocalDate start, LocalDate end) {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT COALESCE(category,'Uncategorized') as cat, SUM(duration_mins) as total FROM time_entries WHERE entry_date >= ? AND entry_date <= ? GROUP BY cat ORDER BY total DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, start.toString());
            ps.setString(2, end.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("cat"), rs.getInt("total"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting category totals", e);
        }
        return map;
    }

    /** Get daily totals for trend chart */
    public Map<LocalDate, Integer> getDailyTotals(LocalDate start, LocalDate end) {
        Map<LocalDate, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT entry_date, SUM(duration_mins) as total FROM time_entries WHERE entry_date >= ? AND entry_date <= ? GROUP BY entry_date ORDER BY entry_date";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, start.toString());
            ps.setString(2, end.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(LocalDate.parse(rs.getString("entry_date")), rs.getInt("total"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting daily totals", e);
        }
        return map;
    }

    public List<String> getAllCategories() {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM time_entries WHERE category IS NOT NULL AND category != '' ORDER BY category";
        try (Statement stmt = getConn().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) cats.add(rs.getString("category"));
        } catch (SQLException e) {
            throw new RuntimeException("Error getting categories", e);
        }
        return cats;
    }

    private TimeEntry mapEntry(ResultSet rs) throws SQLException {
        TimeEntry e = new TimeEntry();
        e.setId(rs.getInt("id"));
        e.setActivityName(rs.getString("activity_name"));
        e.setDurationMins(rs.getInt("duration_mins"));
        e.setEntryDate(LocalDate.parse(rs.getString("entry_date")));
        e.setCategory(rs.getString("category"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) e.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        return e;
    }
}
