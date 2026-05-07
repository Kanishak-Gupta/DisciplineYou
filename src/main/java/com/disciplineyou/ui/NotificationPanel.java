package com.disciplineyou.ui;

import com.disciplineyou.model.Notification;
import com.disciplineyou.service.NotificationService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationPanel extends VBox {

    private final NotificationService notifService;

    public NotificationPanel(NotificationService notifService) {
        this.notifService = notifService;
        setSpacing(6);
        setPadding(new Insets(10));
        setPrefWidth(350);
        setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);");
        refresh();
    }

    public void refresh() {
        getChildren().clear();
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🔔 Notifications");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button markAllBtn = new Button("Mark all read");
        markAllBtn.getStyleClass().add("link-button");
        markAllBtn.setOnAction(e -> { notifService.markAllRead(); refresh(); });
        header.getChildren().addAll(title, sp, markAllBtn);
        getChildren().add(header);

        List<Notification> notifications = notifService.getNotifications();
        if (notifications.isEmpty()) {
            Label empty = new Label("No notifications. All clear! ✨");
            empty.setTextFill(Color.GRAY);
            empty.setPadding(new Insets(10));
            getChildren().add(empty);
            return;
        }

        for (Notification n : notifications) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 8, 6, 8));
            row.setStyle(n.isRead()
                    ? "-fx-background-color: #FAFAFA; -fx-background-radius: 4;"
                    : "-fx-background-color: #E3F2FD; -fx-background-radius: 4;");

            Label icon = new Label(n.getIcon());
            icon.setFont(Font.font(14));

            VBox info = new VBox(2);
            Label tl = new Label(n.getTitle());
            tl.setFont(Font.font("System", n.isRead() ? FontWeight.NORMAL : FontWeight.BOLD, 12));
            Label ml = new Label(n.getMessage());
            ml.setFont(Font.font(11));
            ml.setTextFill(Color.GRAY);
            ml.setWrapText(true);
            info.getChildren().addAll(tl, ml);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label time = new Label(n.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")));
            time.setFont(Font.font(10));
            time.setTextFill(Color.GRAY);

            Button dismissBtn = new Button("✕");
            dismissBtn.getStyleClass().add("delete-button");
            dismissBtn.setOnAction(e -> { notifService.dismiss(n); refresh(); });

            row.getChildren().addAll(icon, info, time, dismissBtn);
            getChildren().add(row);
        }
    }
}
