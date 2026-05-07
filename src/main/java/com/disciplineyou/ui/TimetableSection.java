package com.disciplineyou.ui;

import com.disciplineyou.dao.*;
import com.disciplineyou.model.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Timetable section showing today's schedule with attendance marking,
 * plus dialogs for editing the whole timetable or adding week overrides.
 */
public class TimetableSection extends VBox {

    private final TimetableDAO timetableDAO = new TimetableDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final ExtraClassDAO extraClassDAO = new ExtraClassDAO();
    private VBox scheduleBox;
    private LocalDate currentDate = LocalDate.now();
    private Label dateLabel;

    public TimetableSection() {
        setSpacing(10);
        setPadding(new Insets(10));
        buildUI();
    }

    private void buildUI() {
        // Date navigation
        HBox dateNav = new HBox(10);
        dateNav.setAlignment(Pos.CENTER);

        Button prevBtn = new Button("◀");
        prevBtn.getStyleClass().add("icon-button");
        prevBtn.setOnAction(e -> { currentDate = currentDate.minusDays(1); refresh(); });

        dateLabel = new Label();
        dateLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        Button nextBtn = new Button("▶");
        nextBtn.getStyleClass().add("icon-button");
        nextBtn.setOnAction(e -> { currentDate = currentDate.plusDays(1); refresh(); });

        dateNav.getChildren().addAll(prevBtn, dateLabel, nextBtn);
        getChildren().add(dateNav);

        scheduleBox = new VBox(5);
        getChildren().add(scheduleBox);

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button editTimetableBtn = new Button("📋 Edit Semester Timetable");
        editTimetableBtn.getStyleClass().add("accent-button");
        editTimetableBtn.setOnAction(e -> showEditTimetableDialog());

        Button overrideBtn = new Button("🔄 Override This Week");
        overrideBtn.setOnAction(e -> showOverrideDialog());

        Button addExtraBtn = new Button("➕ Add Extra Class");
        addExtraBtn.setOnAction(e -> showAddExtraClassDialog());

        actions.getChildren().addAll(editTimetableBtn, overrideBtn, addExtraBtn);
        getChildren().add(actions);
    }

