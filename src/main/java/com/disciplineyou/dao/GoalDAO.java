package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.Goal;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GoalDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public Goal insert(Goal goal) {
        String sql = "INSERT INTO goals (parent_id, type, title, description, target_date, original_date, status, shift_count) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (goal.getParentId() != null) ps.setInt(1, goal.getParentId()); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, goal.getType());
            ps.setString(3, goal.getTitle());
            ps.setString(4, goal.getDescription());
            ps.setString(5, goal.getTargetDate().toString());
            ps.setString(6, goal.getOriginalDate() != null ? goal.getOriginalDate().toString() : goal.getTargetDate().toString());
            ps.setString(7, goal.getStatus());
            ps.setInt(8, goal.getShiftCount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) goal.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting goal", e);
        }
        return goal;
    }

    public void update(Goal goal) {
        String sql = "UPDATE goals SET title=?, description=?, target_date=?, status=?, shift_count=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, goal.getTitle());
            ps.setString(2, goal.getDescription());
            ps.setString(3, goal.getTargetDate().toString());
            ps.setString(4, goal.getStatus());
            ps.setInt(5, goal.getShiftCount());
            ps.setInt(6, goal.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating goal", e);
        }
    }

    public void delete(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM goals WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting goal", e);
        }
    }

    /** Get all top-level goals of a given type (MONTHLY/WEEKLY/DAILY) that have no parent */
    public List<Goal> getTopLevelGoalsByType(String type) {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE type=? AND parent_id IS NULL ORDER BY target_date, created_at";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGoal(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting goals", e);
        }
        return list;
    }

    /** Get child goals (e.g., weekly goals under a monthly goal) */
    public List<Goal> getChildGoals(int parentId) {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE parent_id=? ORDER BY target_date, created_at";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGoal(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting child goals", e);
        }
        return list;
    }

    /** Get all goals by type regardless of parent (for flat views) */
    public List<Goal> getAllGoalsByType(String type) {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE type=? ORDER BY "
                   + "CASE WHEN status='SHIFTED' THEN 0 WHEN status='PENDING' THEN 1 ELSE 2 END, "
                   + "target_date, created_at";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGoal(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting goals by type", e);
        }
        return list;
    }

    /** Get daily goals for a specific date */
    public List<Goal> getDailyGoalsForDate(LocalDate date) {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE type='DAILY' AND target_date=? ORDER BY "
                   + "CASE WHEN status='SHIFTED' THEN 0 WHEN status='PENDING' THEN 1 ELSE 2 END, created_at";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapGoal(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting daily goals", e);
        }
        return list;
    }

    /** Toggle a goal's completion status */
    public void toggleComplete(int goalId) {
        String sql = "UPDATE goals SET status = CASE WHEN status='COMPLETED' THEN 'PENDING' ELSE 'COMPLETED' END WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error toggling goal", e);
        }
    }

    /** Auto-shift all overdue PENDING/SHIFTED daily goals to today */
    public List<Goal> autoShiftOverdueGoals() {
        List<Goal> shifted = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String sql = "SELECT * FROM goals WHERE type='DAILY' AND status IN ('PENDING','SHIFTED') AND target_date < ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, today.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Goal g = mapGoal(rs);
                    g.setTargetDate(today);
                    g.setShiftCount(g.getShiftCount() + 1);
                    g.setStatus("SHIFTED");
                    update(g);
                    shifted.add(g);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error auto-shifting goals", e);
        }
        return shifted;
    }

    private Goal mapGoal(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getInt("id"));
        int parentId = rs.getInt("parent_id");
        g.setParentId(rs.wasNull() ? null : parentId);
        g.setType(rs.getString("type"));
        g.setTitle(rs.getString("title"));
        g.setDescription(rs.getString("description"));
        g.setTargetDate(LocalDate.parse(rs.getString("target_date")));
        String origDate = rs.getString("original_date");
        if (origDate != null) g.setOriginalDate(LocalDate.parse(origDate));
        g.setStatus(rs.getString("status"));
        g.setShiftCount(rs.getInt("shift_count"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) g.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
        return g;
    }
}
