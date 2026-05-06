package com.disciplineyou.dao;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.Task;
import com.disciplineyou.model.TaskCompletion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for tasks and task completions.
 */
public class TaskDAO {

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    // ──────────── TASKS ────────────

    public void insertTask(Task task) {
        String sql = "INSERT INTO tasks (title, week_start_date, day_specific) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getWeekStartDate().toString());
            ps.setInt(3, task.getDaySpecific());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) task.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Task> getTasksForWeek(LocalDate weekStart) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE week_start_date = ? ORDER BY id";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, weekStart.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapTask(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    /** Get all distinct week_start_dates that have tasks, for listing months/weeks */
    public List<LocalDate> getAllWeekStarts() {
        List<LocalDate> weeks = new ArrayList<>();
        String sql = "SELECT DISTINCT week_start_date FROM tasks ORDER BY week_start_date";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                weeks.add(LocalDate.parse(rs.getString("week_start_date")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return weeks;
    }

    public void deleteTask(int taskId) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTaskTitle(int taskId, String newTitle) {
        try (PreparedStatement ps = getConn().prepareStatement("UPDATE tasks SET title = ? WHERE id = ?")) {
            ps.setString(1, newTitle);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ──────────── COMPLETIONS ────────────

    public void setCompletion(int taskId, LocalDate date, boolean completed) {
        // Upsert
        String sql = """
            INSERT INTO task_completions (task_id, completion_date, completed) VALUES (?, ?, ?)
            ON CONFLICT(task_id, completion_date) DO UPDATE SET completed = excluded.completed
        """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setString(2, date.toString());
            ps.setInt(3, completed ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isCompleted(int taskId, LocalDate date) {
        String sql = "SELECT completed FROM task_completions WHERE task_id = ? AND completion_date = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("completed") == 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Get completion statuses for all tasks of a week at once for efficiency */
    public List<TaskCompletion> getCompletionsForWeek(LocalDate weekStart) {
        List<TaskCompletion> completions = new ArrayList<>();
        LocalDate weekEnd = weekStart.plusDays(6);
        String sql = """
            SELECT tc.* FROM task_completions tc
            JOIN tasks t ON tc.task_id = t.id
            WHERE t.week_start_date = ? AND tc.completion_date BETWEEN ? AND ?
        """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, weekStart.toString());
            ps.setString(2, weekStart.toString());
            ps.setString(3, weekEnd.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TaskCompletion tc = new TaskCompletion();
                    tc.setId(rs.getInt("id"));
                    tc.setTaskId(rs.getInt("task_id"));
                    tc.setCompletionDate(LocalDate.parse(rs.getString("completion_date")));
                    tc.setCompleted(rs.getInt("completed") == 1);
                    completions.add(tc);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return completions;
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getInt("id"));
        t.setTitle(rs.getString("title"));
        t.setWeekStartDate(LocalDate.parse(rs.getString("week_start_date")));
        t.setDaySpecific(rs.getInt("day_specific"));
        t.setCreatedAt(rs.getString("created_at"));
        return t;
    }
}
