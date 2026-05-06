package com.disciplineyou.ui;

import com.disciplineyou.dao.TaskDAO;
import com.disciplineyou.model.Task;
import com.disciplineyou.model.TaskCompletion;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified Goal/Task section with drill-down:
 *   Months → Weeks → Day grid (columns=days, rows=tasks, cells=checkboxes)
 *
 * Tasks can be added for a whole week or a specific day.
 */
public class GoalTaskSection extends VBox {

    private final TaskDAO taskDAO = new TaskDAO();
    private VBox contentBox;

    // Navigation state
    private enum ViewLevel { MONTHS, WEEKS, DAYS }
    private ViewLevel currentLevel = ViewLevel.MONTHS;
    private YearMonth selectedMonth;
    private LocalDate selectedWeekStart;

    public GoalTaskSection() {
        setSpacing(8);
        setPadding(new Insets(10));
        contentBox = new VBox(8);
        getChildren().add(contentBox);
        showMonthsView();
    }

    public void refresh() {
        switch (currentLevel) {
            case MONTHS -> showMonthsView();
            case WEEKS -> showWeeksView(selectedMonth);
            case DAYS -> showDaysView(selectedWeekStart);
        }
    }

    // ═══════════════════════════════════════════════════
    //  LEVEL 1: MONTHS VIEW
    // ═══════════════════════════════════════════════════
    private void showMonthsView() {
        currentLevel = ViewLevel.MONTHS;
        contentBox.getChildren().clear();

        Label header = new Label("📅 Goals by Month");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        contentBox.getChildren().add(header);

        // Get all weeks that have tasks, group by month
        List<LocalDate> allWeeks = taskDAO.getAllWeekStarts();
        Map<YearMonth, List<LocalDate>> weeksByMonth = allWeeks.stream()
                .collect(Collectors.groupingBy(YearMonth::from, TreeMap::new, Collectors.toList()));

        // Always show current month and next 2 months even if empty
        YearMonth now = YearMonth.now();
        for (int i = -1; i <= 2; i++) {
            weeksByMonth.putIfAbsent(now.plusMonths(i), new ArrayList<>());
        }

        for (Map.Entry<YearMonth, List<LocalDate>> entry : weeksByMonth.entrySet()) {
            YearMonth ym = entry.getKey();
            List<LocalDate> weeks = entry.getValue();

            // Count tasks for this month
            int taskCount = 0;
            for (LocalDate ws : weeks) {
                taskCount += taskDAO.getTasksForWeek(ws).size();
            }

            HBox monthRow = new HBox(10);
            monthRow.setAlignment(Pos.CENTER_LEFT);
            monthRow.setPadding(new Insets(10, 15, 10, 15));
            monthRow.getStyleClass().add("goal-row");
            monthRow.setCursor(javafx.scene.Cursor.HAND);

            Label monthLabel = new Label(ym.getMonth().toString() + " " + ym.getYear());
            monthLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label countLabel = new Label(taskCount + " task(s)");
            countLabel.setTextFill(Color.GRAY);
            countLabel.setFont(Font.font(12));

            Label arrow = new Label("▶");
            arrow.setTextFill(Color.web("#1E88E5"));

            monthRow.getChildren().addAll(monthLabel, spacer, countLabel, arrow);
            monthRow.setOnMouseClicked(e -> showWeeksView(ym));
            contentBox.getChildren().add(monthRow);
        }
    }

