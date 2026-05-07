package com.disciplineyou.ui;

import com.disciplineyou.dao.*;
import com.disciplineyou.model.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Task list with drill-down: Task List → Week Breakdown → Day Subtasks.
 */
public class GoalTaskListSection extends VBox {

    private final GoalTaskDAO goalTaskDAO = new GoalTaskDAO();
    private final SubtaskDAO subtaskDAO = new SubtaskDAO();
    private VBox contentBox;

    private enum View { TASK_LIST, WEEK_BREAKDOWN, DAY_SUBTASKS }
    private View currentView = View.TASK_LIST;
    private GoalTask selectedTask;
    private int selectedWeek;

    public GoalTaskListSection() {
        setSpacing(8);
        setPadding(new Insets(10));
        contentBox = new VBox(8);
        getChildren().add(contentBox);
        showTaskList();
    }

    public void refresh() {
        switch (currentView) {
            case TASK_LIST -> showTaskList();
            case WEEK_BREAKDOWN -> showWeekBreakdown(selectedTask);
            case DAY_SUBTASKS -> showDaySubtasks(selectedTask, selectedWeek);
        }
    }

    // ════════════════ TASK LIST ════════════════
    private void showTaskList() {
        currentView = View.TASK_LIST;
        contentBox.getChildren().clear();

        // Warnings banner
        List<Subtask> missed = subtaskDAO.getIncompleteSubtasksBefore(LocalDate.now());
        if (!missed.isEmpty()) {
            HBox warning = new HBox(8);
            warning.setAlignment(Pos.CENTER_LEFT);
            warning.setPadding(new Insets(8, 12, 8, 12));
            warning.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 6; -fx-border-color: #FFB74D; -fx-border-radius: 6;");
            Label warnIcon = new Label("⚠");
            warnIcon.setFont(Font.font(16));
            Label warnText = new Label(missed.size() + " incomplete subtask(s) from past days. Stay on track!");
            warnText.setTextFill(Color.web("#E65100"));
            warnText.setFont(Font.font("System", FontWeight.BOLD, 12));
            warning.getChildren().addAll(warnIcon, warnText);
            contentBox.getChildren().add(warning);
        }

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🎯 Goal Tasks");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button historyBtn = new Button("📦 History");
        historyBtn.getStyleClass().add("link-button");
        historyBtn.setOnAction(e -> showHistory());

        Button addBtn = new Button("➕ Create Task");
        addBtn.getStyleClass().add("accent-button");
        addBtn.setOnAction(e -> showCreateTaskDialog());

        header.getChildren().addAll(title, sp, historyBtn, addBtn);
        contentBox.getChildren().add(header);

        // Task cards
        List<GoalTask> tasks = goalTaskDAO.getAllActive();
        if (tasks.isEmpty()) {
            Label empty = new Label("No active tasks. Create one to start tracking your goals!");
            empty.setTextFill(Color.GRAY);
            empty.setPadding(new Insets(20));
            contentBox.getChildren().add(empty);
        }

        for (GoalTask task : tasks) {
            contentBox.getChildren().add(createTaskCard(task));
        }
    }

    private VBox createTaskCard(GoalTask task) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.getStyleClass().add("goal-row");
        card.setCursor(javafx.scene.Cursor.HAND);

        // Top row: priority badge + title + days left
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label priorityBadge = new Label(task.getPriority());
        priorityBadge.setStyle("-fx-background-color: " + task.getPriorityColor() + "; -fx-text-fill: white; " +
                "-fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10; -fx-font-weight: bold;");

