package com.disciplineyou.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Main view with two tabs:
 *  1. "Goals & Tasks" — the new drill-down task section
 *  2. "Academic" — Timetable, Attendance, Semester/Subjects
 */
public class MainView extends BorderPane {

    private GoalTaskSection goalTaskSection;
    private TimetableSection timetableSection;
    private AttendanceSection attendanceSection;
    private SemesterDialog semesterDialog;

    public MainView() {
        getStyleClass().add("main-view");
        buildUI();
    }

    private void buildUI() {
        // ═══════════════════ HEADER ═══════════════════
        HBox header = new HBox(15);
        header.setPadding(new Insets(18, 25, 18, 25));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");

        Label logo = new Label("🎯");
        logo.setFont(Font.font(24));

        Label title = new Label("DisciplineYou");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        dateLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
        dateLabel.setTextFill(Color.web("#B3E5FC"));

        header.getChildren().addAll(logo, title, spacer, dateLabel);
        setTop(header);

        // ═══════════════════ TABS ═══════════════════
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setSide(Side.TOP);
        tabPane.getStyleClass().add("main-tabs");

        // ─── Tab 1: Goals & Tasks ───
        goalTaskSection = new GoalTaskSection();
        ScrollPane goalScroll = new ScrollPane(goalTaskSection);
        goalScroll.setFitToWidth(true);
        goalScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        goalScroll.getStyleClass().add("scroll-content");

        Tab goalsTab = new Tab("📋 Goals & Tasks", goalScroll);

        // ─── Tab 2: Academic ───
        VBox academicContent = new VBox(12);
        academicContent.setPadding(new Insets(15, 20, 20, 20));

        timetableSection = new TimetableSection();
        TitledPane timetablePane = new TitledPane("📅 Timetable", timetableSection);
        timetablePane.setExpanded(true);
        timetablePane.getStyleClass().add("section-pane");

        attendanceSection = new AttendanceSection();
        TitledPane attendancePane = new TitledPane("📈 Attendance Summary", attendanceSection);
        attendancePane.setExpanded(true);
        attendancePane.getStyleClass().add("section-pane");

        Runnable academicRefresh = () -> {
            timetableSection.refresh();
            attendanceSection.refresh();
        };
        semesterDialog = new SemesterDialog(academicRefresh);
        TitledPane semesterPane = new TitledPane("🏫 Semester & Subjects", semesterDialog);
        semesterPane.setExpanded(false);
        semesterPane.getStyleClass().add("section-pane");

        academicContent.getChildren().addAll(timetablePane, attendancePane, semesterPane);

        ScrollPane academicScroll = new ScrollPane(academicContent);
        academicScroll.setFitToWidth(true);
        academicScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        academicScroll.getStyleClass().add("scroll-content");

        Tab academicTab = new Tab("🏫 Academic", academicScroll);

        // Refresh data when switching tabs
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == academicTab) {
                timetableSection.refresh();
                attendanceSection.refresh();
            } else if (newTab == goalsTab) {
                goalTaskSection.refresh();
            }
        });

        tabPane.getTabs().addAll(goalsTab, academicTab);
        setCenter(tabPane);

        // Initial refresh
        timetableSection.refresh();
        attendanceSection.refresh();
    }

    /** Called on startup after auto-shift to show notifications */
    public void showShiftNotification(int shiftedCount) {
        if (shiftedCount > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Goals Shifted");
            alert.setHeaderText("⚠ " + shiftedCount + " overdue goal(s) shifted to today");
            alert.setContentText("You have unfinished goals that were automatically moved to today. Look for the ⚠ icon.");
            alert.showAndWait();
        }
    }

    public void refreshAll() {
        goalTaskSection.refresh();
        timetableSection.refresh();
        attendanceSection.refresh();
        semesterDialog.refresh();
    }
}