    // ═══════════════════════════════════════════════════
    //  LEVEL 2: WEEKS VIEW (within a month)
    // ═══════════════════════════════════════════════════
    private void showWeeksView(YearMonth ym) {
        currentLevel = ViewLevel.WEEKS;
        selectedMonth = ym;
        contentBox.getChildren().clear();

        // Back button
        Button backBtn = new Button("◀ Back to Months");
        backBtn.getStyleClass().add("link-button");
        backBtn.setOnAction(e -> showMonthsView());
        contentBox.getChildren().add(backBtn);

        Label header = new Label(ym.getMonth().toString() + " " + ym.getYear() + " — Weeks");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        contentBox.getChildren().add(header);

        // Compute all weeks (Mondays) that fall in this month
        List<LocalDate> mondaysInMonth = getMondaysInMonth(ym);

        int weekNum = 1;
        for (LocalDate monday : mondaysInMonth) {
            LocalDate sunday = monday.plusDays(6);
            List<Task> tasks = taskDAO.getTasksForWeek(monday);

            HBox weekRow = new HBox(10);
            weekRow.setAlignment(Pos.CENTER_LEFT);
            weekRow.setPadding(new Insets(10, 15, 10, 15));
            weekRow.getStyleClass().add("goal-row");
            weekRow.setCursor(javafx.scene.Cursor.HAND);

            Label weekLabel = new Label("Week " + weekNum + ": "
                    + monday.format(DateTimeFormatter.ofPattern("MMM dd"))
                    + " – " + sunday.format(DateTimeFormatter.ofPattern("MMM dd")));
            weekLabel.setFont(Font.font("System", FontWeight.MEDIUM, 13));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label taskCountLabel = new Label(tasks.size() + " task(s)");
            taskCountLabel.setTextFill(Color.GRAY);

            Label arrow = new Label("▶");
            arrow.setTextFill(Color.web("#1E88E5"));

            weekRow.getChildren().addAll(weekLabel, spacer, taskCountLabel, arrow);
            weekRow.setOnMouseClicked(e -> showDaysView(monday));
            contentBox.getChildren().add(weekRow);
            weekNum++;
        }

        // Add task for this month (choose week in dialog)
        Button addBtn = new Button("➕ Add Task");
        addBtn.getStyleClass().add("accent-button");
        addBtn.setOnAction(e -> showAddTaskDialog(mondaysInMonth));
        contentBox.getChildren().add(addBtn);
    }

