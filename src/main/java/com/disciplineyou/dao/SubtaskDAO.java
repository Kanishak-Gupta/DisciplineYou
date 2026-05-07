package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.Subtask;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SubtaskDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public Subtask insert(Subtask subtask) {
        String sql = "INSERT INTO subtasks (goal_task_id, week_number, day_date, title, completed) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, subtask.getGoalTaskId());
            ps.setInt(2, subtask.getWeekNumber());
            ps.setString(3, subtask.getDayDate().toString());
            ps.setString(4, subtask.getTitle());
            ps.setInt(5, subtask.isCompleted() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) subtask.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting subtask", e);
        }
        return subtask;
    }

    public void delete(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM subtasks WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting subtask", e);
        }
    }

    public void toggleCompletion(int subtaskId) {
        String sql = "UPDATE subtasks SET completed = CASE WHEN completed=1 THEN 0 ELSE 1 END, " +
                     "completed_at = CASE WHEN completed=1 THEN NULL ELSE ? END WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setInt(2, subtaskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error toggling subtask", e);
        }
    }

    public List<Subtask> getSubtasksForGoalTask(int goalTaskId) {
        List<Subtask> list = new ArrayList<>();
        String sql = "SELECT * FROM subtasks WHERE goal_task_id=? ORDER BY week_number, day_date";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, goalTaskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSubtask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting subtasks", e);
        }
        return list;
    }

    public List<Subtask> getSubtasksForWeek(int goalTaskId, int weekNumber) {
        List<Subtask> list = new ArrayList<>();
        String sql = "SELECT * FROM subtasks WHERE goal_task_id=? AND week_number=? ORDER BY day_date";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, goalTaskId);
            ps.setInt(2, weekNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSubtask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting subtasks for week", e);
        }
        return list;
    }

    public List<Subtask> getIncompleteSubtasksBefore(LocalDate date) {
        List<Subtask> list = new ArrayList<>();
        String sql = "SELECT s.* FROM subtasks s JOIN goal_tasks gt ON s.goal_task_id = gt.id " +
                     "WHERE s.completed=0 AND s.day_date < ? AND gt.status='ACTIVE' ORDER BY s.day_date DESC LIMIT 50";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapSubtask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting incomplete subtasks", e);
        }
        return list;
    }

    public int[] getCompletionStats(int goalTaskId) {
        String sql = "SELECT COUNT(CASE WHEN completed=1 THEN 1 END) as done, COUNT(*) as total FROM subtasks WHERE goal_task_id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, goalTaskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new int[]{rs.getInt("done"), rs.getInt("total")};
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting completion stats", e);
        }
        return new int[]{0, 0};
    }

    public int[] getWeekCompletionStats(int goalTaskId, int weekNumber) {
        String sql = "SELECT COUNT(CASE WHEN completed=1 THEN 1 END) as done, COUNT(*) as total FROM subtasks WHERE goal_task_id=? AND week_number=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, goalTaskId);
            ps.setInt(2, weekNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new int[]{rs.getInt("done"), rs.getInt("total")};
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting week stats", e);
        }
        return new int[]{0, 0};
    }

    public List<Integer> getWeekNumbers(int goalTaskId) {
        List<Integer> weeks = new ArrayList<>();
        String sql = "SELECT DISTINCT week_number FROM subtasks WHERE goal_task_id=? ORDER BY week_number";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, goalTaskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) weeks.add(rs.getInt("week_number"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting week numbers", e);
        }
        return weeks;
    }

    public LocalDate[] getWeekDateRange(int goalTaskId, int weekNumber) {
        String sql = "SELECT MIN(day_date) as min_date, MAX(day_date) as max_date FROM subtasks WHERE goal_task_id=? AND week_number=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, goalTaskId);
            ps.setInt(2, weekNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String min = rs.getString("min_date");
                    String max = rs.getString("max_date");
                    if (min != null && max != null) {
                        return new LocalDate[]{LocalDate.parse(min), LocalDate.parse(max)};
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting week date range", e);
        }
        return null;
    }

    private Subtask mapSubtask(ResultSet rs) throws SQLException {
        Subtask s = new Subtask();
        s.setId(rs.getInt("id"));
        s.setGoalTaskId(rs.getInt("goal_task_id"));
        s.setWeekNumber(rs.getInt("week_number"));
        s.setDayDate(LocalDate.parse(rs.getString("day_date")));
        s.setTitle(rs.getString("title"));
        s.setCompleted(rs.getInt("completed") == 1);
        String completedAt = rs.getString("completed_at");
        if (completedAt != null) s.setCompletedAt(LocalDateTime.parse(completedAt.replace(" ", "T")));
        return s;
    }
}