        Label titleLabel = new Label(task.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), task.getTargetDate());
        Label deadlineLabel = new Label(daysLeft >= 0 ? daysLeft + "d left" : Math.abs(daysLeft) + "d overdue");
        deadlineLabel.setTextFill(daysLeft < 0 ? Color.web("#F44336") : daysLeft <= 3 ? Color.web("#FF9800") : Color.GRAY);
        deadlineLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

        top.getChildren().addAll(priorityBadge, titleLabel, deadlineLabel);

        // Progress bar
        ProgressBar pb = new ProgressBar(task.getProgressPercent() / 100.0);
        pb.setPrefWidth(Double.MAX_VALUE);
        pb.setPrefHeight(8);
        String barColor = task.getProgressPercent() >= 80 ? "#4CAF50" : task.getProgressPercent() >= 40 ? "#FF9800" : "#2196F3";
        pb.setStyle("-fx-accent: " + barColor + ";");

        // Bottom row: category + progress %
        HBox bottom = new HBox(8);
        bottom.setAlignment(Pos.CENTER_LEFT);
        if (task.getCategory() != null && !task.getCategory().isEmpty()) {
            Label catLabel = new Label("🏷 " + task.getCategory());
            catLabel.setTextFill(Color.web("#7E57C2"));
            catLabel.setFont(Font.font(11));
            bottom.getChildren().add(catLabel);
        }
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        Label progLabel = new Label(task.getProgressPercent() + "%");
        progLabel.setTextFill(Color.GRAY);

        Button completeBtn = new Button("✓");
        completeBtn.getStyleClass().add("present-button");
        completeBtn.setTooltip(new Tooltip("Mark completed"));
        completeBtn.setOnAction(e -> { goalTaskDAO.markCompleted(task.getId()); showTaskList(); e.consume(); });

        Button delBtn = new Button("✕");
        delBtn.getStyleClass().add("delete-button");
        delBtn.setOnAction(e -> {
            new Alert(Alert.AlertType.CONFIRMATION, "Delete '" + task.getTitle() + "'?", ButtonType.YES, ButtonType.NO)
                    .showAndWait().ifPresent(b -> { if (b == ButtonType.YES) { goalTaskDAO.delete(task.getId()); showTaskList(); } });
            e.consume();
        });

        bottom.getChildren().addAll(sp2, progLabel, completeBtn, delBtn);
        card.getChildren().addAll(top, pb, bottom);
        card.setOnMouseClicked(e -> showWeekBreakdown(task));
        return card;
    }

    // ════════════════ WEEK BREAKDOWN ════════════════
    private void showWeekBreakdown(GoalTask task) {
        currentView = View.WEEK_BREAKDOWN;
        selectedTask = task;
        contentBox.getChildren().clear();

        Button backBtn = new Button("◀ Back to Tasks");
        backBtn.getStyleClass().add("link-button");
        backBtn.setOnAction(e -> showTaskList());

        Label header = new Label("📅 " + task.getTitle() + " — Week Breakdown");
        header.setFont(Font.font("System", FontWeight.BOLD, 15));

        contentBox.getChildren().addAll(backBtn, header);

        // Calculate dynamic week segments
        LocalDate start = task.getStartDate().isBefore(LocalDate.now()) ? LocalDate.now() : task.getStartDate();
        LocalDate end = task.getTargetDate();
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        if (totalDays <= 0) totalDays = 1;
        int numWeeks = (int) Math.ceil(totalDays / 7.0);

        List<Integer> existingWeeks = subtaskDAO.getWeekNumbers(task.getId());

        for (int w = 1; w <= Math.max(numWeeks, existingWeeks.isEmpty() ? 0 : existingWeeks.get(existingWeeks.size() - 1)); w++) {
            LocalDate weekStart = start.plusDays((long)(w - 1) * 7);
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(end)) weekEnd = end;

            int[] stats = subtaskDAO.getWeekCompletionStats(task.getId(), w);
            int done = stats[0], total = stats[1];

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.getStyleClass().add("goal-row");
            row.setCursor(javafx.scene.Cursor.HAND);

            Label weekLabel = new Label("Week " + w + ": " +
                    weekStart.format(DateTimeFormatter.ofPattern("MMM dd")) + " – " +
                    weekEnd.format(DateTimeFormatter.ofPattern("MMM dd")));
            weekLabel.setFont(Font.font("System", FontWeight.MEDIUM, 13));
            HBox.setHgrow(weekLabel, Priority.ALWAYS);

            Label statsLabel = new Label(total > 0 ? done + "/" + total + " done" : "No subtasks");
            statsLabel.setTextFill(done == total && total > 0 ? Color.web("#4CAF50") : Color.GRAY);

            Label arrow = new Label("▶");
            arrow.setTextFill(Color.web("#1E88E5"));

            final int weekNum = w;
            final LocalDate ws = weekStart;
            final LocalDate we = weekEnd;
            row.setOnMouseClicked(e -> showDaySubtasks(task, weekNum));
            row.getChildren().addAll(weekLabel, statsLabel, arrow);
            contentBox.getChildren().add(row);
        }

        Button addSubBtn = new Button("➕ Quick Add Subtask");
        addSubBtn.getStyleClass().add("accent-button");
        addSubBtn.setOnAction(e -> showAddSubtaskDialog(task));
        contentBox.getChildren().add(addSubBtn);
    }

    // ════════════════ DAY SUBTASKS ════════════════
    private void showDaySubtasks(GoalTask task, int weekNum) {
        currentView = View.DAY_SUBTASKS;
        selectedTask = task;
        selectedWeek = weekNum;
        contentBox.getChildren().clear();

        Button backBtn = new Button("◀ Back to Weeks");
        backBtn.getStyleClass().add("link-button");
        backBtn.setOnAction(e -> showWeekBreakdown(task));

        Label header = new Label("Week " + weekNum + " — Day Subtasks");
        header.setFont(Font.font("System", FontWeight.BOLD, 15));
        contentBox.getChildren().addAll(backBtn, header);

        List<Subtask> subtasks = subtaskDAO.getSubtasksForWeek(task.getId(), weekNum);

        if (subtasks.isEmpty()) {
            Label empty = new Label("No subtasks for this week yet. Add some!");
            empty.setTextFill(Color.GRAY);
            empty.setPadding(new Insets(10));
            contentBox.getChildren().add(empty);
        } else {
            // Group by date
            java.util.Map<LocalDate, java.util.List<Subtask>> byDate = subtasks.stream()
                    .collect(java.util.stream.Collectors.groupingBy(Subtask::getDayDate, java.util.TreeMap::new, java.util.stream.Collectors.toList()));

            for (var entry : byDate.entrySet()) {
                LocalDate date = entry.getKey();
                boolean isToday = date.equals(LocalDate.now());
                boolean isPast = date.isBefore(LocalDate.now());

                Label dayLabel = new Label((isToday ? "▶ " : "") + date.getDayOfWeek() + ", " +
                        date.format(DateTimeFormatter.ofPattern("MMM dd")));
                dayLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
                dayLabel.setTextFill(isToday ? Color.web("#1E88E5") : isPast ? Color.GRAY : Color.BLACK);
                contentBox.getChildren().add(dayLabel);

                for (Subtask st : entry.getValue()) {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(3, 10, 3, 20));

                    CheckBox cb = new CheckBox(st.getTitle());
                    cb.setSelected(st.isCompleted());
                    if (st.isCompleted()) cb.setStyle("-fx-text-fill: #9E9E9E;");
                    if (st.isOverdue()) cb.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    cb.setOnAction(ev -> {
                        subtaskDAO.toggleCompletion(st.getId());
                        updateTaskProgress(task);
                        showDaySubtasks(task, weekNum);
                    });

                    Button delBtn = new Button("✕");
                    delBtn.getStyleClass().add("delete-button");
                    delBtn.setOnAction(ev -> {
                        subtaskDAO.delete(st.getId());
                        updateTaskProgress(task);
                        showDaySubtasks(task, weekNum);
                    });

                    HBox.setHgrow(cb, Priority.ALWAYS);
                    row.getChildren().addAll(cb, delBtn);
                    contentBox.getChildren().add(row);
                }
            }
        }

        // Add subtask for this week
        HBox addRow = new HBox(8);
        addRow.setAlignment(Pos.CENTER_LEFT);
        addRow.setPadding(new Insets(8, 0, 0, 0));
        TextField titleField = new TextField();
        titleField.setPromptText("New subtask title...");
        titleField.setPrefWidth(200);
        DatePicker datePicker = new DatePicker(LocalDate.now());
        Button addBtn = new Button("➕ Add");
        addBtn.getStyleClass().add("accent-button");
        addBtn.setOnAction(e -> {
            if (titleField.getText().trim().isEmpty()) return;
            Subtask st = new Subtask(task.getId(), weekNum, datePicker.getValue(), titleField.getText().trim());
            subtaskDAO.insert(st);
            updateTaskProgress(task);
            showDaySubtasks(task, weekNum);
        });
        addRow.getChildren().addAll(titleField, datePicker, addBtn);
        contentBox.getChildren().add(addRow);
    }

    private void updateTaskProgress(GoalTask task) {
        int[] stats = subtaskDAO.getCompletionStats(task.getId());
        int pct = stats[1] > 0 ? (int)((double) stats[0] / stats[1] * 100) : 0;
        goalTaskDAO.updateProgress(task.getId(), pct);
        task.setProgressPercent(pct);
    }

    // ════════════════ DIALOGS ════════════════
    private void showCreateTaskDialog() {
        Dialog<GoalTask> dialog = new Dialog<>();
        dialog.setTitle("Create Goal Task");
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(15));

        TextField titleF = new TextField(); titleF.setPromptText("Task title"); titleF.setPrefWidth(250);
        TextArea descF = new TextArea(); descF.setPromptText("Description"); descF.setPrefRowCount(2);
        DatePicker startP = new DatePicker(LocalDate.now());
        DatePicker endP = new DatePicker(LocalDate.now().plusWeeks(2));
        ComboBox<String> prioC = new ComboBox<>(); prioC.getItems().addAll("HIGH","MEDIUM","LOW"); prioC.setValue("MEDIUM");
        TextField catF = new TextField(); catF.setPromptText("Category tag");

        g.add(new Label("Title:"), 0, 0);    g.add(titleF, 1, 0);
        g.add(new Label("Desc:"), 0, 1);     g.add(descF, 1, 1);
        g.add(new Label("Start:"), 0, 2);    g.add(startP, 1, 2);
        g.add(new Label("Target:"), 0, 3);   g.add(endP, 1, 3);
        g.add(new Label("Priority:"), 0, 4); g.add(prioC, 1, 4);
        g.add(new Label("Category:"), 0, 5); g.add(catF, 1, 5);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !titleF.getText().trim().isEmpty())
                return new GoalTask(titleF.getText().trim(), descF.getText().trim(), startP.getValue(), endP.getValue(), prioC.getValue(), catF.getText().trim());
            return null;
        });
        dialog.showAndWait().ifPresent(t -> { goalTaskDAO.insert(t); showTaskList(); });
    }

    private void showAddSubtaskDialog(GoalTask task) {
        Dialog<Subtask> dialog = new Dialog<>();
        dialog.setTitle("Add Subtask to " + task.getTitle());
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(15));
        TextField titleF = new TextField(); titleF.setPromptText("Subtask title"); titleF.setPrefWidth(200);
        Spinner<Integer> weekSp = new Spinner<>(1, 52, 1);
        DatePicker dateP = new DatePicker(LocalDate.now());
        g.add(new Label("Title:"), 0, 0); g.add(titleF, 1, 0);
        g.add(new Label("Week #:"), 0, 1); g.add(weekSp, 1, 1);
        g.add(new Label("Date:"), 0, 2); g.add(dateP, 1, 2);
        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !titleF.getText().trim().isEmpty())
                return new Subtask(task.getId(), weekSp.getValue(), dateP.getValue(), titleF.getText().trim());
            return null;
        });
        dialog.showAndWait().ifPresent(st -> { subtaskDAO.insert(st); updateTaskProgress(task); showWeekBreakdown(task); });
    }

    private void showHistory() {
        contentBox.getChildren().clear();
        Button backBtn = new Button("◀ Back to Tasks");
        backBtn.getStyleClass().add("link-button");
        backBtn.setOnAction(e -> showTaskList());
        contentBox.getChildren().add(backBtn);

        Label header = new Label("📦 Completed Task History");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        contentBox.getChildren().add(header);

        List<GoalTask> completed = goalTaskDAO.getCompleted();
        List<GoalTask> archived = goalTaskDAO.getArchived();
        completed.addAll(archived);

        if (completed.isEmpty()) {
            Label empty = new Label("No completed tasks yet.");
            empty.setTextFill(Color.GRAY);
            contentBox.getChildren().add(empty);
            return;
        }

        for (GoalTask t : completed) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));
            row.getStyleClass().add("goal-row");

            Label tl = new Label("✓ " + t.getTitle());
            tl.setFont(Font.font("System", FontWeight.MEDIUM, 13));
            tl.setTextFill(Color.GRAY);
            HBox.setHgrow(tl, Priority.ALWAYS);

            int[] stats = subtaskDAO.getCompletionStats(t.getId());
            Label sl = new Label(stats[0] + "/" + stats[1] + " subtasks");
            sl.setTextFill(Color.GRAY);

            Label dl = new Label(t.getCompletedAt() != null ? t.getCompletedAt().format(DateTimeFormatter.ofPattern("MMM dd")) : "—");
            dl.setTextFill(Color.GRAY);

            row.getChildren().addAll(tl, sl, dl);
            contentBox.getChildren().add(row);
        }
    }
}