    public void refresh() {
        scheduleBox.getChildren().clear();
        dateLabel.setText(currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMM dd")) + " — Timetable");

        Semester activeSem = subjectDAO.getActiveSemester();
        if (activeSem == null) {
            Label noSem = new Label("No active semester set up yet.");
            noSem.setTextFill(Color.GRAY);
            scheduleBox.getChildren().add(noSem);
            return;
        }

        int dayOfWeek = currentDate.getDayOfWeek().getValue(); // 1=Mon
        List<TimetableEntry> entries = timetableDAO.getEntriesForDay(activeSem.getId(), dayOfWeek);
        List<TimetableOverride> overrides = timetableDAO.getOverridesForDate(currentDate);
        Map<Integer, TimetableOverride> overrideMap = overrides.stream()
                .collect(Collectors.toMap(TimetableOverride::getTimetableId, o -> o, (a, b) -> b));

        List<ExtraClass> extraClasses = extraClassDAO.getExtraClassesForDate(activeSem.getId(), currentDate);

        if (entries.isEmpty() && extraClasses.isEmpty()) {
            Label noClass = new Label("No classes scheduled for this day.");
            noClass.setTextFill(Color.GRAY);
            noClass.setPadding(new Insets(10));
            scheduleBox.getChildren().add(noClass);
            return;
        }

        // Merge entries and extra classes, then sort by time
        List<Object> allItems = new ArrayList<>();
        allItems.addAll(entries);
        allItems.addAll(extraClasses);

        allItems.sort((a, b) -> {
            String timeA = (a instanceof TimetableEntry) ? ((TimetableEntry) a).getStartTime() : ((ExtraClass) a).getStartTime();
            String timeB = (b instanceof TimetableEntry) ? ((TimetableEntry) b).getStartTime() : ((ExtraClass) b).getStartTime();
            return timeA.compareTo(timeB);
        });

        for (Object item : allItems) {
            if (item instanceof TimetableEntry) {
                TimetableEntry entry = (TimetableEntry) item;
                TimetableOverride ov = overrideMap.get(entry.getId());
                scheduleBox.getChildren().add(createClassRow(entry, ov));
            } else {
                scheduleBox.getChildren().add(createExtraClassRow((ExtraClass) item));
            }
        }
    }

    private String formatTime(String time24) {
        try {
            java.time.LocalTime t = java.time.LocalTime.parse(time24);
            return t.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        } catch (Exception e) {
            return time24;
        }
    }

    private HBox createClassRow(TimetableEntry entry, TimetableOverride override) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.getStyleClass().add("timetable-row");

        boolean isCancelled = override != null && "CANCEL".equals(override.getOverrideType());

        // Time
        Label timeLabel = new Label(formatTime(entry.getStartTime()) + " - " + formatTime(entry.getEndTime()));
        timeLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        timeLabel.setMinWidth(130);

        // Subject
        String subjectText = entry.getSubjectShortCode() != null ? entry.getSubjectShortCode() : entry.getSubjectName();
        if (override != null && "SUBSTITUTE".equals(override.getOverrideType()) && override.getNewSubjectId() != null) {
            Subject newSubj = subjectDAO.getSubjectById(override.getNewSubjectId());
            if (newSubj != null) subjectText = newSubj.getShortCode() != null ? newSubj.getShortCode() : newSubj.getName();
        }
        Label subjectLabel = new Label(subjectText);
        subjectLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        subjectLabel.setMinWidth(80);
        if (isCancelled) {
            subjectLabel.setTextFill(Color.GRAY);
            subjectLabel.setStyle("-fx-strikethrough: true;");
        }

        // Room
        String roomText = (override != null && override.getNewRoom() != null) ? override.getNewRoom() : entry.getRoom();
        Label roomLabel = new Label(roomText != null ? "📍 " + roomText : "");
        roomLabel.setMinWidth(80);

        // Faculty
        String facultyText = (override != null && override.getNewFaculty() != null) ? override.getNewFaculty() : entry.getFaculty();
        Label facultyLabel = new Label(facultyText != null ? "👤 " + facultyText : "");
        facultyLabel.setMinWidth(100);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(timeLabel, subjectLabel, roomLabel, facultyLabel, spacer);

        // Override badge
        if (isCancelled) {
            Label cancelBadge = new Label("CANCELLED");
            cancelBadge.setStyle("-fx-background-color: #FFCDD2; -fx-text-fill: #C62828; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 10; -fx-font-weight: bold;");
            
            Button undoBtn = new Button("Undo Cancel");
            undoBtn.setOnAction(e -> {
                timetableDAO.deleteOverride(override.getId());
                refresh();
            });
            row.getChildren().addAll(cancelBadge, undoBtn);
        } else {
            // Attendance buttons
            Attendance att = attendanceDAO.getAttendance(entry.getId(), currentDate);
            String currentStatus = att != null ? att.getStatus() : null;

            Button presentBtn = new Button("P");
            presentBtn.getStyleClass().add("present".equals(currentStatus) || "PRESENT".equals(currentStatus) ? "present-active" : "present-button");
            if ("PRESENT".equals(currentStatus)) presentBtn.getStyleClass().add("present-active");
            presentBtn.setOnAction(e -> {
                if (currentDate.isAfter(LocalDate.now())) {
                    new Alert(Alert.AlertType.WARNING, "Cannot mark attendance for future classes.", ButtonType.OK).showAndWait();
                    return;
                }
                attendanceDAO.markAttendance(entry.getId(), currentDate, "PRESENT");
                refresh();
            });

            Button absentBtn = new Button("A");
            absentBtn.getStyleClass().add("ABSENT".equals(currentStatus) ? "absent-active" : "absent-button");
            absentBtn.setOnAction(e -> {
                if (currentDate.isAfter(LocalDate.now())) {
                    new Alert(Alert.AlertType.WARNING, "Cannot mark attendance for future classes.", ButtonType.OK).showAndWait();
                    return;
                }
                attendanceDAO.markAttendance(entry.getId(), currentDate, "ABSENT");
                refresh();
            });

            row.getChildren().addAll(presentBtn, absentBtn);
        }

        return row;
    }

    private HBox createExtraClassRow(ExtraClass ec) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.getStyleClass().add("timetable-row");
        row.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 0 0 0 4;"); // Highlight extra class

        Label timeLabel = new Label(formatTime(ec.getStartTime()) + " - " + formatTime(ec.getEndTime()));
        timeLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        timeLabel.setMinWidth(130);

        String subjectText = ec.getSubjectShortCode() != null ? ec.getSubjectShortCode() : ec.getSubjectName();
        Label subjectLabel = new Label(subjectText + " (Extra)");
        subjectLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        subjectLabel.setTextFill(Color.web("#2E7D32"));
        subjectLabel.setMinWidth(80);

