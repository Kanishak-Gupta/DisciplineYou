module com.disciplineyou {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.disciplineyou to javafx.fxml;
    opens com.disciplineyou.ui to javafx.fxml;

    exports com.disciplineyou;
    exports com.disciplineyou.ui;
    exports com.disciplineyou.model;
    exports com.disciplineyou.dao;
    exports com.disciplineyou.service;
    exports com.disciplineyou.db;
}
