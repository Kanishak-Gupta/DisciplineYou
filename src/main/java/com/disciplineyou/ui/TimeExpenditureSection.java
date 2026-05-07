package com.disciplineyou.ui;

import com.disciplineyou.dao.TimeEntryDAO;
import com.disciplineyou.model.TimeEntry;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Time expenditure logging and analytics with pie chart.
 */
public class TimeExpenditureSection extends VBox {

    private final TimeEntryDAO timeEntryDAO = new TimeEntryDAO();
    private LocalDate rangeStart;
    private LocalDate rangeEnd;
    private VBox contentBox;

    private static final String[] DEFAULT_CATEGORIES = {"Study", "Leisure", "Exercise", "Work", "Social", "Other"};

    public TimeExpenditureSection() {
        setSpacing(10);
        setPadding(new Insets(10));
        rangeStart = LocalDate.now().minusDays(6);
        rangeEnd = LocalDate.now();
        contentBox = new VBox(10);
        getChildren().add(contentBox);
        buildUI();
    }

    public void refresh() { buildUI(); }

    private void buildUI() {
        contentBox.getChildren().clear();

        // Header
        Label header = new Label("⏱ Time Expenditure");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));

        // Date range selector
        HBox rangeBox = new HBox(8);
        rangeBox.setAlignment(Pos.CENTER_LEFT);
        Button todayBtn = new Button("Today");
        todayBtn.getStyleClass().add("link-button");
        todayBtn.setOnAction(e -> { rangeStart = rangeEnd = LocalDate.now(); buildUI(); });
        Button weekBtn = new Button("This Week");
        weekBtn.getStyleClass().add("link-button");
        weekBtn.setOnAction(e -> { rangeStart = LocalDate.now().minusDays(6); rangeEnd = LocalDate.now(); buildUI(); });
        Button monthBtn = new Button("This Month");
        monthBtn.getStyleClass().add("link-button");
        monthBtn.setOnAction(e -> { rangeStart = LocalDate.now().withDayOfMonth(1); rangeEnd = LocalDate.now(); buildUI(); });
        Label rangeLabel = new Label(rangeStart.format(DateTimeFormatter.ofPattern("MMM dd")) + " – " +
                rangeEnd.format(DateTimeFormatter.ofPattern("MMM dd")));
        rangeLabel.setTextFill(Color.web("#1E88E5"));
        rangeLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        rangeBox.getChildren().addAll(todayBtn, weekBtn, monthBtn, rangeLabel);

        contentBox.getChildren().addAll(header, rangeBox);

        // Log entry form
        TitledPane logPane = new TitledPane();
        logPane.setText("➕ Log Time");
        logPane.setExpanded(false);
        GridPane logGrid = new GridPane();
        logGrid.setHgap(10); logGrid.setVgap(8); logGrid.setPadding(new Insets(10));

        TextField activityField = new TextField(); activityField.setPromptText("Activity name");
        TextField durationField = new TextField("30"); durationField.setPrefWidth(60);
        durationField.textProperty().addListener((o,ov,nv) -> { if (!nv.matches("\\d*")) durationField.setText(nv.replaceAll("[^\\d]", "")); });
        DatePicker datePicker = new DatePicker(LocalDate.now());
        ComboBox<String> catCombo = new ComboBox<>();
        catCombo.getItems().addAll(DEFAULT_CATEGORIES);
        catCombo.setValue("Study");
        catCombo.setEditable(true);

        Button logBtn = new Button("💾 Log");
        logBtn.getStyleClass().add("accent-button");
        logBtn.setOnAction(e -> {
            if (activityField.getText().trim().isEmpty()) return;
            TimeEntry te = new TimeEntry(activityField.getText().trim(),
                    Integer.parseInt(durationField.getText().isEmpty() ? "0" : durationField.getText()),
                    datePicker.getValue(), catCombo.getValue());
            timeEntryDAO.insert(te);
            activityField.clear();
            buildUI();
        });

        logGrid.add(new Label("Activity:"), 0, 0); logGrid.add(activityField, 1, 0);
        logGrid.add(new Label("Minutes:"), 2, 0); logGrid.add(durationField, 3, 0);
        logGrid.add(new Label("Date:"), 0, 1); logGrid.add(datePicker, 1, 1);
        logGrid.add(new Label("Category:"), 2, 1); logGrid.add(catCombo, 3, 1);
        logGrid.add(logBtn, 1, 2);
        logPane.setContent(logGrid);
        contentBox.getChildren().add(logPane);

        // Analytics
        Map<String, Integer> byActivity = timeEntryDAO.getTotalByActivity(rangeStart, rangeEnd);
        Map<String, Integer> byCategory = timeEntryDAO.getTotalByCategory(rangeStart, rangeEnd);

        int totalMins = byActivity.values().stream().mapToInt(Integer::intValue).sum();

        // Summary bar
        HBox summaryBar = new HBox(20);
        summaryBar.setAlignment(Pos.CENTER_LEFT);
        summaryBar.setPadding(new Insets(8, 12, 8, 12));
        summaryBar.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 6;");

        long days = java.time.temporal.ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1;
        Label totalLabel = new Label("Total: " + formatMins(totalMins));
        totalLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        Label avgLabel = new Label("Avg/day: " + formatMins((int)(totalMins / Math.max(1, days))));
        avgLabel.setFont(Font.font(12));
        summaryBar.getChildren().addAll(totalLabel, avgLabel);
        contentBox.getChildren().add(summaryBar);

        if (!byCategory.isEmpty()) {
            // Pie chart
            PieChart pie = new PieChart();
            pie.setTitle("Time by Category");
            pie.setPrefHeight(250);
            pie.setLabelsVisible(true);
            pie.setLegendVisible(true);
            for (var entry : byCategory.entrySet()) {
                pie.getData().add(new PieChart.Data(entry.getKey() + " (" + formatMins(entry.getValue()) + ")", entry.getValue()));
            }
            contentBox.getChildren().add(pie);
        }

        // Activity list
        if (!byActivity.isEmpty()) {
            Label actHeader = new Label("📊 Activities");
            actHeader.setFont(Font.font("System", FontWeight.BOLD, 13));
            contentBox.getChildren().add(actHeader);

            for (var entry : byActivity.entrySet()) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(3, 10, 3, 10));
                Label nameL = new Label(entry.getKey());
                HBox.setHgrow(nameL, Priority.ALWAYS);
                Label durL = new Label(formatMins(entry.getValue()));
                durL.setTextFill(Color.web("#1E88E5"));
                durL.setFont(Font.font("System", FontWeight.BOLD, 12));
                double pct = totalMins > 0 ? (double) entry.getValue() / totalMins : 0;
                ProgressBar bar = new ProgressBar(pct);
                bar.setPrefWidth(100);
                bar.setPrefHeight(6);
                row.getChildren().addAll(nameL, bar, durL);
                contentBox.getChildren().add(row);
            }
        }

        // Recent entries for today
        List<TimeEntry> todayEntries = timeEntryDAO.getEntriesForDate(LocalDate.now());
        if (!todayEntries.isEmpty()) {
            Label todayHeader = new Label("📋 Today's Entries");
            todayHeader.setFont(Font.font("System", FontWeight.BOLD, 13));
            todayHeader.setPadding(new Insets(10, 0, 0, 0));
            contentBox.getChildren().add(todayHeader);
            for (TimeEntry te : todayEntries) {
                HBox row = new HBox(8);
                row.setPadding(new Insets(2, 10, 2, 10));
                Label nl = new Label(te.getActivityName() + " — " + te.getFormattedDuration());
                HBox.setHgrow(nl, Priority.ALWAYS);
                Label cl = new Label(te.getCategory() != null ? te.getCategory() : "");
                cl.setTextFill(Color.GRAY);
                Button delBtn = new Button("✕");
                delBtn.getStyleClass().add("delete-button");
                delBtn.setOnAction(e -> { timeEntryDAO.delete(te.getId()); buildUI(); });
                row.getChildren().addAll(nl, cl, delBtn);
                contentBox.getChildren().add(row);
            }
        }
    }

    private String formatMins(int mins) {
        int h = mins / 60, m = mins % 60;
        if (h > 0 && m > 0) return h + "h " + m + "m";
        if (h > 0) return h + "h";
        return m + "m";
    }
}
