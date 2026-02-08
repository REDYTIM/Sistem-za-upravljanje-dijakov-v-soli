module com.example.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson; // <-- dodaj to
    opens com.example.frontend to javafx.fxml;
    exports com.example.frontend;
}