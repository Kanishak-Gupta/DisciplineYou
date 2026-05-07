package com.disciplineyou.ui;

import com.disciplineyou.dao.AbsentLogDAO;
import com.disciplineyou.dao.SubjectDAO;
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
import java.util.List;
import java.util.Map;

/**
 * Absent class log section with analytics.
 */
public class AbsentLogSection extends VBox {

    private final AbsentLogDAO absentLogDAO = new AbsentLogDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private VBox contentBox;

    public AbsentLogSection() {
        setSpacing(8);
        setPadding(new Insets(10));
        contentBox = new VBox(8);
        getChildren().add(contentBox);
        refresh();
    }

    public void refresh() {
        contentBox.getChildren().clear();

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("📋 Absent Class Log");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button addBtn = new Button("➕ Log Absence");
        addBtn.getStyleClass().add("accent-button");
        addBtn.setOnAction(e -> showLogDialog());
        header.getChildren().addAll(title, sp, addBtn);
        contentBox.getChildren().add(header);

        // Subject-wise stats
        Map<String, Integer> stats = absentLogDAO.getSubjectWiseAbsenceCount();
        if (!stats.isEmpty()) {
            HBox statsBar = new HBox(15);
            statsBar.setPadding(new Insets(6, 10, 6, 10));
            statsBar.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 6;");
            for (var entry : stats.entrySet()) {
                Label sl = new Label(entry.getKey() + ": " + entry.getValue() + " absence(s)");
                sl.setFont(Font.font(11));
                sl.setTextFill(entry.getValue() >= 5 ? Color.web("#F44336") : Color.web("#E65100"));
                statsBar.getChildren().add(sl);
            }
            contentBox.getChildren().add(statsBar);
        }

        // Log entries
        List<AbsentLog> logs = absentLogDAO.getAll();
        if (logs.isEmpty()) {
            Label empty = new Label("No absences logged. Great attendance!");
            empty.setTextFill(Color.GRAY);
            contentBox.getChildren().add(empty);
        }
        for (AbsentLog log : logs) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 10, 4, 10));
            row.getStyleClass().add("goal-row");

            Label dateL = new Label(log.getAbsentDate().format(DateTimeFormatter.ofPattern("MMM dd")));
            dateL.setFont(Font.font("System", FontWeight.BOLD, 12));
            dateL.setPrefWidth(60);

            Label subjL = new Label(log.getSubjectName() != null ? log.getSubjectName() : "—");
            subjL.setPrefWidth(120);

            Label reasonL = new Label(log.getReasonDisplay());
            reasonL.setPrefWidth(120);

            Label notesL = new Label(log.getNotes() != null ? log.getNotes() : "");
            notesL.setTextFill(Color.GRAY);
            HBox.setHgrow(notesL, Priority.ALWAYS);

            Button delBtn = new Button("✕");
            delBtn.getStyleClass().add("delete-button");
            delBtn.setOnAction(e -> { absentLogDAO.delete(log.getId()); refresh(); });

            row.getChildren().addAll(dateL, subjL, reasonL, notesL, delBtn);
            contentBox.getChildren().add(row);
        }
    }

    private void showLogDialog() {
        Semester sem = subjectDAO.getActiveSemester();
        if (sem == null) {
            new Alert(Alert.AlertType.WARNING, "No active semester found.", ButtonType.OK).showAndWait();
            return;
        }
        List<Subject> subjects = subjectDAO.getSubjectsBySemester(sem.getId());
        if (subjects.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No subjects found.", ButtonType.OK).showAndWait();
            return;
        }

        Dialog<AbsentLog> dialog = new Dialog<>();
        dialog.setTitle("Log Absence");
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(15));

        ComboBox<Subject> subjCombo = new ComboBox<>();
        subjCombo.getItems().addAll(subjects);
        subjCombo.setValue(subjects.get(0));
        DatePicker dateP = new DatePicker(LocalDate.now());
        ComboBox<String> reasonCombo = new ComboBox<>();
        reasonCombo.getItems().addAll("SICK", "PERSONAL_WORK", "EMERGENCY", "OTHER");
        reasonCombo.setValue("SICK");
        TextField notesF = new TextField();
        notesF.setPromptText("Optional notes");

        g.add(new Label("Subject:"), 0, 0); g.add(subjCombo, 1, 0);
        g.add(new Label("Date:"), 0, 1); g.add(dateP, 1, 1);
        g.add(new Label("Reason:"), 0, 2); g.add(reasonCombo, 1, 2);
        g.add(new Label("Notes:"), 0, 3); g.add(notesF, 1, 3);

        dialog.getDialogPane().setContent(g);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK)
                return new AbsentLog(subjCombo.getValue().getId(), dateP.getValue(), reasonCombo.getValue(), notesF.getText().trim());
            return null;
        });
        dialog.showAndWait().ifPresent(log -> { absentLogDAO.insert(log); refresh(); });
    }
}
