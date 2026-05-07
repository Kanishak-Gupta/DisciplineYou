package com.disciplineyou.service;

import com.disciplineyou.dao.GoalTaskDAO;
import com.disciplineyou.dao.SubtaskDAO;
import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.Achievement;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Gamification service for streaks, achievements, and weekly self-leaderboard.
 */
public class GamificationService {

    private final SubtaskDAO subtaskDAO = new SubtaskDAO();
    private final GoalTaskDAO goalTaskDAO = new GoalTaskDAO();

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    // ══════ STREAKS ══════

    /** Record that today all tasks were completed (or not) */
    public void recordDayCompletion(LocalDate date, boolean allCompleted) {
        String sql = "INSERT INTO streaks (streak_date, all_completed) VALUES (?, ?) ON CONFLICT(streak_date) DO UPDATE SET all_completed=excluded.all_completed";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, allCompleted ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error recording streak: " + e.getMessage());
        }
    }

    /** Get current consecutive-day streak */
    public int getCurrentStreak() {
        int streak = 0;
        LocalDate date = LocalDate.now();
        String sql = "SELECT all_completed FROM streaks WHERE streak_date=?";
        try {
            while (true) {
                try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                    ps.setString(1, date.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt("all_completed") == 1) {
                            streak++;
                            date = date.minusDays(1);
                        } else {
                            break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting streak: " + e.getMessage());
        }
        return streak;
    }

    /** Get best streak ever */
    public int getBestStreak() {
        int best = 0, current = 0;
        String sql = "SELECT streak_date, all_completed FROM streaks ORDER BY streak_date";
        try (Statement stmt = getConn().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            LocalDate prevDate = null;
            while (rs.next()) {
                LocalDate date = LocalDate.parse(rs.getString("streak_date"));
                boolean completed = rs.getInt("all_completed") == 1;
                if (completed) {
                    if (prevDate != null && date.equals(prevDate.plusDays(1))) {
                        current++;
                    } else {
                        current = 1;
                    }
                    best = Math.max(best, current);
                } else {
                    current = 0;
                }
                prevDate = date;
            }
        } catch (SQLException e) {
            System.err.println("Error getting best streak: " + e.getMessage());
        }
        return best;
    }

    // ══════ ACHIEVEMENTS ══════

    /** Initialize default achievements if not present */
    public void initDefaultAchievements() {
        List<Object[]> defaults = List.of(
            new Object[]{"7-Day Streak", "Complete all daily tasks for 7 consecutive days", "🔥", "STREAK", 7},
            new Object[]{"14-Day Streak", "Maintain a 14-day completion streak", "💪", "STREAK", 14},
            new Object[]{"30-Day Streak", "A full month of consistency!", "🏆", "STREAK", 30},
            new Object[]{"Task Starter", "Complete your first task", "🌱", "TASK_COUNT", 1},
            new Object[]{"Task Master", "Complete 25 tasks", "⭐", "TASK_COUNT", 25},
            new Object[]{"Centurion", "Complete 100 tasks", "💯", "TASK_COUNT", 100},
            new Object[]{"Perfect Week", "Complete all subtasks in a week", "✨", "PERFECT_WEEK", 1}
        );

        String sql = "INSERT OR IGNORE INTO achievements (name, description, icon, type, threshold) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            for (Object[] d : defaults) {
                ps.setString(1, (String) d[0]);
                ps.setString(2, (String) d[1]);
                ps.setString(3, (String) d[2]);
                ps.setString(4, (String) d[3]);
                ps.setInt(5, (Integer) d[4]);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            System.err.println("Error initializing achievements: " + e.getMessage());
        }
    }

    /** Check and unlock eligible achievements */
    public List<Achievement> checkAndUnlockAchievements() {
        List<Achievement> newlyUnlocked = new ArrayList<>();
        int streak = getCurrentStreak();
        int completedTasks = goalTaskDAO.getCompleted().size();

        List<Achievement> all = getAllAchievements();
        for (Achievement a : all) {
            if (a.isUnlocked()) continue;

            boolean earned = switch (a.getType()) {
                case "STREAK" -> streak >= a.getThreshold();
                case "TASK_COUNT" -> completedTasks >= a.getThreshold();
                default -> false;
            };

            if (earned) {
                unlockAchievement(a.getId());
                a.setUnlockedAt(LocalDate.now());
                newlyUnlocked.add(a);
            }
        }
        return newlyUnlocked;
    }

    public List<Achievement> getAllAchievements() {
        List<Achievement> list = new ArrayList<>();
        String sql = "SELECT * FROM achievements ORDER BY type, threshold";
        try (Statement stmt = getConn().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Achievement a = new Achievement();
                a.setId(rs.getInt("id"));
                a.setName(rs.getString("name"));
                a.setDescription(rs.getString("description"));
                a.setIcon(rs.getString("icon"));
                a.setType(rs.getString("type"));
                a.setThreshold(rs.getInt("threshold"));
                String unlocked = rs.getString("unlocked_at");
                if (unlocked != null) a.setUnlockedAt(LocalDate.parse(unlocked));
                list.add(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting achievements", e);
        }
        return list;
    }

    private void unlockAchievement(int id) {
        try (PreparedStatement ps = getConn().prepareStatement("UPDATE achievements SET unlocked_at=? WHERE id=?")) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error unlocking achievement: " + e.getMessage());
        }
    }

    // ══════ WEEKLY LEADERBOARD ══════

    /** Get weekly productivity (total completed subtasks per week for last N weeks) */
    public List<int[]> getWeeklyProductivity(int weeks) {
        List<int[]> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < weeks; i++) {
            LocalDate weekEnd = today.minusWeeks(i);
            LocalDate weekStart = weekEnd.minusDays(6);
            String sql = "SELECT COUNT(CASE WHEN completed=1 THEN 1 END) as done, COUNT(*) as total FROM subtasks WHERE day_date >= ? AND day_date <= ?";
            try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                ps.setString(1, weekStart.toString());
                ps.setString(2, weekEnd.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        result.add(new int[]{rs.getInt("done"), rs.getInt("total")});
                    } else {
                        result.add(new int[]{0, 0});
                    }
                }
            } catch (SQLException e) {
                result.add(new int[]{0, 0});
            }
        }
        return result;
    }
}
