package com.disciplineyou.service;

import com.disciplineyou.dao.*;
import com.disciplineyou.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates notifications for missed tasks, deadlines, timetable changes, and attendance warnings.
 */
public class NotificationService {

    private final GoalTaskDAO goalTaskDAO = new GoalTaskDAO();
    private final SubtaskDAO subtaskDAO = new SubtaskDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final TimetableDAO timetableDAO = new TimetableDAO();

    private final List<Notification> notifications = new ArrayList<>();

    /** Generate all notifications. Call on app startup and on refresh. */
    public List<Notification> generateNotifications() {
        notifications.clear();
        checkMissedTasks();
        checkUpcomingDeadlines();
        checkTimetableChanges();
        checkAttendanceWarnings();
        return notifications;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public int getUnreadCount() {
        return (int) notifications.stream().filter(n -> !n.isRead()).count();
    }

    public void markAllRead() {
        notifications.forEach(n -> n.setRead(true));
    }

    public void dismiss(Notification n) {
        notifications.remove(n);
    }

    private void checkMissedTasks() {
        List<Subtask> missed = subtaskDAO.getIncompleteSubtasksBefore(LocalDate.now());
        if (!missed.isEmpty()) {
            int count = missed.size();
            String msg = count == 1 ? "You missed yesterday's planned task." : "You have " + count + " incomplete subtasks from past days.";
            notifications.add(new Notification(Notification.Type.MISSED_TASK, "Missed Tasks", msg));
        }

        List<GoalTask> overdue = goalTaskDAO.getOverdueTasks();
        for (GoalTask t : overdue) {
            notifications.add(new Notification(Notification.Type.MISSED_TASK,
                    "Overdue: " + t.getTitle(),
                    "Target was " + t.getTargetDate() + ". Weekly progress is behind target."));
        }
    }

    private void checkUpcomingDeadlines() {
        List<GoalTask> upcoming = goalTaskDAO.getUpcomingDeadlines(3);
        for (GoalTask t : upcoming) {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), t.getTargetDate());
            String when = daysLeft == 0 ? "today" : daysLeft == 1 ? "tomorrow" : "in " + daysLeft + " days";
            notifications.add(new Notification(Notification.Type.DEADLINE,
                    "Deadline: " + t.getTitle(),
                    "Due " + when + " (" + t.getProgressPercent() + "% complete)"));
        }
    }

    private void checkTimetableChanges() {
        LocalDate today = LocalDate.now();
        List<TimetableOverride> todayOverrides = timetableDAO.getOverridesForDate(today);
        List<TimetableOverride> tomorrowOverrides = timetableDAO.getOverridesForDate(today.plusDays(1));

        for (TimetableOverride ov : todayOverrides) {
            String type = ov.getOverrideType();
            String desc = "CANCEL".equals(type) ? "A class has been cancelled today." : "A class has been changed today.";
            notifications.add(new Notification(Notification.Type.TIMETABLE_CHANGE, "Today's Timetable Change", desc));
        }
        for (TimetableOverride ov : tomorrowOverrides) {
            String type = ov.getOverrideType();
            String desc = "CANCEL".equals(type) ? "A class is cancelled tomorrow." : "A class is changed tomorrow.";
            notifications.add(new Notification(Notification.Type.TIMETABLE_CHANGE, "Tomorrow's Timetable Change", desc));
        }
    }

    private void checkAttendanceWarnings() {
        Semester sem = subjectDAO.getActiveSemester();
        if (sem == null) return;

        List<Subject> subjects = subjectDAO.getSubjectsBySemester(sem.getId());
        LocalDate endDate = LocalDate.now().isBefore(sem.getEndDate()) ? LocalDate.now() : sem.getEndDate();

        for (Subject subj : subjects) {
            int[] stats = attendanceDAO.getCalculatedAttendanceStats(subj.getId(), sem.getStartDate(), endDate);
            int present = stats[0];
            int total = stats[1];
            if (total > 0) {
                double pct = (double) present / total * 100;
                if (pct < 75) {
                    notifications.add(new Notification(Notification.Type.ATTENDANCE_WARNING,
                            "Low Attendance: " + subj.getName(),
                            String.format("Attendance at %.0f%% (%d/%d). Minimum 75%% required.", pct, present, total)));
                }
            }
        }
    }
}
