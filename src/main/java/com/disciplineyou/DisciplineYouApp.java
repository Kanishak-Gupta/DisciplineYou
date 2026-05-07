package com.disciplineyou;

import com.disciplineyou.db.DatabaseManager;
import com.disciplineyou.model.Goal;
import com.disciplineyou.service.GoalShiftService;
import com.disciplineyou.ui.MainView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

/**
 * Main entry point for the DisciplineYou application.
 * Initializes the database, auto-shifts overdue goals, and launches the UI.
 */
public class DisciplineYouApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize database (creates tables if first run)
        DatabaseManager.getInstance();

        // Auto-shift overdue daily goals
        GoalShiftService shiftService = new GoalShiftService();
        List<Goal> shiftedGoals = shiftService.shiftOverdueGoals();

        // Build the main UI
        MainView mainView = new MainView();

        Scene scene = new Scene(mainView, 850, 700);

        // Load CSS
        String css = getClass().getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("DisciplineYou — Self Analysis & Day Tracker");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);

        // Register keyboard shortcuts
        mainView.registerShortcuts(scene);

        primaryStage.show();

        // Show shift notification if any goals were moved
        if (!shiftedGoals.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    shiftedGoals.size() + " overdue goal(s) were shifted to today.",
                    javafx.scene.control.ButtonType.OK);
            alert.setHeaderText("⚠ Goals Auto-Shifted");
            alert.showAndWait();
        }
    }

    @Override
    public void stop() {
        // Close database connection on exit
        DatabaseManager.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
