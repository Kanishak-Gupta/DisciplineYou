package com.disciplineyou.ui;

import com.disciplineyou.model.Achievement;
import com.disciplineyou.service.GamificationService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gamification section with streaks, achievements, and weekly leaderboard.
 */
public class GamificationSection extends VBox {

    private final GamificationService gamService = new GamificationService();
    private VBox contentBox;

    public GamificationSection() {
        setSpacing(10);
        setPadding(new Insets(10));
        contentBox = new VBox(10);
        getChildren().add(contentBox);
        gamService.initDefaultAchievements();
        refresh();
    }

    public void refresh() {
        contentBox.getChildren().clear();

        Label header = new Label("🏅 Gamification");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        contentBox.getChildren().add(header);

        // Streak display
        int currentStreak = gamService.getCurrentStreak();
        int bestStreak = gamService.getBestStreak();

        HBox streakBar = new HBox(20);
        streakBar.setAlignment(Pos.CENTER);
        streakBar.setPadding(new Insets(12));
        streakBar.setStyle("-fx-background-color: linear-gradient(to right, #FF9800, #F44336); -fx-background-radius: 8;");

        Label fireLabel = new Label("🔥");
        fireLabel.setFont(Font.font(28));

        VBox streakInfo = new VBox(2);
        streakInfo.setAlignment(Pos.CENTER);
        Label streakNum = new Label(currentStreak + " day streak");
        streakNum.setFont(Font.font("System", FontWeight.BOLD, 18));
        streakNum.setTextFill(Color.WHITE);
        Label bestLabel = new Label("Best: " + bestStreak + " days");
        bestLabel.setFont(Font.font(12));
        bestLabel.setTextFill(Color.web("#FFECB3"));
        streakInfo.getChildren().addAll(streakNum, bestLabel);

        streakBar.getChildren().addAll(fireLabel, streakInfo);
        contentBox.getChildren().add(streakBar);

        // Achievements grid
        Label achHeader = new Label("🎖 Achievements");
        achHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
        contentBox.getChildren().add(achHeader);

        FlowPane achGrid = new FlowPane(10, 10);
        achGrid.setPadding(new Insets(5));

        List<Achievement> achievements = gamService.getAllAchievements();
        for (Achievement a : achievements) {
            VBox badge = new VBox(4);
            badge.setAlignment(Pos.CENTER);
            badge.setPadding(new Insets(10, 14, 10, 14));
            badge.setPrefWidth(130);
            badge.setStyle(a.isUnlocked()
                    ? "-fx-background-color: #E8F5E9; -fx-background-radius: 8; -fx-border-color: #4CAF50; -fx-border-radius: 8;"
                    : "-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8;");

            Label iconL = new Label(a.getIcon());
            iconL.setFont(Font.font(24));
            if (!a.isUnlocked()) iconL.setOpacity(0.3);

            Label nameL = new Label(a.getName());
            nameL.setFont(Font.font("System", FontWeight.BOLD, 11));
            nameL.setTextFill(a.isUnlocked() ? Color.web("#2E7D32") : Color.GRAY);
            nameL.setWrapText(true);

            Label descL = new Label(a.getDescription());
            descL.setFont(Font.font(9));
            descL.setTextFill(Color.GRAY);
            descL.setWrapText(true);

            badge.getChildren().addAll(iconL, nameL, descL);
            if (a.isUnlocked()) {
                Label dateL = new Label("✓ " + a.getUnlockedAt().format(DateTimeFormatter.ofPattern("MMM dd")));
                dateL.setFont(Font.font(9));
                dateL.setTextFill(Color.web("#4CAF50"));
                badge.getChildren().add(dateL);
            }
            achGrid.getChildren().add(badge);
        }
        contentBox.getChildren().add(achGrid);

        // Weekly leaderboard
        Label lbHeader = new Label("📊 Weekly Self-Leaderboard");
        lbHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
        contentBox.getChildren().add(lbHeader);

        List<int[]> weeklyStats = gamService.getWeeklyProductivity(5);
        String[] weekLabels = {"This Week", "Last Week", "2 Weeks Ago", "3 Weeks Ago", "4 Weeks Ago"};

        for (int i = 0; i < weeklyStats.size(); i++) {
            int[] s = weeklyStats.get(i);
            double pct = s[1] > 0 ? (double) s[0] / s[1] : 0;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 10, 4, 10));

            Label weekL = new Label(weekLabels[i]);
            weekL.setPrefWidth(110);
            weekL.setFont(Font.font("System", i == 0 ? FontWeight.BOLD : FontWeight.NORMAL, 12));

            javafx.scene.control.ProgressBar bar = new javafx.scene.control.ProgressBar(pct);
            bar.setPrefWidth(150);
            bar.setPrefHeight(8);
            if (i == 0) bar.setStyle("-fx-accent: #1E88E5;");

            Label statL = new Label(s[0] + "/" + s[1] + " (" + (int)(pct * 100) + "%)");
            statL.setFont(Font.font(11));
            statL.setTextFill(Color.GRAY);

            row.getChildren().addAll(weekL, bar, statL);
            contentBox.getChildren().add(row);
        }
    }
}
