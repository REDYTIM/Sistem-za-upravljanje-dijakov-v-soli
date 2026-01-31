package com.example.frontend;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Main extends Application {

    private TextArea output;

    @Override
    public void start(Stage stage) {
        Button loadStudentsBtn = new Button("Load Students");
        output = new TextArea();
        output.setEditable(false);
        output.setPrefHeight(300);

        loadStudentsBtn.setOnAction(e -> loadStudents());

        VBox layout = new VBox(10, loadStudentsBtn, output);
        layout.setStyle("-fx-padding: 20");

        stage.setTitle("School Information System");
        stage.setScene(new Scene(layout, 500, 400));
        stage.show();
    }

    private void loadStudents() {
        try {
            URL url = new URL("http://localhost:3000/dijaki");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            String line;
            StringBuilder response = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }

            reader.close();
            output.setText(response.toString());

        } catch (Exception e) {
            output.setText("Error loading students");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}

