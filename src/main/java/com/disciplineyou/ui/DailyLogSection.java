package com.disciplineyou.ui;

import com.disciplineyou.dao.DailyLogDAO;
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
import java.util.*;

/**
 * Daily log section for tracking social media, movies, college time,
 * and study time per subject.
 */
public class DailyLogSection extends VBox {

    private final DailyLogDAO dailyLogDAO = new DailyLogDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();

    private LocalDate currentDate = LocalDate.now();
    private TextField socialMediaField;
    private TextField movieField;
    private TextField collegeField;
    private TextArea notesField;
    private VBox studyFieldsBox;
    private Map<Integer, TextField> studyFields = new HashMap<>();
    private Label dateLabel;

    public DailyLogSection() {
        setSpacing(10);
        setPadding(new Insets(10));
        buildUI();
        loadData();
    }

    private void buildUI() {
        // Date navigation
        HBox dateNav = new HBox(10);
        dateNav.setAlignment(Pos.CENTER);

        Button prevBtn = new Button("◀");
        prevBtn.getStyleClass().add("icon-button");
        prevBtn.setOnAction(e -> { currentDate = currentDate.minusDays(1); loadData(); });

        dateLabel = new Label();
        dateLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        dateLabel.setTextFill(Color.web("#2196F3"));

        Button nextBtn = new Button("▶");
        nextBtn.getStyleClass().add("icon-button");
        nextBtn.setOnAction(e -> { currentDate = currentDate.plusDays(1); loadData(); });

        Button todayBtn = new Button("Today");
        todayBtn.getStyleClass().add("link-button");
        todayBtn.setOnAction(e -> { currentDate = LocalDate.now(); loadData(); });

        dateNav.getChildren().addAll(prevBtn, dateLabel, nextBtn, todayBtn);
        getChildren().add(dateNav);

        // Activity fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(5));

        socialMediaField = createMinuteField();
        movieField = createMinuteField();
        collegeField = createMinuteField();

        grid.add(new Label("📱 Social Media:"), 0, 0);
        grid.add(socialMediaField, 1, 0);
        grid.add(new Label("mins"), 2, 0);

        grid.add(new Label("🎬 Movies/Shows:"), 0, 1);
        grid.add(movieField, 1, 1);
        grid.add(new Label("mins"), 2, 1);

        grid.add(new Label("🏫 College Time:"), 0, 2);
        grid.add(collegeField, 1, 2);
        grid.add(new Label("mins"), 2, 2);

        getChildren().add(grid);

        // Study time per subject
        Label studyHeader = new Label("📚 Study Time (per subject)");
        studyHeader.setFont(Font.font("System", FontWeight.BOLD, 13));
        studyHeader.setPadding(new Insets(10, 0, 5, 0));
        getChildren().add(studyHeader);

        studyFieldsBox = new VBox(5);
        getChildren().add(studyFieldsBox);

        // Notes
        Label notesLabel = new Label("📝 Notes");
        notesLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        notesLabel.setPadding(new Insets(10, 0, 5, 0));
        getChildren().add(notesLabel);

        notesField = new TextArea();
        notesField.setPromptText("Any notes about your day...");
        notesField.setPrefRowCount(2);
        notesField.setWrapText(true);
        getChildren().add(notesField);

        // Save button
        Button saveBtn = new Button("💾 Save Log");
        saveBtn.getStyleClass().add("accent-button");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> saveData());
        getChildren().add(saveBtn);
    }

    public void loadData() {
        dateLabel.setText(currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")));

        DailyLog log = dailyLogDAO.getByDate(currentDate);
        if (log != null) {
            socialMediaField.setText(String.valueOf(log.getSocialMediaMins()));
            movieField.setText(String.valueOf(log.getMovieMins()));
            collegeField.setText(String.valueOf(log.getCollegeMins()));
            notesField.setText(log.getNotes() != null ? log.getNotes() : "");
        } else {
            socialMediaField.setText("0");
            movieField.setText("0");
            collegeField.setText("0");
            notesField.setText("");
        }

        // Build study fields for active semester subjects
        studyFieldsBox.getChildren().clear();
        studyFields.clear();

        Semester activeSem = subjectDAO.getActiveSemester();
        if (activeSem != null) {
            List<Subject> subjects = subjectDAO.getSubjectsBySemester(activeSem.getId());
            List<StudyLog> existingLogs = log != null ? dailyLogDAO.getStudyLogs(log.getId()) : new ArrayList<>();
            Map<Integer, Integer> existingMins = new HashMap<>();
            for (StudyLog sl : existingLogs) {
                existingMins.put(sl.getSubjectId(), sl.getDurationMins());
            }

            GridPane studyGrid = new GridPane();
            studyGrid.setHgap(10);
            studyGrid.setVgap(5);
            int row = 0;
            for (Subject subj : subjects) {
                Label label = new Label("  " + subj.toString() + ":");
                TextField field = createMinuteField();
                field.setText(String.valueOf(existingMins.getOrDefault(subj.getId(), 0)));
                studyFields.put(subj.getId(), field);
                studyGrid.add(label, 0, row);
                studyGrid.add(field, 1, row);
                studyGrid.add(new Label("mins"), 2, row);
                row++;
            }
            studyFieldsBox.getChildren().add(studyGrid);
        } else {
            Label noSem = new Label("No active semester. Go to Semester & Subjects to set one up.");
            noSem.setTextFill(Color.GRAY);
            studyFieldsBox.getChildren().add(noSem);
        }
    }

    private void saveData() {
        try {
            DailyLog log = dailyLogDAO.getOrCreateForDate(currentDate);
            log.setSocialMediaMins(parseMinutes(socialMediaField.getText()));
            log.setMovieMins(parseMinutes(movieField.getText()));
            log.setCollegeMins(parseMinutes(collegeField.getText()));
            log.setNotes(notesField.getText().trim());
            dailyLogDAO.update(log);

            // Save study logs
            List<StudyLog> studyLogs = new ArrayList<>();
            for (Map.Entry<Integer, TextField> entry : studyFields.entrySet()) {
                StudyLog sl = new StudyLog(log.getId(), entry.getKey(), parseMinutes(entry.getValue().getText()));
                studyLogs.add(sl);
            }
            dailyLogDAO.saveStudyLogs(log.getId(), studyLogs);

            // Show success feedback
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Daily log saved!", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error saving: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private TextField createMinuteField() {
        TextField field = new TextField("0");
        field.setPrefWidth(80);
        field.setAlignment(Pos.CENTER_RIGHT);
        // Only allow numbers
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
        return field;
    }

    private int parseMinutes(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
