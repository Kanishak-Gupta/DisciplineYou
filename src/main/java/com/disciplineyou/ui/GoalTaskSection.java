package com.disciplineyou.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Container section with sub-tabs: Tasks, Time Expenditure, Gamification.
 * Wraps the new GoalTaskListSection and TimeExpenditureSection.
 */
public class GoalTaskSection extends VBox {

    private final GoalTaskListSection taskListSection;
    private final TimeExpenditureSection timeSection;
    private final GamificationSection gamSection;

    public GoalTaskSection() {
        setSpacing(0);
        setPadding(new Insets(0));

        taskListSection = new GoalTaskListSection();
        timeSection = new TimeExpenditureSection();
        gamSection = new GamificationSection();

        TabPane subTabs = new TabPane();
        subTabs.getStyleClass().add("main-tabs");
        subTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tasksTab = new Tab("🎯 Tasks");
        ScrollPane taskScroll = new ScrollPane(taskListSection);
        taskScroll.setFitToWidth(true);
        taskScroll.getStyleClass().add("scroll-content");
        tasksTab.setContent(taskScroll);

        Tab timeTab = new Tab("⏱ Time");
        ScrollPane timeScroll = new ScrollPane(timeSection);
        timeScroll.setFitToWidth(true);
        timeScroll.getStyleClass().add("scroll-content");
        timeTab.setContent(timeScroll);

        Tab gamTab = new Tab("🏅 Achievements");
        ScrollPane gamScroll = new ScrollPane(gamSection);
        gamScroll.setFitToWidth(true);
        gamScroll.getStyleClass().add("scroll-content");
        gamTab.setContent(gamScroll);

        subTabs.getTabs().addAll(tasksTab, timeTab, gamTab);

        // Refresh on tab switch
        subTabs.getSelectionModel().selectedItemProperty().addListener((obs, old, newTab) -> {
            if (newTab == tasksTab) taskListSection.refresh();
            else if (newTab == timeTab) timeSection.refresh();
            else if (newTab == gamTab) gamSection.refresh();
        });

        VBox.setVgrow(subTabs, Priority.ALWAYS);
        getChildren().add(subTabs);
    }

    public void refresh() {
        taskListSection.refresh();
    }

    public GoalTaskListSection getTaskListSection() { return taskListSection; }
    public TimeExpenditureSection getTimeSection() { return timeSection; }
    public GamificationSection getGamSection() { return gamSection; }
}
