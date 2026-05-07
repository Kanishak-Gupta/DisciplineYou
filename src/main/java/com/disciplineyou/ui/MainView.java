package com.disciplineyou.ui;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.service.GamificationService;
import com.disciplineyou.service.GoalShiftService;
import com.disciplineyou.service.NotificationService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;

/**
 * Main application view with header, tabs, dark mode toggle, notifications, and search.
 */
public class MainView extends BorderPane {

    private GoalTaskSection goalTaskSection;
    private GoalSection goalSection;
    private DailyLogSection dailyLogSection;
    private TimetableSection timetableSection;
    private AttendanceSection attendanceSection;
    private AbsentLogSection absentLogSection;
    private final NotificationService notifService = new NotificationService();
    private boolean darkMode = false;
    private Label notifBadge;
    private Popup notifPopup;

    public MainView() {
        // Load dark mode pref
        String darkPref = DatabaseManager.getInstance().getSetting("dark_mode");
        darkMode = "true".equals(darkPref);

        buildHeader();
        buildContent();

        // Shift overdue goals on startup
        new GoalShiftService().shiftOverdueGoals();

        // Init gamification
        new GamificationService().initDefaultAchievements();

        // Generate notifications
        notifService.generateNotifications();
        updateNotifBadge();

        if (darkMode) applyDarkMode(true);
    }