        Label roomLabel = new Label(ec.getRoom() != null ? "📍 " + ec.getRoom() : "");
        roomLabel.setMinWidth(80);

        Label facultyLabel = new Label(ec.getFaculty() != null ? "👤 " + ec.getFaculty() : "");
        facultyLabel.setMinWidth(100);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(timeLabel, subjectLabel, roomLabel, facultyLabel, spacer);

        String currentStatus = ec.getStatus();

        Button presentBtn = new Button("P");
        presentBtn.getStyleClass().add("PRESENT".equals(currentStatus) ? "present-active" : "present-button");
        presentBtn.setOnAction(e -> {
            if (currentDate.isAfter(LocalDate.now())) {
                new Alert(Alert.AlertType.WARNING, "Cannot mark attendance for future classes.", ButtonType.OK).showAndWait();
                return;
            }
            extraClassDAO.updateExtraClassStatus(ec.getId(), "PRESENT");
            refresh();
        });

        Button absentBtn = new Button("A");
        absentBtn.getStyleClass().add("ABSENT".equals(currentStatus) ? "absent-active" : "absent-button");
        absentBtn.setOnAction(e -> {
            if (currentDate.isAfter(LocalDate.now())) {
                new Alert(Alert.AlertType.WARNING, "Cannot mark attendance for future classes.", ButtonType.OK).showAndWait();
                return;
            }
            extraClassDAO.updateExtraClassStatus(ec.getId(), "ABSENT");
            refresh();
        });