    // ═══════════════════════════════════════════════════
    //  LEVEL 3: DAYS VIEW (day grid for a specific week)
    // ═══════════════════════════════════════════════════
    private void showDaysView(LocalDate weekStart) {
        currentLevel = ViewLevel.DAYS;
        selectedWeekStart = weekStart;
        contentBox.getChildren().clear();

        LocalDate weekEnd = weekStart.plusDays(6);

        // Back button
        Button backBtn = new Button("◀ Back to " + YearMonth.from(weekStart).getMonth());
        backBtn.getStyleClass().add("link-button");
        backBtn.setOnAction(e -> showWeeksView(YearMonth.from(weekStart)));
        contentBox.getChildren().add(backBtn);

        Label header = new Label("Week: " + weekStart.format(DateTimeFormatter.ofPattern("MMM dd"))
                + " – " + weekEnd.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        header.setFont(Font.font("System", FontWeight.BOLD, 15));
        contentBox.getChildren().add(header);

        List<Task> tasks = taskDAO.getTasksForWeek(weekStart);
        List<TaskCompletion> completions = taskDAO.getCompletionsForWeek(weekStart);

        // Build a lookup: (taskId, date) → completed
        Map<String, Boolean> completionMap = new HashMap<>();
        for (TaskCompletion tc : completions) {
            completionMap.put(tc.getTaskId() + "_" + tc.getCompletionDate(), tc.isCompleted());
        }

        if (tasks.isEmpty()) {
            Label empty = new Label("No tasks for this week. Add some!");
            empty.setTextFill(Color.GRAY);
            empty.setPadding(new Insets(15));
            contentBox.getChildren().add(empty);
        } else {
            // Build grid: columns = days, rows = tasks
            GridPane grid = new GridPane();
            grid.setHgap(3);
            grid.setVgap(5);
            grid.setPadding(new Insets(10, 0, 10, 0));

            // Header row: Task | Mon | Tue | Wed | Thu | Fri | Sat | Sun
            Label taskHeader = new Label("Task");
            taskHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
            taskHeader.setMinWidth(150);
            taskHeader.setPadding(new Insets(0, 10, 5, 0));
            grid.add(taskHeader, 0, 0);

            String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (int d = 0; d < 7; d++) {
                LocalDate date = weekStart.plusDays(d);
                VBox dayHeader = new VBox(1);
                dayHeader.setAlignment(Pos.CENTER);
                Label dayName = new Label(dayNames[d]);
                dayName.setFont(Font.font("System", FontWeight.BOLD, 11));
                Label dayDate = new Label(date.format(DateTimeFormatter.ofPattern("dd")));
                dayDate.setFont(Font.font(10));
                dayDate.setTextFill(Color.GRAY);

                // Highlight today
                if (date.equals(LocalDate.now())) {
                    dayName.setTextFill(Color.web("#1E88E5"));
                    dayDate.setTextFill(Color.web("#1E88E5"));
                }

                dayHeader.getChildren().addAll(dayName, dayDate);
                dayHeader.setMinWidth(50);
                grid.add(dayHeader, d + 1, 0);
            }

            // Action column header
            Label actHeader = new Label("");
            actHeader.setMinWidth(30);
            grid.add(actHeader, 8, 0);

            // Task rows
            int row = 1;
            for (Task task : tasks) {
                // Task name
                Label taskLabel = new Label(task.getTitle());
                taskLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                taskLabel.setMinWidth(150);
                taskLabel.setMaxWidth(200);
                taskLabel.setWrapText(true);
                taskLabel.setPadding(new Insets(0, 10, 0, 0));

                // Show badge for day-specific
                if (!task.isWholeWeek()) {
                    taskLabel.setText(task.getTitle() + " 📌");
                    taskLabel.setStyle("-fx-text-fill: #1565C0;");
                }

                grid.add(taskLabel, 0, row);

                // Checkboxes per day
                for (int d = 0; d < 7; d++) {
                    int dayOfWeek = d + 1; // 1=Mon..7=Sun
                    LocalDate date = weekStart.plusDays(d);

                    if (task.appliesTo(dayOfWeek)) {
                        CheckBox cb = new CheckBox();
                        String key = task.getId() + "_" + date;
                        cb.setSelected(Boolean.TRUE.equals(completionMap.get(key)));
                        cb.setOnAction(ev -> taskDAO.setCompletion(task.getId(), date, cb.isSelected()));

                        StackPane cellPane = new StackPane(cb);
                        cellPane.setAlignment(Pos.CENTER);
                        cellPane.setMinWidth(50);
                        grid.add(cellPane, d + 1, row);
                    } else {
                        // Gray dash for non-applicable days
                        Label dash = new Label("—");
                        dash.setTextFill(Color.LIGHTGRAY);
                        dash.setAlignment(Pos.CENTER);
                        dash.setMinWidth(50);
                        StackPane cellPane = new StackPane(dash);
                        cellPane.setAlignment(Pos.CENTER);
                        grid.add(cellPane, d + 1, row);
                    }
                }

                // Delete button
                Button delBtn = new Button("✕");
                delBtn.getStyleClass().add("delete-button");
                delBtn.setOnAction(ev -> {
                    taskDAO.deleteTask(task.getId());
                    showDaysView(weekStart);
                });
                grid.add(delBtn, 8, row);

                row++;
            }

            // Wrap grid in scrollpane for wide screens
            ScrollPane gridScroll = new ScrollPane(grid);
            gridScroll.setFitToWidth(true);
            gridScroll.setFitToHeight(true);
            gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            gridScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            gridScroll.setStyle("-fx-background-color: transparent;");
            gridScroll.setMaxHeight(400);
            contentBox.getChildren().add(gridScroll);
        }

        // Add task button
        Button addBtn = new Button("➕ Add Task to This Week");
        addBtn.getStyleClass().add("accent-button");
        addBtn.setOnAction(e -> showAddTaskForWeekDialog(weekStart));
        contentBox.getChildren().add(addBtn);
    }

    // ═══════════════════════════════════════════════════
    //  ADD TASK DIALOGS
    // ═══════════════════════════════════════════════════

    /** Add task dialog from the weeks view (user picks which week) */
    private void showAddTaskDialog(List<LocalDate> mondaysInMonth) {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Add Task");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Task name...");
        titleField.setPrefWidth(250);

        // Week selector
        ComboBox<String> weekCombo = new ComboBox<>();
        List<LocalDate> weeks = new ArrayList<>(mondaysInMonth);
        for (LocalDate monday : weeks) {
            LocalDate sunday = monday.plusDays(6);
            weekCombo.getItems().add("Week: " + monday.format(DateTimeFormatter.ofPattern("MMM dd"))
                    + " – " + sunday.format(DateTimeFormatter.ofPattern("MMM dd")));
        }
        if (!weekCombo.getItems().isEmpty()) weekCombo.getSelectionModel().selectFirst();

        // Scope: whole week vs specific day
        ToggleGroup scopeGroup = new ToggleGroup();
        RadioButton wholeWeekRb = new RadioButton("For the whole week");
        wholeWeekRb.setToggleGroup(scopeGroup);
        wholeWeekRb.setSelected(true);
        RadioButton specificDayRb = new RadioButton("For a specific day");
        specificDayRb.setToggleGroup(scopeGroup);

        ComboBox<String> dayCombo = new ComboBox<>();
        dayCombo.getItems().addAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        dayCombo.setValue("Monday");
        dayCombo.setDisable(true);

        specificDayRb.setOnAction(e -> dayCombo.setDisable(false));
        wholeWeekRb.setOnAction(e -> dayCombo.setDisable(true));

        grid.add(new Label("Task:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Week:"), 0, 1);
        grid.add(weekCombo, 1, 1);
        grid.add(wholeWeekRb, 0, 2, 2, 1);
        grid.add(specificDayRb, 0, 3);
        grid.add(dayCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !titleField.getText().trim().isEmpty()) {
                int weekIdx = weekCombo.getSelectionModel().getSelectedIndex();
                if (weekIdx < 0) return null;
                LocalDate weekStart = weeks.get(weekIdx);
                int daySpec = wholeWeekRb.isSelected() ? 0 : dayCombo.getSelectionModel().getSelectedIndex() + 1;
                return new Task(titleField.getText().trim(), weekStart, daySpec);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(task -> {
            taskDAO.insertTask(task);
            refresh();
        });
    }

    /** Add task dialog directly for a specific week (from days view) */
    private void showAddTaskForWeekDialog(LocalDate weekStart) {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Add Task — Week of " + weekStart.format(DateTimeFormatter.ofPattern("MMM dd")));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Task name...");
        titleField.setPrefWidth(250);

        ToggleGroup scopeGroup = new ToggleGroup();
        RadioButton wholeWeekRb = new RadioButton("For the whole week");
        wholeWeekRb.setToggleGroup(scopeGroup);
        wholeWeekRb.setSelected(true);
        RadioButton specificDayRb = new RadioButton("For a specific day");
        specificDayRb.setToggleGroup(scopeGroup);

        ComboBox<String> dayCombo = new ComboBox<>();
        for (int d = 0; d < 7; d++) {
            LocalDate date = weekStart.plusDays(d);
            dayCombo.getItems().add(date.getDayOfWeek().toString() + " (" + date.format(DateTimeFormatter.ofPattern("MMM dd")) + ")");
        }
        dayCombo.getSelectionModel().selectFirst();
        dayCombo.setDisable(true);

        specificDayRb.setOnAction(e -> dayCombo.setDisable(false));
        wholeWeekRb.setOnAction(e -> dayCombo.setDisable(true));

        grid.add(new Label("Task:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(wholeWeekRb, 0, 1, 2, 1);
        grid.add(specificDayRb, 0, 2);
        grid.add(dayCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !titleField.getText().trim().isEmpty()) {
                int daySpec = wholeWeekRb.isSelected() ? 0 : dayCombo.getSelectionModel().getSelectedIndex() + 1;
                return new Task(titleField.getText().trim(), weekStart, daySpec);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(task -> {
            taskDAO.insertTask(task);
            showDaysView(weekStart);
        });
    }

    // ═══════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════

    /** Get all Mondays whose week overlaps with the given month */
    private List<LocalDate> getMondaysInMonth(YearMonth ym) {
        List<LocalDate> mondays = new ArrayList<>();
        LocalDate firstOfMonth = ym.atDay(1);
        LocalDate lastOfMonth = ym.atEndOfMonth();

        // Find Monday of the week containing the first day of the month
        LocalDate monday = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        while (!monday.isAfter(lastOfMonth)) {
            mondays.add(monday);
            monday = monday.plusWeeks(1);
        }
        return mondays;
    }
}
