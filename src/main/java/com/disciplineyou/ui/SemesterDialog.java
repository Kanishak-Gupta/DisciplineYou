package com.disciplineyou.ui;

import com.disciplineyou.dao.SubjectDAO;
import com.disciplineyou.model.Semester;
import com.disciplineyou.model.Subject;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.util.List;

/**
 * Dialog/section for managing semesters and subjects.
 */
public class SemesterDialog extends VBox {

    private final SubjectDAO subjectDAO = new SubjectDAO();
    private VBox contentBox;
    private Runnable onUpdate;

    public SemesterDialog(Runnable onUpdate) {
        this.onUpdate = onUpdate;
        setSpacing(10);
        setPadding(new Insets(10));
        contentBox = new VBox(10);
        getChildren().add(contentBox);
        refresh();
    }

    public void refresh() {
        contentBox.getChildren().clear();

        // Semester list
        List<Semester> semesters = subjectDAO.getAllSemesters();

        if (semesters.isEmpty()) {
            Label empty = new Label("No semesters yet. Create one to get started!");
            empty.setTextFill(Color.GRAY);
            contentBox.getChildren().add(empty);
        }

        for (Semester sem : semesters) {
            TitledPane semPane = createSemesterPane(sem);
            contentBox.getChildren().add(semPane);
        }

        // Add semester button
        Button addSemBtn = new Button("➕ Add Semester");
        addSemBtn.getStyleClass().add("accent-button");
        addSemBtn.setOnAction(e -> showAddSemesterDialog());
        contentBox.getChildren().add(addSemBtn);
    }

    private TitledPane createSemesterPane(Semester sem) {
        VBox semContent = new VBox(8);
        semContent.setPadding(new Insets(10));

        // Semester info
        HBox infoRow = new HBox(10);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        Label dateRange = new Label(sem.getStartDate() + " to " + sem.getEndDate());
        dateRange.setTextFill(Color.GRAY);

        Button activateBtn;
        if (sem.isActive()) {
            activateBtn = new Button("✓ Active");
            activateBtn.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32;");
            activateBtn.setDisable(true);
        } else {
            activateBtn = new Button("Set Active");
            activateBtn.setOnAction(e -> {
                subjectDAO.setActiveSemester(sem.getId());
                refresh();
                if (onUpdate != null) onUpdate.run();
            });
        }

        Button delSemBtn = new Button("🗑");
        delSemBtn.getStyleClass().add("delete-button");
        delSemBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete semester '" + sem.getName() + "' and all its data?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    subjectDAO.deleteSemester(sem.getId());
                    refresh();
                    if (onUpdate != null) onUpdate.run();
                }
            });
        });

        infoRow.getChildren().addAll(dateRange, activateBtn, delSemBtn);
        semContent.getChildren().add(infoRow);

        // Subjects
        List<Subject> subjects = subjectDAO.getSubjectsBySemester(sem.getId());
        for (Subject subj : subjects) {
            HBox subjRow = new HBox(8);
            subjRow.setAlignment(Pos.CENTER_LEFT);
            subjRow.setPadding(new Insets(2, 0, 2, 15));

            Label nameLabel = new Label("• " + subj.getName());
            nameLabel.setFont(Font.font("System", FontWeight.MEDIUM, 12));
            Label codeLabel = new Label("[" + subj.getShortCode() + "]");
            codeLabel.setTextFill(Color.web("#2196F3"));
            Label facultyLabel = new Label("👤 " + (subj.getFaculty() != null ? subj.getFaculty() : "—"));
            facultyLabel.setTextFill(Color.GRAY);

            Button editSubjBtn = new Button("✏");
            editSubjBtn.getStyleClass().add("icon-button");
            editSubjBtn.setOnAction(e -> showEditSubjectDialog(subj, sem.getId()));

            Button delSubjBtn = new Button("✕");
            delSubjBtn.getStyleClass().add("delete-button");
            delSubjBtn.setOnAction(e -> {
                subjectDAO.deleteSubject(subj.getId());
                refresh();
                if (onUpdate != null) onUpdate.run();
            });

            subjRow.getChildren().addAll(nameLabel, codeLabel, facultyLabel, editSubjBtn, delSubjBtn);
            semContent.getChildren().add(subjRow);
        }

        // Add subject button
        Button addSubjBtn = new Button("+ Add Subject");
        addSubjBtn.getStyleClass().add("link-button");
        addSubjBtn.setOnAction(e -> showAddSubjectDialog(sem.getId()));
        semContent.getChildren().add(addSubjBtn);

        TitledPane pane = new TitledPane(sem.getName() + (sem.isActive() ? " ✓" : ""), semContent);
        pane.setExpanded(sem.isActive());
        return pane;
    }

    private void showAddSemesterDialog() {
        Dialog<Semester> dialog = new Dialog<>();
        dialog.setTitle("Add Semester");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Sem 4");
        DatePicker startPicker = new DatePicker(LocalDate.now());
        DatePicker endPicker = new DatePicker(LocalDate.now().plusMonths(5));

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Start:"), 0, 1);
        grid.add(startPicker, 1, 1);
        grid.add(new Label("End:"), 0, 2);
        grid.add(endPicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                return new Semester(nameField.getText().trim(), startPicker.getValue(), endPicker.getValue(), true);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(sem -> {
            subjectDAO.insertSemester(sem);
            subjectDAO.setActiveSemester(sem.getId());
            refresh();
            if (onUpdate != null) onUpdate.run();
        });
    }

    private void showAddSubjectDialog(int semesterId) {
        Dialog<Subject> dialog = new Dialog<>();
        dialog.setTitle("Add Subject");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Data Structures");
        TextField codeField = new TextField();
        codeField.setPromptText("e.g., DSA");
        TextField facultyField = new TextField();
        facultyField.setPromptText("e.g., Prof. Sharma");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Short Code:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("Faculty:"), 0, 2);
        grid.add(facultyField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                return new Subject(semesterId, nameField.getText().trim(), codeField.getText().trim(), facultyField.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(subj -> {
            subjectDAO.insertSubject(subj);
            refresh();
            if (onUpdate != null) onUpdate.run();
        });
    }

    private void showEditSubjectDialog(Subject subj, int semesterId) {
        Dialog<Subject> dialog = new Dialog<>();
        dialog.setTitle("Edit Subject");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(subj.getName());
        TextField codeField = new TextField(subj.getShortCode());
        TextField facultyField = new TextField(subj.getFaculty());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Short Code:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("Faculty:"), 0, 2);
        grid.add(facultyField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !nameField.getText().trim().isEmpty()) {
                subj.setName(nameField.getText().trim());
                subj.setShortCode(codeField.getText().trim());
                subj.setFaculty(facultyField.getText().trim());
                return subj;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            subjectDAO.updateSubject(s);
            refresh();
            if (onUpdate != null) onUpdate.run();
        });
    }
}