        Button deleteBtn = new Button("✕");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> {
            extraClassDAO.deleteExtraClass(ec.getId());
            refresh();
        });

        row.getChildren().addAll(presentBtn, absentBtn, deleteBtn);
        return row;
    }

    private void showEditTimetableDialog() {
        Semester activeSem = subjectDAO.getActiveSemester();
        if (activeSem == null) {
            new Alert(Alert.AlertType.WARNING, "Please create a semester first!", ButtonType.OK).showAndWait();
            return;
        }

        List<Subject> subjects = subjectDAO.getSubjectsBySemester(activeSem.getId());
        if (subjects.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please add subjects first!", ButtonType.OK).showAndWait();
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Semester Timetable — " + activeSem.getName());
        dialog.setResizable(true);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(650);

        // ── Entries list (grouped by day) ──
        VBox entriesBox = new VBox(5);
        // ── Collapsible form area ──
        VBox formArea = new VBox(8);
        formArea.setVisible(false);
        formArea.setManaged(false);

        // Shared state for edit mode
        final int[] editingId = {-1}; // -1 = add mode, >0 = editing that entry

        Runnable rebuildList = () -> {
            entriesBox.getChildren().clear();
            List<TimetableEntry> all = timetableDAO.getAllEntries(activeSem.getId());
            Map<Integer, List<TimetableEntry>> byDay = all.stream()
                    .collect(Collectors.groupingBy(TimetableEntry::getDayOfWeek));

            for (int d = 1; d <= 7; d++) {
                List<TimetableEntry> dayEntries = byDay.getOrDefault(d, List.of());
                if (dayEntries.isEmpty()) continue;
                String dayName = dayEntries.get(0).getDayName();
                Label dayHeader = new Label("━━ " + dayName + " ━━");
                dayHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
                dayHeader.setTextFill(Color.web("#1E88E5"));
                entriesBox.getChildren().add(dayHeader);

                for (TimetableEntry entry : dayEntries) {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(4, 8, 4, 16));
                    row.getStyleClass().add("timetable-row");

                    String subj = entry.getSubjectShortCode() != null ? entry.getSubjectShortCode() : entry.getSubjectName();
                    Label lbl = new Label(formatTime(entry.getStartTime()) + " - " + formatTime(entry.getEndTime())
                            + "  │  " + subj + "  │  📍" + (entry.getRoom() != null ? entry.getRoom() : "—")
                            + "  │  👤" + (entry.getFaculty() != null ? entry.getFaculty() : "—")
                            + (entry.getNotes() != null && !entry.getNotes().isEmpty() ? "  │  📝" + entry.getNotes() : ""));
                    lbl.setWrapText(true);
                    HBox.setHgrow(lbl, Priority.ALWAYS);

                    Button editBtn = new Button("✏️");
                    editBtn.getStyleClass().add("icon-button");
                    editBtn.setOnAction(ev -> {
                        editingId[0] = entry.getId();
                        showEntryForm(formArea, subjects, activeSem.getId(), entry, editingId, entriesBox, content);
                    });

                    Button delBtn = new Button("❌");
                    delBtn.getStyleClass().add("delete-button");
                    delBtn.setOnAction(ev -> {
                        timetableDAO.deleteEntry(entry.getId());
                        // Trigger rebuild via a manual call
                        entriesBox.getChildren().clear();
                        List<TimetableEntry> refreshed = timetableDAO.getAllEntries(activeSem.getId());
                        rebuildEntriesList(entriesBox, refreshed, formArea, subjects, activeSem.getId(), editingId, content);
                    });

                    row.getChildren().addAll(lbl, editBtn, delBtn);
                    entriesBox.getChildren().add(row);
                }
            }

            if (all.isEmpty()) {
                Label empty = new Label("No classes yet. Click '+ Add Class' to get started.");
                empty.setTextFill(Color.GRAY);
                empty.setPadding(new Insets(10));
                entriesBox.getChildren().add(empty);
            }
        };

        rebuildList.run();

        ScrollPane scroll = new ScrollPane(entriesBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(250);

        // ── + Add Class button ──
        Button addClassBtn = new Button("➕ Add Class");
        addClassBtn.getStyleClass().add("accent-button");
        addClassBtn.setOnAction(e -> {
            editingId[0] = -1;
            showEntryForm(formArea, subjects, activeSem.getId(), null, editingId, entriesBox, content);
        });

        content.getChildren().addAll(scroll, addClassBtn, formArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
        refresh();
    }

    /** Helper to rebuild entries list after modification */
    private void rebuildEntriesList(VBox entriesBox, List<TimetableEntry> all, VBox formArea,
                                     List<Subject> subjects, int semId, int[] editingId, VBox content) {
        Map<Integer, List<TimetableEntry>> byDay = all.stream()
                .collect(Collectors.groupingBy(TimetableEntry::getDayOfWeek));

        for (int d = 1; d <= 7; d++) {
            List<TimetableEntry> dayEntries = byDay.getOrDefault(d, List.of());
            if (dayEntries.isEmpty()) continue;
            String dayName = dayEntries.get(0).getDayName();
            Label dayHeader = new Label("━━ " + dayName + " ━━");
            dayHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
            dayHeader.setTextFill(Color.web("#1E88E5"));
            entriesBox.getChildren().add(dayHeader);

            for (TimetableEntry entry : dayEntries) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 8, 4, 16));
                row.getStyleClass().add("timetable-row");

                String subj = entry.getSubjectShortCode() != null ? entry.getSubjectShortCode() : entry.getSubjectName();
                Label lbl = new Label(formatTime(entry.getStartTime()) + " - " + formatTime(entry.getEndTime())
                        + "  │  " + subj + "  │  📍" + (entry.getRoom() != null ? entry.getRoom() : "—")
                        + "  │  👤" + (entry.getFaculty() != null ? entry.getFaculty() : "—"));
                lbl.setWrapText(true);
                HBox.setHgrow(lbl, Priority.ALWAYS);

                Button editBtn = new Button("✏️");
                editBtn.getStyleClass().add("icon-button");
                editBtn.setOnAction(ev -> {
                    editingId[0] = entry.getId();
                    showEntryForm(formArea, subjects, semId, entry, editingId, entriesBox, content);
                });

                Button delBtn = new Button("❌");
                delBtn.getStyleClass().add("delete-button");
                delBtn.setOnAction(ev -> {
                    timetableDAO.deleteEntry(entry.getId());
                    entriesBox.getChildren().clear();
                    rebuildEntriesList(entriesBox, timetableDAO.getAllEntries(semId), formArea, subjects, semId, editingId, content);
                });

                row.getChildren().addAll(lbl, editBtn, delBtn);
                entriesBox.getChildren().add(row);
            }
        }

        if (all.isEmpty()) {
            Label empty = new Label("No classes yet. Click '+ Add Class' to get started.");
            empty.setTextFill(Color.GRAY);
            entriesBox.getChildren().add(empty);
        }
    }

    /** Show/expand the add/edit form */
    private void showEntryForm(VBox formArea, List<Subject> subjects, int semId,
                                TimetableEntry existing, int[] editingId, VBox entriesBox, VBox content) {
        formArea.getChildren().clear();
        formArea.setVisible(true);
        formArea.setManaged(true);
        formArea.setStyle("-fx-background-color: #E3F2FD; -fx-padding: 12; -fx-background-radius: 6;");

        Label formTitle = new Label(existing != null ? "✏️ Edit Class" : "➕ New Class");
        formTitle.setFont(Font.font("System", FontWeight.BOLD, 13));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        ComboBox<String> dayCombo = new ComboBox<>();
        dayCombo.getItems().addAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        dayCombo.setValue(existing != null ? existing.getDayName() : "Monday");

        TextField startField = new TextField(existing != null ? existing.getStartTime() : "09:00");
        startField.setPrefWidth(80);
        TextField endField = new TextField(existing != null ? existing.getEndTime() : "10:00");
        endField.setPrefWidth(80);

        ComboBox<Subject> subjectCombo = new ComboBox<>();
        subjectCombo.getItems().addAll(subjects);
        if (existing != null) {
            subjects.stream().filter(s -> s.getId() == existing.getSubjectId()).findFirst().ifPresent(subjectCombo::setValue);
        } else if (!subjects.isEmpty()) {
            subjectCombo.setValue(subjects.get(0));
        }

        TextField roomField = new TextField(existing != null ? (existing.getRoom() != null ? existing.getRoom() : "") : "");
        roomField.setPromptText("Room / Lab");
        roomField.setPrefWidth(100);

        TextField facultyField = new TextField(existing != null ? (existing.getFaculty() != null ? existing.getFaculty() : "") : "");
        facultyField.setPromptText("Faculty name");
        facultyField.setPrefWidth(120);

        TextField notesField = new TextField(existing != null && existing.getNotes() != null ? existing.getNotes() : "");
        notesField.setPromptText("Optional notes");
        notesField.setPrefWidth(200);

        grid.add(new Label("Day:"), 0, 0);      grid.add(dayCombo, 1, 0);
        grid.add(new Label("Start:"), 2, 0);     grid.add(startField, 3, 0);
        grid.add(new Label("End:"), 4, 0);        grid.add(endField, 5, 0);
        grid.add(new Label("Subject:"), 0, 1);   grid.add(subjectCombo, 1, 1);
        grid.add(new Label("Room:"), 2, 1);      grid.add(roomField, 3, 1);
        grid.add(new Label("Faculty:"), 4, 1);   grid.add(facultyField, 5, 1);
        grid.add(new Label("Notes:"), 0, 2);     grid.add(notesField, 1, 2, 5, 1);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        Button saveBtn = new Button(existing != null ? "💾 Update" : "💾 Save");
        saveBtn.getStyleClass().add("accent-button");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("link-button");

        cancelBtn.setOnAction(ev -> { formArea.setVisible(false); formArea.setManaged(false); });

        saveBtn.setOnAction(ev -> {
            if (subjectCombo.getValue() == null) return;
            int day = dayCombo.getSelectionModel().getSelectedIndex() + 1;

            if (existing != null) {
                existing.setDayOfWeek(day);
                existing.setStartTime(startField.getText());
                existing.setEndTime(endField.getText());
                existing.setSubjectId(subjectCombo.getValue().getId());
                existing.setRoom(roomField.getText());
                existing.setFaculty(facultyField.getText());
                existing.setNotes(notesField.getText());
                timetableDAO.updateEntry(existing);
            } else {
                TimetableEntry ne = new TimetableEntry(semId, day, startField.getText(), endField.getText(),
                        subjectCombo.getValue().getId(), roomField.getText(), facultyField.getText());
                ne.setNotes(notesField.getText());
                timetableDAO.insertEntry(ne);
            }

            formArea.setVisible(false);
            formArea.setManaged(false);
            entriesBox.getChildren().clear();
            rebuildEntriesList(entriesBox, timetableDAO.getAllEntries(semId), formArea, subjects, semId, editingId, content);
        });

        btnRow.getChildren().addAll(saveBtn, cancelBtn);
        formArea.getChildren().addAll(formTitle, grid, btnRow);
    }

    private void showOverrideDialog() {
        Semester activeSem = subjectDAO.getActiveSemester();
        if (activeSem == null) {
            new Alert(Alert.AlertType.WARNING, "Please create a semester first!", ButtonType.OK).showAndWait();
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Override This Week's Timetable");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(500);

        content.getChildren().add(new Label("Select a class to cancel, change room, or substitute."));

        // Date picker for the override
        DatePicker datePicker = new DatePicker(currentDate);
        HBox dateBox = new HBox(10, new Label("Date:"), datePicker);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().add(dateBox);

        VBox classesBox = new VBox(5);

        Runnable loadClasses = new Runnable() {
            @Override
            public void run() {
                classesBox.getChildren().clear();
                LocalDate date = datePicker.getValue();
                int dow = date.getDayOfWeek().getValue();
                List<TimetableEntry> entries = timetableDAO.getEntriesForDay(activeSem.getId(), dow);
                for (TimetableEntry entry : entries) {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(5));
                    Label lbl = new Label(formatTime(entry.getStartTime()) + "-" + formatTime(entry.getEndTime()) + " " + entry.getSubjectName());
                    Button cancelBtn = new Button("Cancel");
                    cancelBtn.setStyle("-fx-background-color: #FFCDD2;");
                    cancelBtn.setOnAction(e -> {
                        TimetableOverride ov = new TimetableOverride(entry.getId(), date, "CANCEL");
                        timetableDAO.insertOverride(ov);
                        new Alert(Alert.AlertType.INFORMATION, "Class cancelled for " + date, ButtonType.OK).showAndWait();
                        this.run();
                    });
                    Button roomBtn = new Button("Change Room");
                    roomBtn.setOnAction(e -> {
                        TextInputDialog tid = new TextInputDialog();
                        tid.setTitle("New Room");
                        tid.setHeaderText("Enter new room for " + entry.getSubjectName());
                        tid.showAndWait().ifPresent(room -> {
                            TimetableOverride ov = new TimetableOverride(entry.getId(), date, "ROOM_CHANGE");
                            ov.setNewRoom(room);
                            timetableDAO.insertOverride(ov);
                            this.run();
                        });
                    });
                    row.getChildren().addAll(lbl, cancelBtn, roomBtn);
                    classesBox.getChildren().add(row);
                }
                if (entries.isEmpty()) {
                    classesBox.getChildren().add(new Label("No classes on this day."));
                }
            }
        };

        datePicker.setOnAction(e -> loadClasses.run());
        loadClasses.run();

        content.getChildren().add(classesBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
        refresh();
    }

    private void showAddExtraClassDialog() {
        Semester activeSem = subjectDAO.getActiveSemester();
        if (activeSem == null) {
            new Alert(Alert.AlertType.WARNING, "Please create a semester first!", ButtonType.OK).showAndWait();
            return;
        }

        List<Subject> subjects = subjectDAO.getSubjectsBySemester(activeSem.getId());
        if (subjects.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please add subjects first!", ButtonType.OK).showAndWait();
            return;
        }

        Dialog<ExtraClass> dialog = new Dialog<>();
        dialog.setTitle("Add Extra Class for " + currentDate.format(DateTimeFormatter.ofPattern("MMM dd")));
        
        GridPane addGrid = new GridPane();
        addGrid.setHgap(8);
        addGrid.setVgap(5);
        addGrid.setPadding(new Insets(15));

        TextField startField = new TextField("09:00");
        startField.setPrefWidth(70);
        TextField endField = new TextField("10:00");
        endField.setPrefWidth(70);

        ComboBox<Subject> subjectCombo = new ComboBox<>();
        subjectCombo.getItems().addAll(subjects);
        if (!subjects.isEmpty()) subjectCombo.setValue(subjects.get(0));

        TextField roomField = new TextField();
        roomField.setPromptText("Room");
        roomField.setPrefWidth(80);

        TextField facultyField = new TextField();
        facultyField.setPromptText("Faculty");
        facultyField.setPrefWidth(120);

        addGrid.add(new Label("Start (24h):"), 0, 0);
        addGrid.add(startField, 1, 0);
        addGrid.add(new Label("End (24h):"), 2, 0);
        addGrid.add(endField, 3, 0);
        addGrid.add(new Label("Subject:"), 0, 1);
        addGrid.add(subjectCombo, 1, 1);
        addGrid.add(new Label("Room:"), 2, 1);
        addGrid.add(roomField, 3, 1);
        addGrid.add(new Label("Faculty:"), 0, 2);
        addGrid.add(facultyField, 1, 2);

        dialog.getDialogPane().setContent(addGrid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && subjectCombo.getValue() != null) {
                ExtraClass ec = new ExtraClass(activeSem.getId(), currentDate, startField.getText(), endField.getText(),
                        subjectCombo.getValue().getId(), roomField.getText(), facultyField.getText());
                return ec;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(ec -> {
            extraClassDAO.insertExtraClass(ec);
            refresh();
        });
    }
}
