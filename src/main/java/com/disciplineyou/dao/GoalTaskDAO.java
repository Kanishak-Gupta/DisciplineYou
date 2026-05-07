package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.GoalTask;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for goal-based tasks with hierarchical subtask support.
 */
public class GoalTaskDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public GoalTask insert(GoalTask task) {
        String sql = "INSERT INTO goal_tasks (title, description, start_date, target_date, priority, progress_percent, category, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStartDate().toString());
            ps.setString(4, task.getTargetDate().toString());
            ps.setString(5, task.getPriority());
            ps.setInt(6, task.getProgressPercent());
            ps.setString(7, task.getCategory());
            ps.setString(8, task.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) task.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting goal task", e);
        }
        return task;
    }

    public void update(GoalTask task) {
        String sql = "UPDATE goal_tasks SET title=?, description=?, start_date=?, target_date=?, priority=?, progress_percent=?, category=?, status=?, completed_at=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStartDate().toString());
            ps.setString(4, task.getTargetDate().toString());
            ps.setString(5, task.getPriority());
            ps.setInt(6, task.getProgressPercent());
            ps.setString(7, task.getCategory());
            ps.setString(8, task.getStatus());
            ps.setString(9, task.getCompletedAt() != null ? task.getCompletedAt().toString() : null);
            ps.setInt(10, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating goal task", e);
        }
    }

    public void delete(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM goal_tasks WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting goal task", e);
        }
    }

    public GoalTask getById(int id) {
        String sql = "SELECT * FROM goal_tasks WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapGoalTask(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting goal task", e);
        }
        return null;
    }

    public List<GoalTask> getAllActive() {
        return getByStatus("ACTIVE");
    }

    public List<GoalTask> getCompleted() {
        return getByStatus("COMPLETED");
    }

    public List<GoalTask> getArchived() {
        return getByStatus("ARCHIVED");
    }

    public List<GoalTask> getByStatus(String status) {
        List<GoalTask> list = new ArrayList<>();
        String sql = "SELECT * FROM goal_tasks WHERE status=? ORDER BY " +
                "CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'LOW' THEN 2 END, target_date";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGoalTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting goal tasks", e);
        }
        return list;
    }

    public List<GoalTask> getAll() {
        List<GoalTask> list = new ArrayList<>();
        String sql = "SELECT * FROM goal_tasks ORDER BY " +
                "CASE status WHEN 'ACTIVE' THEN 0 WHEN 'COMPLETED' THEN 1 WHEN 'ARCHIVED' THEN 2 END, " +
                "CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 WHEN 'LOW' THEN 2 END, target_date";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapGoalTask(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error getting all goal tasks", e);
        }
        return list;
    }

    public List<GoalTask> getByCategory(String category) {
        List<GoalTask> list = new ArrayList<>();
        String sql = "SELECT * FROM goal_tasks WHERE category=? ORDER BY target_date";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGoalTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting goal tasks by category", e);
        }
        return list;
    }

    public List<GoalTask> getOverdueTasks() {
        List<GoalTask> list = new ArrayList<>();
        String sql = "SELECT * FROM goal_tasks WHERE status='ACTIVE' AND target_date < ? ORDER BY target_date";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGoalTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting overdue tasks", e);
        }
        return list;
    }

    public List<GoalTask> getUpcomingDeadlines(int withinDays) {
        List<GoalTask> list = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate limit = now.plusDays(withinDays);
        String sql = "SELECT * FROM goal_tasks WHERE status='ACTIVE' AND target_date >= ? AND target_date <= ? ORDER BY target_date";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, now.toString());
            ps.setString(2, limit.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGoalTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting upcoming deadlines", e);
        }
        return list;
    }

    public void updateProgress(int taskId, int progressPercent) {
        try (PreparedStatement ps = getConn().prepareStatement("UPDATE goal_tasks SET progress_percent=? WHERE id=?")) {
            ps.setInt(1, progressPercent);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating progress", e);
        }
    }

    public void markCompleted(int taskId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "UPDATE goal_tasks SET status='COMPLETED', progress_percent=100, completed_at=? WHERE id=?")) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error marking task completed", e);
        }
    }

    public void archive(int taskId) {
        try (PreparedStatement ps = getConn().prepareStatement("UPDATE goal_tasks SET status='ARCHIVED' WHERE id=?")) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error archiving task", e);
        }
    }

    /** Get distinct categories for filtering */
    public List<String> getAllCategories() {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM goal_tasks WHERE category IS NOT NULL AND category != '' ORDER BY category";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) cats.add(rs.getString("category"));
        } catch (SQLException e) {
            throw new RuntimeException("Error getting categories", e);
        }
        return cats;
    }

    /** Get completed tasks filtered by month */
    public List<GoalTask> getCompletedByMonth(int year, int month) {
        List<GoalTask> list = new ArrayList<>();
        String startDate = String.format("%04d-%02d-01", year, month);
        String endDate = String.format("%04d-%02d-31", year, month);
        String sql = "SELECT * FROM goal_tasks WHERE status IN ('COMPLETED','ARCHIVED') AND completed_at >= ? AND completed_at <= ? ORDER BY completed_at DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGoalTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting completed tasks by month", e);
        }
        return list;
    }

    private GoalTask mapGoalTask(ResultSet rs) throws SQLException {
        GoalTask t = new GoalTask();
        t.setId(rs.getInt("id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setStartDate(LocalDate.parse(rs.getString("start_date")));
        t.setTargetDate(LocalDate.parse(rs.getString("target_date")));
        t.setPriority(rs.getString("priority"));
        t.setProgressPercent(rs.getInt("progress_percent"));
        t.setCategory(rs.getString("category"));
        t.setStatus(rs.getString("status"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) t.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        String completedAt = rs.getString("completed_at");
        if (completedAt != null) t.setCompletedAt(LocalDate.parse(completedAt));
        return t;
    }
}
