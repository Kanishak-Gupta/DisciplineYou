package com.disciplineyou.ui;

import com.disciplineyou.dao.AttendanceDAO;
import com.disciplineyou.dao.SubjectDAO;
import com.disciplineyou.model.Semester;
import com.disciplineyou.model.Subject;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * Attendance summary section showing percentage per subject
 * with color-coded progress bars.
 */
public class AttendanceSection extends VBox {

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private VBox contentBox;

    public AttendanceSection() {
        setSpacing(5);
        setPadding(new Insets(10));
        contentBox = new VBox(8);
        getChildren().add(contentBox);
    }

    public void refresh() {
        contentBox.getChildren().clear();

        Semester activeSem = subjectDAO.getActiveSemester();
        if (activeSem == null) {
            Label noSem = new Label("No active semester.");
            noSem.setTextFill(Color.GRAY);
            contentBox.getChildren().add(noSem);
            return;
        }

        List<Subject> subjects = subjectDAO.getSubjectsBySemester(activeSem.getId());
        if (subjects.isEmpty()) {
            Label noSubj = new Label("No subjects added yet.");
            noSubj.setTextFill(Color.GRAY);
            contentBox.getChildren().add(noSubj);
            return;
        }

        for (Subject subj : subjects) {
            java.time.LocalDate endDate = java.time.LocalDate.now().isBefore(activeSem.getEndDate()) ? java.time.LocalDate.now() : activeSem.getEndDate();
            int[] stats = attendanceDAO.getCalculatedAttendanceStats(subj.getId(), activeSem.getStartDate(), endDate);
            int present = stats[0];
            int total = stats[1];
            double pct = total > 0 ? (double) present / total * 100 : 0;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(subj.toString());
            nameLabel.setFont(Font.font("System", FontWeight.MEDIUM, 13));
            nameLabel.setMinWidth(80);

            ProgressBar bar = new ProgressBar(total > 0 ? (double) present / total : 0);
            bar.setPrefWidth(200);
            bar.setPrefHeight(18);

            // Color code
            String barColor;
            if (pct >= 75) barColor = "#4CAF50";      // Green
            else if (pct >= 65) barColor = "#FF9800";  // Orange
            else barColor = "#F44336";                 // Red

            bar.setStyle("-fx-accent: " + barColor + ";");

            Label pctLabel = new Label(String.format("%.0f%% (%d/%d)", pct, present, total));
            pctLabel.setFont(Font.font(12));
            pctLabel.setMinWidth(100);

            if (pct < 75) {
                pctLabel.setTextFill(Color.web(barColor));
                pctLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            }

            row.getChildren().addAll(nameLabel, bar, pctLabel);
            contentBox.getChildren().add(row);
        }
    }
}
