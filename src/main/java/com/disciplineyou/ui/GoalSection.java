package com.disciplineyou.ui;

import com.disciplineyou.dao.GoalDAO;
import com.disciplineyou.model.Goal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Goal section with accordion-style drill-down: Monthly → Weekly → Daily.
 * Each goal is a checkbox. Shifted goals show a ⚠ caution icon.
 */
public class GoalSection extends VBox {

    private final GoalDAO goalDAO = new GoalDAO();
    private VBox contentBox;
    private String currentView = "MONTHLY";  // MONTHLY, WEEKLY, DAILY
    private Integer currentParentId = null;

    public GoalSection() {
        setSpacing(0);
        setPadding(new Insets(0));
        buildUI();
        refresh();
    }

    private void buildUI() {
        contentBox = new VBox(5);
        contentBox.setPadding(new Insets(10));
        getChildren().add(contentBox);
    }

    public void refresh() {
        contentBox.getChildren().clear();

        // Navigation bar
        HBox navBar = new HBox(10);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 0, 10, 0));

        if (!currentView.equals("MONTHLY")) {
            Button backBtn = new Button("← Back");
            backBtn.getStyleClass().add("link-button");
            backBtn.setOnAction(e -> navigateBack());
            navBar.getChildren().add(backBtn);
        }

        Label viewLabel = new Label(getViewTitle());
        viewLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        viewLabel.setTextFill(Color.web("#2196F3"));
        navBar.getChildren().add(viewLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        navBar.getChildren().add(spacer);

        Button addBtn = new Button("+ Add " + currentView.substring(0, 1) + currentView.substring(1).toLowerCase() + " Goal");
        addBtn.getStyleClass().add("accent-button");
        addBtn.setOnAction(e -> showAddGoalDialog());
        navBar.getChildren().add(addBtn);

        contentBox.getChildren().add(navBar);

        // Get goals
        List<Goal> goals;
        if (currentParentId != null) {
            goals = goalDAO.getChildGoals(currentParentId);
        } else {
            goals = goalDAO.getAllGoalsByType(currentView);
        }

        if (goals.isEmpty()) {
            Label empty = new Label("No " + currentView.toLowerCase() + " goals yet. Click '+ Add' to create one.");
            empty.setTextFill(Color.GRAY);
            empty.setPadding(new Insets(20));
            contentBox.getChildren().add(empty);
        } else {
            for (Goal goal : goals) {
                contentBox.getChildren().add(createGoalRow(goal));
            }
        }
    }

    private HBox createGoalRow(Goal goal) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 10, 5, 10));
        row.getStyleClass().add("goal-row");

        // Checkbox
        CheckBox cb = new CheckBox();
        cb.setSelected(goal.isCompleted());
        cb.setOnAction(e -> {
            goalDAO.toggleComplete(goal.getId());
            refresh();
        });

        // Title
        Label titleLabel = new Label(goal.getTitle());
        titleLabel.setFont(Font.font("System", goal.isCompleted() ? FontWeight.NORMAL : FontWeight.MEDIUM, 13));
        if (goal.isCompleted()) {
            titleLabel.setTextFill(Color.GRAY);
            titleLabel.setStyle("-fx-strikethrough: true;");
        }
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        row.getChildren().addAll(cb, titleLabel);

        // Caution icon for shifted goals
        if (goal.isShifted()) {
            Label cautionLabel = new Label("⚠ shifted " + goal.getShiftCount() + "x");
            cautionLabel.setTextFill(Color.web("#FF9800"));
            cautionLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
            cautionLabel.setStyle("-fx-background-color: #FFF3E0; -fx-padding: 2 6; -fx-background-radius: 3;");
            row.getChildren().add(cautionLabel);
        }

        // Date label
        if (goal.getTargetDate() != null) {
            Label dateLabel = new Label(goal.getTargetDate().format(DateTimeFormatter.ofPattern("MMM dd")));
            dateLabel.setTextFill(Color.web("#757575"));
            dateLabel.setFont(Font.font(11));
            row.getChildren().add(dateLabel);
        }

        // Drill-down arrow for non-DAILY goals
        if (!currentView.equals("DAILY")) {
            Button drillBtn = new Button("▶");
            drillBtn.getStyleClass().add("icon-button");
            drillBtn.setTooltip(new Tooltip("View sub-goals"));
            drillBtn.setOnAction(e -> drillDown(goal));
            row.getChildren().add(drillBtn);
        }

        // Delete button
        Button delBtn = new Button("✕");
        delBtn.getStyleClass().add("delete-button");
        delBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this goal?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    goalDAO.delete(goal.getId());
                    refresh();
                }
            });
        });
        row.getChildren().add(delBtn);

        return row;
    }

    private void drillDown(Goal goal) {
        if (currentView.equals("MONTHLY")) {
            currentView = "WEEKLY";
        } else if (currentView.equals("WEEKLY")) {
            currentView = "DAILY";
        }
        currentParentId = goal.getId();
        refresh();
    }

    private void navigateBack() {
        if (currentView.equals("DAILY")) {
            currentView = "WEEKLY";
            // Find parent's parent
            if (currentParentId != null) {
                List<Goal> parent = goalDAO.getChildGoals(currentParentId);
                // Go back to monthly level if we can't find the weekly parent
                currentParentId = null;
                // Try to get the parent goal to find its parent_id
                try {
                    List<Goal> monthlyGoals = goalDAO.getAllGoalsByType("MONTHLY");
                    for (Goal mg : monthlyGoals) {
                        List<Goal> weeklyChildren = goalDAO.getChildGoals(mg.getId());
                        for (Goal wg : weeklyChildren) {
                            if (wg.getId() == currentParentId || goalDAO.getChildGoals(wg.getId()).stream().anyMatch(g -> g.getParentId() != null && g.getParentId().equals(currentParentId))) {
                                currentParentId = mg.getId();
                                break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    currentParentId = null;
                }
            }
        } else if (currentView.equals("WEEKLY")) {
            currentView = "MONTHLY";
            currentParentId = null;
        }
        refresh();
    }

    private String getViewTitle() {
        switch (currentView) {
            case "MONTHLY": return "🎯 Monthly Goals";
            case "WEEKLY": return "📅 Weekly Goals";
            case "DAILY": return "📋 Daily Goals";
            default: return "Goals";
        }
    }

    private void showAddGoalDialog() {
        Dialog<Goal> dialog = new Dialog<>();
        dialog.setTitle("Add " + currentView.substring(0, 1) + currentView.substring(1).toLowerCase() + " Goal");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Goal title...");
        titleField.setPrefWidth(300);

        TextArea descField = new TextArea();
        descField.setPromptText("Description (optional)");
        descField.setPrefRowCount(2);

        DatePicker datePicker = new DatePicker(getDefaultDate());

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Target Date:"), 0, 2);
        grid.add(datePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !titleField.getText().trim().isEmpty()) {
                Goal g = new Goal(currentView, titleField.getText().trim(), datePicker.getValue());
                g.setDescription(descField.getText().trim());
                g.setParentId(currentParentId);
                return g;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(g -> {
            goalDAO.insert(g);
            refresh();
        });
    }

    private LocalDate getDefaultDate() {
        LocalDate today = LocalDate.now();
        switch (currentView) {
            case "MONTHLY": return today.plusMonths(1).withDayOfMonth(1).minusDays(1);
            case "WEEKLY": return today.plusWeeks(1);
            case "DAILY": return today;
            default: return today;
        }
    }
}