    private void buildHeader() {
        HBox header = new HBox(12);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");

        Label titleLabel = new Label("📘 DisciplineYou");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        titleLabel.setTextFill(Color.WHITE);

        Label subLabel = new Label("Self Analysis & Productivity");
        subLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
        subLabel.setTextFill(Color.web("#B3D4FC"));

        VBox titleBox = new VBox(2);
        titleBox.getChildren().addAll(titleLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search button
        Button searchBtn = new Button("🔍");
        searchBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 10; -fx-cursor: hand;");
        searchBtn.setTooltip(new Tooltip("Search (Ctrl+F)"));
        searchBtn.setOnAction(e -> showSearchDialog());

        // Notification bell
        StackPane bellStack = new StackPane();
        Button bellBtn = new Button("🔔");
        bellBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 10; -fx-cursor: hand;");
        bellBtn.setTooltip(new Tooltip("Notifications (Ctrl+Shift+N)"));
        bellBtn.setOnAction(e -> toggleNotifications(bellBtn));

        notifBadge = new Label("0");
        notifBadge.setFont(Font.font("System", FontWeight.BOLD, 9));
        notifBadge.setTextFill(Color.WHITE);
        notifBadge.setStyle("-fx-background-color: #F44336; -fx-background-radius: 8; -fx-padding: 1 4;");
        notifBadge.setVisible(false);
        StackPane.setAlignment(notifBadge, Pos.TOP_RIGHT);
        bellStack.getChildren().addAll(bellBtn, notifBadge);

        // Dark mode toggle
        Button darkToggle = new Button(darkMode ? "☀️" : "🌙");
        darkToggle.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 10; -fx-cursor: hand;");
        darkToggle.setTooltip(new Tooltip("Toggle Dark Mode"));
        darkToggle.setOnAction(e -> {
            darkMode = !darkMode;
            darkToggle.setText(darkMode ? "☀️" : "🌙");
            applyDarkMode(darkMode);
            DatabaseManager.getInstance().setSetting("dark_mode", String.valueOf(darkMode));
        });

        header.getChildren().addAll(titleBox, spacer, searchBtn, bellStack, darkToggle);
        setTop(header);
    }

    private void buildContent() {
        TabPane mainTabs = new TabPane();
        mainTabs.getStyleClass().add("main-tabs");
        mainTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Goals & Tasks tab
        goalTaskSection = new GoalTaskSection();
        Tab goalsTab = new Tab("🎯 Goals & Tasks");
        goalsTab.setContent(goalTaskSection);

        // Daily Log + Goals tab
        goalSection = new GoalSection();
        dailyLogSection = new DailyLogSection();

        VBox dailyContent = new VBox(5);
        TitledPane goalsPane = new TitledPane("🎯 Monthly / Weekly / Daily Goals", goalSection);
        goalsPane.getStyleClass().add("section-pane");
        goalsPane.setExpanded(false);
        TitledPane dailyLogPane = new TitledPane("📊 Daily Log", dailyLogSection);
        dailyLogPane.getStyleClass().add("section-pane");
        dailyContent.getChildren().addAll(goalsPane, dailyLogPane);

        ScrollPane dailyScroll = new ScrollPane(dailyContent);
        dailyScroll.setFitToWidth(true);
        dailyScroll.getStyleClass().add("scroll-content");

        Tab dailyTab = new Tab("📊 Daily Log");
        dailyTab.setContent(dailyScroll);

        // Academic tab
        timetableSection = new TimetableSection();
        attendanceSection = new AttendanceSection();
        absentLogSection = new AbsentLogSection();

        VBox academicContent = new VBox(5);
        TitledPane timetablePane = new TitledPane("📅 Timetable & Attendance", timetableSection);
        timetablePane.getStyleClass().add("section-pane");
        TitledPane attendancePane = new TitledPane("📊 Attendance Stats", attendanceSection);
        attendancePane.getStyleClass().add("section-pane");
        TitledPane absentPane = new TitledPane("📋 Absent Class Log", absentLogSection);
        absentPane.getStyleClass().add("section-pane");
        absentPane.setExpanded(false);

        Button semBtn = new Button("⚙ Semester & Subjects");
        semBtn.getStyleClass().add("accent-button");
        semBtn.setOnAction(e -> {
            Dialog<Void> semDialog = new Dialog<>();
            semDialog.setTitle("Semester & Subjects");
            semDialog.setResizable(true);
            SemesterDialog semContent = new SemesterDialog(() -> {
                timetableSection.refresh();
                attendanceSection.refresh();
                absentLogSection.refresh();
            });
            ScrollPane sp2 = new ScrollPane(semContent);
            sp2.setFitToWidth(true);
            sp2.setPrefWidth(500);
            sp2.setPrefHeight(400);
            semDialog.getDialogPane().setContent(sp2);
            semDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            semDialog.showAndWait();
            timetableSection.refresh();
            attendanceSection.refresh();
            absentLogSection.refresh();
        });

        academicContent.getChildren().addAll(semBtn, timetablePane, attendancePane, absentPane);
        ScrollPane academicScroll = new ScrollPane(academicContent);
        academicScroll.setFitToWidth(true);
        academicScroll.getStyleClass().add("scroll-content");

        Tab academicTab = new Tab("🏫 Academic");
        academicTab.setContent(academicScroll);

        mainTabs.getTabs().addAll(goalsTab, dailyTab, academicTab);

        // Tab switch refresh
        mainTabs.getSelectionModel().selectedItemProperty().addListener((obs, old, newTab) -> {
            if (newTab == goalsTab) goalTaskSection.refresh();
            else if (newTab == dailyTab) { goalSection.refresh(); dailyLogSection.loadData(); }
            else if (newTab == academicTab) { timetableSection.refresh(); attendanceSection.refresh(); absentLogSection.refresh(); }
        });

        setCenter(mainTabs);

        // Register keyboard shortcuts
        setOnKeyPressed(event -> {
            // Already handled by accelerators, but this is a fallback
        });
    }

    public void registerShortcuts(javafx.scene.Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), this::showSearchDialog);
    }

    private void applyDarkMode(boolean dark) {
        if (dark) {
            if (!getStyleClass().contains("dark-mode")) getStyleClass().add("dark-mode");
        } else {
            getStyleClass().remove("dark-mode");
        }
    }

    private void updateNotifBadge() {
        int count = notifService.getUnreadCount();
        notifBadge.setText(String.valueOf(count));
        notifBadge.setVisible(count > 0);
    }

    private void toggleNotifications(Button anchor) {
        if (notifPopup != null && notifPopup.isShowing()) {
            notifPopup.hide();
            notifPopup = null;
            return;
        }
        notifService.generateNotifications();
        NotificationPanel panel = new NotificationPanel(notifService);

        notifPopup = new Popup();
        notifPopup.setAutoHide(true);
        notifPopup.getContent().add(panel);
        notifPopup.setOnHidden(e -> updateNotifBadge());

        javafx.geometry.Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds != null) {
            notifPopup.show(anchor, bounds.getMinX() - 300, bounds.getMaxY() + 5);
        }
    }

    private void showSearchDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search");
        dialog.setHeaderText("🔍 Search across tasks, timetable, and activities");
        dialog.setContentText("Query:");
        dialog.showAndWait().ifPresent(query -> {
            if (!query.trim().isEmpty()) {
                Alert results = new Alert(Alert.AlertType.INFORMATION);
                results.setTitle("Search Results");
                results.setHeaderText("Results for: " + query);

                StringBuilder sb = new StringBuilder();
                // Search goal tasks
                var tasks = new com.disciplineyou.dao.GoalTaskDAO().getAll();
                tasks.stream().filter(t -> t.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(query.toLowerCase())))
                        .forEach(t -> sb.append("📋 Task: ").append(t.getTitle()).append(" (").append(t.getStatus()).append(")\n"));

                // Search time entries
                var entries = new com.disciplineyou.dao.TimeEntryDAO().getEntriesForDateRange(
                        java.time.LocalDate.now().minusMonths(3), java.time.LocalDate.now());
                entries.stream().filter(e -> e.getActivityName().toLowerCase().contains(query.toLowerCase()))
                        .limit(10)
                        .forEach(e -> sb.append("⏱ Activity: ").append(e.getActivityName()).append(" (").append(e.getFormattedDuration()).append(")\n"));

                results.setContentText(sb.length() > 0 ? sb.toString() : "No results found.");
                results.showAndWait();
            }
        });
    }
}
