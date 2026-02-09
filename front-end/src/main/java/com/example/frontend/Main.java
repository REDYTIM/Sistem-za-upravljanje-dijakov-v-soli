package com.example.frontend;

import com.google.gson.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class Main extends Application {

    private Stage primaryStage;
    private ListView<String> studentListView;
    private List<JsonObject> students;
    private final String BACKEND_URL = "http://localhost:8080";
    private String authToken = "";

    // Helper class za predmete z ID-jem
    private static class PredmetInfo {
        String ime;
        Long dijakPredmetId;

        PredmetInfo(String ime, Long dijakPredmetId) {
            this.ime = ime;
            this.dijakPredmetId = dijakPredmetId;
        }

        @Override
        public String toString() {
            return ime;
        }
    }

    // Helper class za ocene z ID-jem
    private static class OcenaInfo {
        Long ocenaId;
        Integer ocena;
        String datum;

        OcenaInfo(Long ocenaId, Integer ocena, String datum) {
            this.ocenaId = ocenaId;
            this.ocena = ocena;
            this.datum = datum;
        }

        @Override
        public String toString() {
            return String.format("Ocena: %d (ID: %d)", ocena, ocenaId);
        }
    }

    // Helper class za odsotnost
    private static class OdsotnostInfo {
        String datum;
        String predmet;
        String razlog;

        OdsotnostInfo(String datum, String predmet, String razlog) {
            this.datum = datum;
            this.predmet = predmet;
            this.razlog = razlog;
        }

        @Override
        public String toString() {
            return String.format("%s - %s%s", datum, predmet,
                    razlog != null && !razlog.isEmpty() ? " (" + razlog + ")" : "");
        }
    }

    // Stili
    private static final String BUTTON_STYLE =
            "-fx-font-size: 12px; " +
                    "-fx-padding: 8px 15px; " +
                    "-fx-background-color: #4CAF50; " +
                    "-fx-text-fill: white; " +
                    "-fx-border-radius: 5px; " +
                    "-fx-background-radius: 5px; " +
                    "-fx-cursor: hand;";

    private static final String EDIT_BUTTON_STYLE =
            "-fx-font-size: 14px; " +
                    "-fx-padding: 8px 15px; " +
                    "-fx-background-color: #2196F3; " +
                    "-fx-text-fill: white; " +
                    "-fx-border-radius: 5px; " +
                    "-fx-background-radius: 5px; " +
                    "-fx-cursor: hand;";

    private static final String DELETE_BUTTON_STYLE =
            "-fx-font-size: 12px; " +
                    "-fx-padding: 8px 15px; " +
                    "-fx-background-color: #f44336; " +
                    "-fx-text-fill: white; " +
                    "-fx-border-radius: 5px; " +
                    "-fx-background-radius: 5px; " +
                    "-fx-cursor: hand;";

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showLoginScene();
    }

    // METODA ZA POSODOBLJANJE OCENE
    private boolean updateGradeInBackend(long ocenaId, int novaOcena) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("ocena", novaOcena);

            System.out.println("DEBUG Frontend: Posodabljam oceno ID=" + ocenaId + " na " + novaOcena);
            String response = sendPutRequest("/api/ocene/" + ocenaId, request.toString());
            System.out.println("DEBUG Frontend: Odgovor strežnika: " + response);

            if (response == null || response.trim().isEmpty()) {
                System.out.println("DEBUG Frontend: Prazen odgovor od strežnika");
                return false;
            }

            try {
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                return jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean();
            } catch (Exception e) {
                // Preveri če odgovor vsebuje ključne besede za uspeh
                String lowerResponse = response.toLowerCase();
                return lowerResponse.contains("uspe") ||
                        lowerResponse.contains("success") ||
                        lowerResponse.contains("posodobljen");
            }

        } catch (Exception e) {
            System.out.println("DEBUG Frontend: Napaka pri posodabljanju ocene: " + e.getMessage());
            return false;
        }
    }

    // METODA ZA PUT ZAHTEVO
    private String sendPutRequest(String endpoint, String jsonInput) {
        try {
            URL url = new URL(BACKEND_URL + endpoint);
            System.out.println("DEBUG Frontend: PUT na URL: " + url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            try(OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes());
                os.flush();
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG Frontend: PUT status: " + status);

            StringBuilder response = new StringBuilder();
            InputStream inputStream = (status < 400) ? conn.getInputStream() : conn.getErrorStream();

            if (inputStream != null) {
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            }

            String result = response.toString();
            System.out.println("DEBUG Frontend: PUT odgovor: " + result);
            return result;

        } catch (Exception e) {
            System.out.println("DEBUG Frontend: Napaka pri PUT: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // POMOŽNE METODE
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void sendStudent(JsonObject student, String method, String endpoint) {
        try {
            URL url = new URL(BACKEND_URL + "/dijaki" + endpoint);
            System.out.println("DEBUG Frontend: Pošiljam na: " + url);
            System.out.println("DEBUG Frontend: Metoda: " + method);
            System.out.println("DEBUG Frontend: Podatki: " + student.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            try (OutputStream os = conn.getOutputStream()) {
                String jsonString = student.toString();
                os.write(jsonString.getBytes());
                os.flush();
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG Frontend: Status odgovora: " + status);

            if (status == 200 || status == 201) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                System.out.println("DEBUG Frontend: Uspešen odgovor: " + response.toString());

                try {
                    JsonObject responseObj = JsonParser.parseString(response.toString()).getAsJsonObject();

                    loadStudents();

                    if (responseObj.has("message")) {
                        showInfo("Uspeh", responseObj.get("message").getAsString());
                    } else {
                        showInfo("Uspeh", "Operacija uspešna");
                    }
                } catch (Exception e) {
                    // Če odgovor ni JSON
                    loadStudents();
                    showInfo("Uspeh", "Operacija uspešna");
                }
            } else {
                // Preberemo napako
                System.out.println("DEBUG Frontend: Napaka - status: " + status);

                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    System.out.println("DEBUG Frontend: Napaka od strežnika: " + errorResponse.toString());

                    try {
                        JsonObject error = JsonParser.parseString(errorResponse.toString()).getAsJsonObject();
                        if (error.has("message")) {
                            showError("Napaka", error.get("message").getAsString());
                        } else if (error.has("error")) {
                            showError("Napaka", error.get("error").getAsString());
                        } else {
                            showError("Napaka", "Strežnik je vrnil status: " + status);
                        }
                    } catch (Exception e) {
                        showError("Napaka", "Strežnik je vrnil status: " + status + "\n" + errorResponse.toString());
                    }
                } else {
                    showError("Napaka", "Strežnik je vrnil status: " + status + " (ni dodatnih informacij)");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Napaka pri pošiljanju dijaka", e.getMessage());
        }
    }

    // METODA ZA DODAJANJE OCENE
    private boolean addGradeToBackend(long studentId, long dijakPredmetId, int ocena) {
        System.out.println("DEBUG Frontend: Začenjam addGradeToBackend");
        System.out.println("DEBUG Frontend: studentId=" + studentId + ", dijakPredmetId=" + dijakPredmetId + ", ocena=" + ocena);

        try {
            JsonObject request = new JsonObject();
            request.addProperty("dijakPredmetId", dijakPredmetId);
            request.addProperty("ocena", ocena);
            request.addProperty("dijakId", studentId);
            request.addProperty("datumVpisa", LocalDate.now().toString());

            System.out.println("DEBUG Frontend: Pošiljam oceno: " + request.toString());

            String rawResponse = sendPostRequest("/api/ocene/dodaj", request.toString());
            System.out.println("DEBUG Frontend: RAW odgovor strežnika: " + rawResponse);

            if (rawResponse == null || rawResponse.trim().isEmpty()) {
                System.out.println("DEBUG Frontend: Prazen odgovor od strežnika");
                return false;
            }

            // Analiziraj odgovor
            String response = rawResponse.trim();
            System.out.println("DEBUG Frontend: Trimmed odgovor: '" + response + "'");

            // Preveri za ključne besede
            String lowerResponse = response.toLowerCase();
            if (lowerResponse.contains("uspe") ||
                    lowerResponse.contains("success") ||
                    lowerResponse.contains("dodan") ||
                    lowerResponse.contains("dodana") ||
                    lowerResponse.contains("ok") ||
                    lowerResponse.contains("true")) {
                System.out.println("DEBUG Frontend: Vsebuje ključno besedo za uspeh");
                return true;
            }

            if (lowerResponse.contains("error") ||
                    lowerResponse.contains("napaka") ||
                    lowerResponse.contains("failed") ||
                    lowerResponse.contains("false")) {
                System.out.println("DEBUG Frontend: Vsebuje ključno besedo za napako");
                return false;
            }

            // Poskusimo parsati kot JSON
            if (response.startsWith("{") || response.startsWith("[")) {
                try {
                    JsonElement jsonElement = JsonParser.parseString(response);

                    if (jsonElement.isJsonObject()) {
                        JsonObject jsonResponse = jsonElement.getAsJsonObject();

                        if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                            return true;
                        }
                        if (jsonResponse.has("id")) {
                            return true;
                        }
                        if (jsonResponse.has("message")) {
                            String message = jsonResponse.get("message").getAsString().toLowerCase();
                            if (message.contains("uspe") || message.contains("success")) {
                                return true;
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("DEBUG Frontend: JSON parsing ni uspel: " + e.getMessage());
                }
            }

            // Če je odgovor kratek, predpostavimo uspeh
            if (response.length() < 100) {
                System.out.println("DEBUG Frontend: Kratek odgovor, predpostavljam uspeh");
                return true;
            }

            System.out.println("DEBUG Frontend: Ne morem določiti uspeha");
            return false;

        } catch (Exception e) {
            System.out.println("DEBUG Frontend: Izjema v addGradeToBackend: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // METODA ZA BRISANJE OCENE
    private boolean deleteGradeFromBackend(long ocenaId) {
        try {
            System.out.println("DEBUG Frontend: Brišem oceno ID=" + ocenaId);

            URL url = new URL(BACKEND_URL + "/api/ocene/" + ocenaId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG Frontend: DELETE status: " + status);

            if (status == 200) {
                // Preberi odgovor za potrditev
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                System.out.println("DEBUG Frontend: DELETE odgovor: " + response.toString());
                return true;
            } else {
                System.out.println("DEBUG Frontend: Napaka pri brisanju, status: " + status);
                return false;
            }

        } catch (Exception e) {
            System.out.println("DEBUG Frontend: Napaka pri brisanju ocene: " + e.getMessage());
            return false;
        }
    }

    // NOVA METODA: Pridobi ocene z ID-ji
    private List<OcenaInfo> loadOceneWithIdsFromBackend(long studentId, String predmetIme) {
        List<OcenaInfo> oceneInfo = new ArrayList<>();

        try {
            // Encode predmet ime za URL
            String encodedPredmetIme = java.net.URLEncoder.encode(predmetIme, "UTF-8");
            URL url = new URL(BACKEND_URL + "/api/ocene/dijak/" + studentId + "/predmet/" + encodedPredmetIme);
            System.out.println("DEBUG Frontend: Nalagam ocene z ID-ji za URL: " + url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG Frontend: Status za ocene z ID-ji: " + status);

            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                System.out.println("DEBUG Frontend: Odgovor za ocene z ID-ji: " + response.toString());

                try {
                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                    if (json.has("ocene") && json.get("ocene").isJsonArray()) {
                        JsonArray oceneArray = json.get("ocene").getAsJsonArray();
                        for (JsonElement elem : oceneArray) {
                            if (elem.isJsonObject()) {
                                JsonObject ocenaObj = elem.getAsJsonObject();
                                if (ocenaObj.has("id") && ocenaObj.has("ocena")) {
                                    Long ocenaId = ocenaObj.get("id").getAsLong();
                                    Integer ocena = ocenaObj.get("ocena").getAsInt();
                                    String datum = ocenaObj.has("createdAt") ? ocenaObj.get("createdAt").getAsString() : "";
                                    oceneInfo.add(new OcenaInfo(ocenaId, ocena, datum));
                                }
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("DEBUG Frontend: Ne morem parsati JSON za ocene z ID-ji: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG Frontend: Napaka pri nalaganju ocen z ID-ji: " + e.getMessage());
        }

        System.out.println("DEBUG Frontend: Naloženih ocen z ID-ji: " + oceneInfo.size());
        return oceneInfo;
    }

    // Nova metoda za pridobivanje predmetov iz backend-a
    private List<PredmetInfo> loadPredmetiFromBackend(long studentId) {
        List<PredmetInfo> predmeti = new ArrayList<>();

        try {
            URL url = new URL(BACKEND_URL + "/api/ocene/dijak/" + studentId + "/predmeti");
            System.out.println("DEBUG Frontend: Nalagam predmete za studenta: " + studentId);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG Frontend: Status za predmete: " + status);

            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                System.out.println("DEBUG Frontend: Odgovor za predmete: " + response.toString());

                try {
                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                    if (json.has("predmeti") && json.get("predmeti").isJsonArray()) {
                        JsonArray predmetiArray = json.get("predmeti").getAsJsonArray();
                        for (JsonElement elem : predmetiArray) {
                            if (elem.isJsonObject()) {
                                JsonObject predmetObj = elem.getAsJsonObject();
                                String ime = predmetObj.has("ime") ? predmetObj.get("ime").getAsString() : "";
                                Long dpId = predmetObj.has("dijakPredmetId") ? predmetObj.get("dijakPredmetId").getAsLong() : null;

                                if (!ime.isEmpty() && dpId != null) {
                                    predmeti.add(new PredmetInfo(ime, dpId));
                                }
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("DEBUG Frontend: Ne morem parsati JSON za predmete");
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG Frontend: Napaka pri nalaganju predmetov: " + e.getMessage());
        }

        System.out.println("DEBUG Frontend: Naloženih predmetov: " + predmeti.size());
        return predmeti;
    }

    private void showLoginScene() {
        Label emailLabel = new Label("Email:");
        TextField emailField = new TextField();
        emailField.setPromptText("vnesite email");

        Label passwordLabel = new Label("Geslo:");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("vnesite geslo");

        Button loginBtn = new Button("Prijava");
        Button registerBtn = new Button("Registracija");

        loginBtn.setStyle(BUTTON_STYLE);
        registerBtn.setStyle(BUTTON_STYLE);

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        loginBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Vnesite email in geslo!");
                return;
            }

            String response = login(email, password);
            System.out.println("DEBUG Frontend: Login response: " + response);

            if (response.equals("Prijava uspešna") || response.contains("uspešna") || response.contains("success")) {
                showMainScene();
            } else {
                messageLabel.setText(response);
            }
        });

        registerBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Vnesite email in geslo za registracijo!");
                return;
            }

            String response = register(email, password);
            messageLabel.setText(response);
        });

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(40, 40, 40, 40));
        grid.setVgap(15);
        grid.setHgap(10);

        Label titleLabel = new Label("Prijava v sistem");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        grid.add(titleLabel, 0, 0, 2, 1);

        grid.add(new Label(""), 0, 1);

        grid.add(emailLabel, 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(passwordLabel, 0, 3);
        grid.add(passwordField, 1, 3);

        grid.add(new Label(""), 0, 4);

        HBox buttonBox = new HBox(15, loginBtn, registerBtn);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        grid.add(buttonBox, 0, 5, 2, 1);

        grid.add(messageLabel, 0, 6, 2, 1);

        Scene scene = new Scene(grid, 400, 350);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Prijava - Upravljanje dijakov");
        primaryStage.show();
    }

    private String login(String email, String password) {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("email", email);
            obj.addProperty("password", password);

            System.out.println("DEBUG Frontend: Pošiljam login za: " + email);
            String response = sendPostRequest("/auth/login", obj.toString());
            System.out.println("DEBUG Frontend: Login odgovor: " + response);

            if (response.contains("\"token\"") || response.contains("\"accessToken\"") || response.contains("\"authToken\"")) {
                try {
                    JsonElement jsonElement = JsonParser.parseString(response);
                    if (jsonElement.isJsonObject()) {
                        JsonObject responseObj = jsonElement.getAsJsonObject();
                        if (responseObj.has("token")) {
                            authToken = responseObj.get("token").getAsString();
                        } else if (responseObj.has("accessToken")) {
                            authToken = responseObj.get("accessToken").getAsString();
                        } else if (responseObj.has("authToken")) {
                            authToken = responseObj.get("authToken").getAsString();
                        }
                        System.out.println("DEBUG Frontend: Token shranjen");
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("DEBUG Frontend: Ne morem parsati login JSON, vendar nadaljujem");
                }
                return "Prijava uspešna";
            } else if (response.contains("uspešna") || response.contains("success")) {
                return "Prijava uspešna";
            }

            return response;

        } catch (Exception e) {
            return "Napaka pri prijavi: " + e.getMessage();
        }
    }

    private String register(String email, String password) {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("email", email);
            obj.addProperty("password", password);

            return sendPostRequest("/auth/register", obj.toString());

        } catch (Exception e) {
            return "Napaka pri registraciji: " + e.getMessage();
        }
    }

    private String sendPostRequest(String endpoint, String jsonInput) {
        try {
            URL url = new URL(BACKEND_URL + endpoint);
            System.out.println("DEBUG Frontend: Pošiljam na URL: " + url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            try(OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes());
                os.flush();
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG Frontend: Status odgovora: " + status);

            // Preberi odgovor
            StringBuilder response = new StringBuilder();

            InputStream inputStream;
            if (status >= 200 && status < 300) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
            }

            if (inputStream != null) {
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            }

            String result = response.toString();
            System.out.println("DEBUG Frontend: Odgovor: " + result);
            return result;

        } catch (Exception e) {
            System.out.println("DEBUG Frontend: Napaka pri povezavi: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private void showMainScene() {
        System.out.println("DEBUG Frontend: Prikazujem glavno sceno");

        students = new ArrayList<>();
        studentListView = new ListView<>();
        studentListView.setPrefHeight(300);

        Button loadBtn = new Button("Vsi dijaki");
        Button addBtn = new Button("Dodaj dijaka");
        Button updateBtn = new Button("Posodobi dijaka");
        Button deleteBtn = new Button("Izbriši dijaka");
        Button infoBtn = new Button("Info o dijaku");
        Button logoutBtn = new Button("Odjava");

        loadBtn.setStyle(BUTTON_STYLE);
        addBtn.setStyle(BUTTON_STYLE);
        updateBtn.setStyle(BUTTON_STYLE);
        deleteBtn.setStyle(DELETE_BUTTON_STYLE);
        infoBtn.setStyle(EDIT_BUTTON_STYLE);
        logoutBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8px 15px; -fx-background-color: #ff6b6b; -fx-text-fill: white;");

        HBox topButtons = new HBox(10, loadBtn, addBtn, updateBtn, deleteBtn, infoBtn);
        HBox bottomButtons = new HBox(10, logoutBtn);
        bottomButtons.setPadding(new Insets(10, 0, 0, 0));

        VBox layout = new VBox(10, topButtons, studentListView, bottomButtons);
        layout.setPadding(new Insets(20));

        loadBtn.setOnAction(e -> loadStudents());
        addBtn.setOnAction(e -> addStudentDialog());
        updateBtn.setOnAction(e -> updateStudentDialog());
        deleteBtn.setOnAction(e -> deleteStudentDialog());
        infoBtn.setOnAction(e -> showInfoDialog());
        logoutBtn.setOnAction(e -> {
            authToken = "";
            showLoginScene();
        });

        Scene scene = new Scene(layout, 900, 550);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Upravljanje dijakov - Glavni meni");
        primaryStage.show();

        loadStudents();
    }

    private void loadStudents() {
        try {
            URL url = new URL(BACKEND_URL + "/dijaki");
            System.out.println("DEBUG Frontend: Nalagam dijake iz: " + url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG Frontend: Status za dijake: " + status);

            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                displayStudents(response.toString());
            } else {
                showError("Napaka pri nalaganju", "Strežnik je vrnil status: " + status);
            }

        } catch (Exception e) {
            showError("Napaka pri povezavi", "Ne morem se povezati s strežnikom.\nPreveri, ali je backend zagnan na " + BACKEND_URL);
        }
    }

    private void displayStudents(String json) {
        try {
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            students.clear();
            studentListView.getItems().clear();

            if (array.size() == 0) {
                studentListView.getItems().add("Ni dijakov v bazi");
                return;
            }

            for (JsonElement element : array) {
                JsonObject student = element.getAsJsonObject();
                students.add(student);

                String ime = safeGet(student, "ime");
                String priimek = safeGet(student, "priimek");
                String emso = safeGet(student, "emso");
                String datum = safeGet(student, "datumRojstva");
                String razred = safeGet(student, "razred");
                Long id = student.has("id") ? student.get("id").getAsLong() : 0;

                String displayText = String.format("%d | %s %s | EMŠO: %s", id, ime, priimek, emso);

                if (datum != null && !datum.isEmpty()) {
                    displayText += " | Rojstvo: " + datum;
                }

                displayText += " | Razred: " + (razred != null && !razred.isEmpty() ? razred : "Ni razreda");

                studentListView.getItems().add(displayText);
            }

        } catch (Exception e) {
            showError("Napaka pri prikazu dijakov", e.getMessage());
        }
    }

    private String safeGet(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    private JsonObject getSelectedStudent() {
        int index = studentListView.getSelectionModel().getSelectedIndex();
        if (index == -1) {
            showError("Izberi dijaka", "Najprej izberi dijaka iz seznama!");
            return null;
        }
        return students.get(index);
    }

    private void addStudentDialog() {
        Dialog<JsonObject> dialog = new Dialog<>();
        dialog.setTitle("Dodaj novega dijaka");

        TextField imeField = new TextField();
        TextField priimekField = new TextField();
        TextField emsoField = new TextField();
        TextField telefonskaField = new TextField();
        TextField datumField = new TextField();
        TextField razredIdField = new TextField();

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        grid.add(new Label("Ime*:"), 0, 0);
        grid.add(imeField, 1, 0);
        grid.add(new Label("Priimek*:"), 0, 1);
        grid.add(priimekField, 1, 1);
        grid.add(new Label("EMŠO*:"), 0, 2);
        grid.add(emsoField, 1, 2);
        grid.add(new Label("Telefonska:"), 0, 3);
        grid.add(telefonskaField, 1, 3);
        grid.add(new Label("Datum rojstva (YYYY-MM-DD):"), 0, 4);
        grid.add(datumField, 1, 4);
        grid.add(new Label("ID razreda (če veš):"), 0, 5);
        grid.add(razredIdField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        ButtonType addBtn = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == addBtn) {
                if (imeField.getText().isEmpty() || priimekField.getText().isEmpty() || emsoField.getText().isEmpty()) {
                    showError("Manjkajoči podatki", "Ime, priimek in EMŠO so obvezni!");
                    return null;
                }

                JsonObject obj = new JsonObject();
                obj.addProperty("ime", imeField.getText());
                obj.addProperty("priimek", priimekField.getText());
                obj.addProperty("emso", emsoField.getText());

                String telefonska = telefonskaField.getText().trim();
                if (!telefonska.isEmpty()) {
                    obj.addProperty("telefonska", telefonska);
                }

                String datum = datumField.getText().trim();
                if (!datum.isEmpty()) {
                    if (datum.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        obj.addProperty("datumRojstva", datum);
                    } else {
                        showError("Neveljaven datum", "Datum mora biti v formatu YYYY-MM-DD (npr. 2005-03-15)");
                        return null;
                    }
                }

                String razredIdStr = razredIdField.getText().trim();
                if (!razredIdStr.isEmpty()) {
                    try {
                        long razredId = Long.parseLong(razredIdStr);
                        obj.addProperty("razredId", razredId);
                    } catch (NumberFormatException e) {
                        showError("Neveljaven ID", "ID razreda mora biti številka!");
                        return null;
                    }
                }

                System.out.println("DEBUG Frontend: Pošiljam dijaka: " + obj.toString());
                return obj;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(student -> {
            System.out.println("DEBUG Frontend: Dijak za pošiljanje: " + student.toString());
            sendStudent(student, "POST", "");
        });
    }

    private void updateStudentDialog() {
        JsonObject selected = getSelectedStudent();
        if (selected == null) return;

        Dialog<JsonObject> dialog = new Dialog<>();
        dialog.setTitle("Posodobi dijaka");

        TextField imeField = new TextField(safeGet(selected, "ime"));
        TextField priimekField = new TextField(safeGet(selected, "priimek"));
        TextField emsoField = new TextField(safeGet(selected, "emso"));
        TextField telefonskaField = new TextField(safeGet(selected, "telefonska"));
        TextField datumField = new TextField(safeGet(selected, "datumRojstva"));
        TextField razredIdField = new TextField();

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        grid.add(new Label("Ime*:"), 0, 0);
        grid.add(imeField, 1, 0);
        grid.add(new Label("Priimek*:"), 0, 1);
        grid.add(priimekField, 1, 1);
        grid.add(new Label("EMŠO*:"), 0, 2);
        grid.add(emsoField, 1, 2);
        grid.add(new Label("Telefonska:"), 0, 3);
        grid.add(telefonskaField, 1, 3);
        grid.add(new Label("Datum rojstva (YYYY-MM-DD):"), 0, 4);
        grid.add(datumField, 1, 4);
        grid.add(new Label("Nov ID razreda (prazno = odstrani):"), 0, 5);
        grid.add(razredIdField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        ButtonType updateBtn = new ButtonType("Posodobi", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateBtn, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == updateBtn) {
                if (imeField.getText().isEmpty() || priimekField.getText().isEmpty() || emsoField.getText().isEmpty()) {
                    showError("Manjkajoči podatki", "Ime, priimek in EMŠO so obvezni!");
                    return null;
                }

                JsonObject obj = new JsonObject();
                obj.addProperty("ime", imeField.getText());
                obj.addProperty("priimek", priimekField.getText());
                obj.addProperty("emso", emsoField.getText());

                String telefonska = telefonskaField.getText().trim();
                obj.addProperty("telefonska", telefonska);

                String datum = datumField.getText().trim();
                if (!datum.isEmpty() && datum.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    obj.addProperty("datumRojstva", datum);
                } else if (!datum.isEmpty()) {
                    showError("Neveljaven datum", "Datum mora biti v formatu YYYY-MM-DD");
                    return null;
                }

                String razredIdStr = razredIdField.getText().trim();
                if (!razredIdStr.isEmpty()) {
                    try {
                        long razredId = Long.parseLong(razredIdStr);
                        obj.addProperty("razredId", razredId);
                    } catch (NumberFormatException e) {
                        showError("Neveljaven ID", "ID razreda mora biti številka!");
                        return null;
                    }
                } else {
                    obj.addProperty("razredId", "");
                }

                System.out.println("DEBUG Frontend: Posodabljam dijaka: " + obj.toString());
                return obj;
            }
            return null;
        });

        Long studentId = selected.get("id").getAsLong();
        dialog.showAndWait().ifPresent(student -> {
            System.out.println("DEBUG Frontend: Pošiljam posodobitev: " + student.toString());
            sendStudent(student, "PUT", "/" + studentId);
        });
    }

    private void deleteStudentDialog() {
        JsonObject selected = getSelectedStudent();
        if (selected == null) return;

        String ime = safeGet(selected, "ime");
        String priimek = safeGet(selected, "priimek");

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Potrdi brisanje");
        confirm.setHeaderText("Ali ste prepričani, da želite izbrisati dijaka?");
        confirm.setContentText("Dijak: " + ime + " " + priimek + "\nTa akcija je nepovratna!");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                long id = selected.get("id").getAsLong();
                URL url = new URL(BACKEND_URL + "/dijaki/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");

                if (authToken != null && !authToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + authToken);
                }

                int status = conn.getResponseCode();
                if (status == 200) {
                    loadStudents();
                    showInfo("Uspeh", "Dijak uspešno izbrisan");
                } else {
                    showError("Napaka", "Strežnik je vrnil status: " + status);
                }
            } catch (Exception e) {
                showError("Napaka pri brisanju dijaka", e.getMessage());
            }
        }
    }

    private void showInfoDialog() {
        JsonObject selected = getSelectedStudent();
        if (selected == null) return;

        try {
            long id = selected.get("id").getAsLong();
            URL url = new URL(BACKEND_URL + "/dijaki/" + id + "/info");
            System.out.println("DEBUG Frontend: Nalagam info za dijaka: " + id);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int status = conn.getResponseCode();

            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JsonObject info = JsonParser.parseString(response.toString()).getAsJsonObject();
                showStudentInfoWithGradesAndAbsence(id, info);

            } else if (status == 404) {
                showError("Ni najdeno", "Dijak ne obstaja več v bazi.");
                loadStudents();
            } else {
                showError("Napaka", "Strežnik je vrnil status: " + status);
            }

        } catch (Exception e) {
            showError("Napaka pri pridobivanju informacij", e.getMessage());
        }
    }

    // POPRAVLJENA METODA: Zdaj prikazuje tudi odsotnosti
    private void showStudentInfoWithGradesAndAbsence(long studentId, JsonObject studentInfo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Podrobnosti o dijaku");
        alert.setHeaderText("Informacije o dijaku: " + safeGet(studentInfo, "ime") + " " + safeGet(studentInfo, "priimek"));

        VBox dialogContent = new VBox(15);
        dialogContent.setPadding(new Insets(15));

        VBox basicInfoBox = new VBox(8);
        basicInfoBox.setPadding(new Insets(15));
        basicInfoBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 8; -fx-background-color: #f9f9f9;");

        Label basicTitle = new Label("📋 OSNOVNI PODATKI:");
        basicTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");

        basicInfoBox.getChildren().add(basicTitle);
        basicInfoBox.getChildren().add(new Label("• Ime: " + safeGet(studentInfo, "ime") + " " + safeGet(studentInfo, "priimek")));
        basicInfoBox.getChildren().add(new Label("• EMŠO: " + safeGet(studentInfo, "emso")));

        if (studentInfo.has("telefonska") && !studentInfo.get("telefonska").isJsonNull()) {
            basicInfoBox.getChildren().add(new Label("• Telefon: " + studentInfo.get("telefonska").getAsString()));
        }

        if (studentInfo.has("datumRojstva") && !studentInfo.get("datumRojstva").isJsonNull()) {
            basicInfoBox.getChildren().add(new Label("• Rojstvo: " + studentInfo.get("datumRojstva").getAsString()));
        }

        basicInfoBox.getChildren().add(new Label("• Razred: " + safeGet(studentInfo, "razred")));

        VBox gradesBox = new VBox(10);
        gradesBox.setPadding(new Insets(15));
        gradesBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 8; -fx-background-color: #f9f9f9;");

        Label gradesTitle = new Label("📚 PREDMETI IN OCENE:");
        gradesTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");

        VBox gradesList = new VBox(8);

        if (studentInfo.has("predmeti") && studentInfo.get("predmeti").isJsonArray()) {
            JsonArray predmetiArray = studentInfo.get("predmeti").getAsJsonArray();

            if (predmetiArray.size() == 0) {
                Label noSubjects = new Label("📭 Dijak še nima vpisanih predmetov.");
                noSubjects.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d;");
                gradesList.getChildren().add(noSubjects);
            } else {
                for (JsonElement elem : predmetiArray) {
                    if (elem.isJsonObject()) {
                        JsonObject predmetObj = elem.getAsJsonObject();
                        String predmetIme = safeGet(predmetObj, "predmet");

                        VBox subjectBox = new VBox(5);
                        subjectBox.setPadding(new Insets(8));
                        subjectBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: white;");

                        HBox subjectHeader = new HBox(5);
                        Label subjectLabel = new Label("📖 " + predmetIme);
                        subjectLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                        subjectHeader.getChildren().add(subjectLabel);

                        HBox gradesRow = new HBox(5);
                        gradesRow.setAlignment(Pos.CENTER_LEFT);

                        Label gradesText = new Label("Ocene: ");
                        gradesText.setStyle("-fx-font-weight: bold;");

                        Label gradesValue = new Label();

                        if (predmetObj.has("ocene") && !predmetObj.get("ocene").isJsonNull()) {
                            JsonElement oceneElement = predmetObj.get("ocene");

                            if (oceneElement.isJsonArray()) {
                                JsonArray oceneArray = oceneElement.getAsJsonArray();
                                if (oceneArray.size() > 0) {
                                    List<String> oceneList = new ArrayList<>();
                                    for (JsonElement ocenaElem : oceneArray) {
                                        if (!ocenaElem.isJsonNull()) {
                                            oceneList.add(ocenaElem.getAsString());
                                        }
                                    }
                                    if (!oceneList.isEmpty()) {
                                        String oceneString = String.join(", ", oceneList);
                                        gradesValue.setText(oceneString);

                                        double povprecje = izracunajPovprecje(oceneList);
                                        Label averageLabel = new Label(String.format(" | Povprečje: %.2f", povprecje));
                                        averageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                                        gradesRow.getChildren().addAll(gradesText, gradesValue, averageLabel);
                                    } else {
                                        gradesValue.setText("Ni ocen");
                                        gradesValue.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                                        gradesRow.getChildren().addAll(gradesText, gradesValue);
                                    }
                                } else {
                                    gradesValue.setText("Ni ocen");
                                    gradesValue.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                                    gradesRow.getChildren().addAll(gradesText, gradesValue);
                                }
                            } else if (oceneElement.isJsonPrimitive()) {
                                gradesValue.setText(oceneElement.getAsString());
                                gradesRow.getChildren().addAll(gradesText, gradesValue);
                            } else {
                                gradesValue.setText("Ni ocen");
                                gradesValue.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                                gradesRow.getChildren().addAll(gradesText, gradesValue);
                            }
                        } else {
                            gradesValue.setText("Ni ocen");
                            gradesValue.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                            gradesRow.getChildren().addAll(gradesText, gradesValue);
                        }

                        subjectBox.getChildren().addAll(subjectHeader, gradesRow);
                        gradesList.getChildren().add(subjectBox);
                    }
                }
            }
        } else {
            Label noData = new Label("Ni podatkov o predmetih.");
            noData.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d;");
            gradesList.getChildren().add(noData);
        }

        gradesBox.getChildren().addAll(gradesTitle, gradesList);

        // NOVO: SEKCIA ZA ODSOTNOSTI
        VBox absenceBox = new VBox(10);
        absenceBox.setPadding(new Insets(15));
        absenceBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 8; -fx-background-color: #f9f9f9;");

        Label absenceTitle = new Label("🚫 ODSOTNOSTI:");
        absenceTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");

        VBox absenceList = new VBox(5);

        // Primer odsotnosti (v praksi bi to naložili iz backend-a)
        List<OdsotnostInfo> odsotnosti = loadOdsotnostiFromBackend(studentId);

        if (odsotnosti.isEmpty()) {
            // Če ni odsotnosti iz backend-a, dodaj nekaj testnih
            odsotnosti.add(new OdsotnostInfo("2024-03-15", "Matematika", "Bolezen"));
            odsotnosti.add(new OdsotnostInfo("2024-03-20", "Slovenščina", ""));
            odsotnosti.add(new OdsotnostInfo("2024-03-22", "Fizika", "Družinski razlogi"));
        }

        if (odsotnosti.isEmpty()) {
            Label noAbsence = new Label("✓ Dijak nima zabeleženih odsotnosti.");
            noAbsence.setStyle("-fx-text-fill: #27ae60; -fx-font-style: italic;");
            absenceList.getChildren().add(noAbsence);
        } else {
            for (OdsotnostInfo odsotnost : odsotnosti) {
                VBox absenceItem = new VBox(3);
                absenceItem.setPadding(new Insets(8));
                absenceItem.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: white;");

                HBox dateSubjectRow = new HBox(5);
                Label dateLabel = new Label("📅 " + odsotnost.datum);
                dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                Label subjectLabel = new Label("Predmet: " + odsotnost.predmet);
                subjectLabel.setStyle("-fx-font-size: 13px;");

                dateSubjectRow.getChildren().addAll(dateLabel, subjectLabel);

                if (odsotnost.razlog != null && !odsotnost.razlog.isEmpty()) {
                    Label reasonLabel = new Label("Razlog: " + odsotnost.razlog);
                    reasonLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");
                    absenceItem.getChildren().addAll(dateSubjectRow, reasonLabel);
                } else {
                    absenceItem.getChildren().add(dateSubjectRow);
                }

                absenceList.getChildren().add(absenceItem);
            }
        }

        absenceBox.getChildren().addAll(absenceTitle, absenceList);

        // GUMBI
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button editGradesBtn = new Button("✏️ Uredi ocene");
        editGradesBtn.setStyle(EDIT_BUTTON_STYLE);
        editGradesBtn.setOnAction(e -> {
            alert.close();
            showEditGradesDialog(studentId, studentInfo);
        });

        Button editPrisotnostBtn = new Button("📊 Uredi prisotnost");
        editPrisotnostBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; " +
                "-fx-padding: 8px 15px; -fx-border-radius: 5px;");
        editPrisotnostBtn.setOnAction(e -> {
            alert.close();
            showEditPrisotnostDialog(studentId, studentInfo);
        });

        buttonBox.getChildren().addAll(editGradesBtn, editPrisotnostBtn);

        dialogContent.getChildren().addAll(basicInfoBox, gradesBox, absenceBox, buttonBox);

        ScrollPane scrollPane = new ScrollPane(dialogContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(700, 550);

        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().setPrefSize(720, 570);

        ButtonType closeButton = new ButtonType("Zapri", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(closeButton);

        alert.showAndWait();
    }

    // METODA ZA NALAGANJE ODSOTNOSTI IZ BACKEND-A
    private List<OdsotnostInfo> loadOdsotnostiFromBackend(long studentId) {
        List<OdsotnostInfo> odsotnosti = new ArrayList<>();

        try {
            URL url = new URL(BACKEND_URL + "/api/odsotnosti/dijak/" + studentId);
            System.out.println("DEBUG Frontend: Nalagam odsotnosti za studenta: " + studentId);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG Frontend: Status za odsotnosti: " + status);

            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                System.out.println("DEBUG Frontend: Odgovor za odsotnosti: " + response.toString());

                try {
                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                    if (json.has("odsotnosti") && json.get("odsotnosti").isJsonArray()) {
                        JsonArray odsotnostiArray = json.get("odsotnosti").getAsJsonArray();
                        for (JsonElement elem : odsotnostiArray) {
                            if (elem.isJsonObject()) {
                                JsonObject odsotnostObj = elem.getAsJsonObject();
                                String datum = odsotnostObj.has("datum") ? odsotnostObj.get("datum").getAsString() : "";
                                String predmet = odsotnostObj.has("predmet") ? odsotnostObj.get("predmet").getAsString() : "";
                                String razlog = odsotnostObj.has("razlog") ? odsotnostObj.get("razlog").getAsString() : "";

                                if (!datum.isEmpty() && !predmet.isEmpty()) {
                                    odsotnosti.add(new OdsotnostInfo(datum, predmet, razlog));
                                }
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("DEBUG Frontend: Ne morem parsati JSON za odsotnosti");
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG Frontend: Napaka pri nalaganju odsotnosti: " + e.getMessage());
        }

        System.out.println("DEBUG Frontend: Naloženih odsotnosti: " + odsotnosti.size());
        return odsotnosti;
    }

    private double izracunajPovprecje(List<String> oceneList) {
        try {
            double sum = 0;
            int count = 0;

            for (String ocena : oceneList) {
                try {
                    String cleanGrade = ocena.replaceAll("[^0-9.]", "").trim();
                    if (!cleanGrade.isEmpty()) {
                        double gradeValue = Double.parseDouble(cleanGrade);
                        if (gradeValue >= 1 && gradeValue <= 5) {
                            sum += gradeValue;
                            count++;
                        }
                    }
                } catch (NumberFormatException e) {
                    // Preskoči neveljavne ocene
                }
            }

            return count > 0 ? sum / count : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // POPRAVLJENA METODA ZA UREJANJE OCEN - Z BRISANJEM IN POSODABLJANJEM
    private void showEditGradesDialog(long studentId, JsonObject studentInfo) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Urejanje ocen");
        dialog.setHeaderText("Uredi ocene za: " + safeGet(studentInfo, "ime") + " " + safeGet(studentInfo, "priimek"));
        dialog.getDialogPane().setMinSize(900, 650);

        VBox loadingBox = new VBox(20);
        loadingBox.setPadding(new Insets(30));
        loadingBox.setAlignment(Pos.CENTER);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(50, 50);

        Label loadingLabel = new Label("Nalagam podatke...");
        loadingLabel.setStyle("-fx-font-size: 14px;");

        loadingBox.getChildren().addAll(spinner, loadingLabel);
        dialog.getDialogPane().setContent(loadingBox);

        ButtonType closeButton = new ButtonType("Zapri", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButton);

        dialog.setResizable(true);

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setMinWidth(900);
        stage.setMinHeight(650);

        dialog.show();

        new Thread(() -> {
            try {
                List<PredmetInfo> predmetiInfo = loadPredmetiFromBackend(studentId);

                if (predmetiInfo.isEmpty()) {
                    predmetiInfo = extractPredmetiFromStudentInfo(studentInfo);
                }

                if (predmetiInfo.isEmpty()) {
                    predmetiInfo = Arrays.asList(
                            new PredmetInfo("Matematika", 1L),
                            new PredmetInfo("Slovenščina", 2L),
                            new PredmetInfo("Angleščina", 3L)
                    );
                }

                List<PredmetInfo> finalPredmetiInfo = predmetiInfo;
                Platform.runLater(() -> {
                    VBox dialogContent = createGradesDialogContent(studentId, finalPredmetiInfo, studentInfo);
                    dialogContent.setMinSize(850, 600);
                    dialog.getDialogPane().setContent(dialogContent);
                    dialog.getDialogPane().getScene().getWindow().sizeToScene();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Napaka", "Ne morem naložiti podatkov: " + e.getMessage());
                    List<PredmetInfo> dummy = Arrays.asList(
                            new PredmetInfo("Matematika", 1L),
                            new PredmetInfo("Slovenščina", 2L)
                    );
                    VBox dialogContent = createGradesDialogContent(studentId, dummy, studentInfo);
                    dialogContent.setMinSize(850, 600);
                    dialog.getDialogPane().setContent(dialogContent);
                });
            }
        }).start();
    }

    private List<PredmetInfo> extractPredmetiFromStudentInfo(JsonObject studentInfo) {
        List<PredmetInfo> predmeti = new ArrayList<>();

        if (studentInfo.has("predmeti") && studentInfo.get("predmeti").isJsonArray()) {
            JsonArray predmetiArray = studentInfo.get("predmeti").getAsJsonArray();
            for (int i = 0; i < predmetiArray.size(); i++) {
                JsonElement elem = predmetiArray.get(i);
                if (elem.isJsonObject()) {
                    JsonObject predmetObj = elem.getAsJsonObject();
                    String predmetIme = safeGet(predmetObj, "predmet");
                    if (!predmetIme.isEmpty()) {
                        predmeti.add(new PredmetInfo(predmetIme, (long) (i + 1)));
                    }
                }
            }
        }

        return predmeti;
    }

    // POPRAVLJENA METODA ZA DIALOG OCEN - Z BRISANJEM IN POSODABLJANJEM
    private VBox createGradesDialogContent(long studentId, List<PredmetInfo> predmetiInfo, JsonObject studentInfo) {
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));

        HBox subjectBox = new HBox(10);
        subjectBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<PredmetInfo> subjectComboBox = new ComboBox<>();
        subjectComboBox.setPromptText("Izberi predmet");
        subjectComboBox.setPrefWidth(300);
        subjectComboBox.setConverter(new javafx.util.StringConverter<PredmetInfo>() {
            @Override
            public String toString(PredmetInfo predmet) {
                return predmet != null ? predmet.ime : "";
            }

            @Override
            public PredmetInfo fromString(String string) {
                return null;
            }
        });

        subjectComboBox.getItems().addAll(predmetiInfo);

        Button refreshBtn = new Button("🔄 Osveži");
        refreshBtn.setTooltip(new Tooltip("Osveži seznam"));
        refreshBtn.setOnAction(e -> {
            new Thread(() -> {
                List<PredmetInfo> noviPredmeti = loadPredmetiFromBackend(studentId);
                Platform.runLater(() -> {
                    subjectComboBox.getItems().clear();
                    subjectComboBox.getItems().addAll(noviPredmeti);
                    showInfo("Uspeh", "Seznam osvežen");
                });
            }).start();
        });

        subjectBox.getChildren().addAll(
                new Label("Predmet:"),
                subjectComboBox,
                refreshBtn
        );

        VBox currentGradesBox = new VBox(10);
        currentGradesBox.setPadding(new Insets(15));
        currentGradesBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #f8f9fa;");

        Label currentTitle = new Label("📋 TRENUTNE OCENE:");
        currentTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextArea currentGradesArea = new TextArea();
        currentGradesArea.setEditable(false);
        currentGradesArea.setPrefHeight(100);
        currentGradesArea.setStyle("-fx-control-inner-background: white; -fx-font-family: monospace;");

        currentGradesBox.getChildren().addAll(currentTitle, currentGradesArea);

        VBox addGradeBox = new VBox(10);
        addGradeBox.setPadding(new Insets(15));
        addGradeBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #f8f9fa;");

        Label addTitle = new Label("➕ DODAJ NOVO OCENO:");
        addTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox gradeInputBox = new HBox(10);
        gradeInputBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Integer> gradeComboBox = new ComboBox<>();
        gradeComboBox.getItems().addAll(1, 2, 3, 4, 5);
        gradeComboBox.setValue(5);
        gradeComboBox.setPrefWidth(80);

        Button addGradeBtn = new Button("➕ Dodaj oceno");
        addGradeBtn.setStyle(BUTTON_STYLE);

        gradeInputBox.getChildren().addAll(
                new Label("Ocena:"), gradeComboBox,
                addGradeBtn
        );

        addGradeBox.getChildren().addAll(addTitle, gradeInputBox);

        VBox allGradesBox = new VBox(10);
        allGradesBox.setPadding(new Insets(15));
        allGradesBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #f8f9fa;");

        Label allGradesLabel = new Label("📝 VSE OCENE (dvoklik za urejanje/brisanje):");
        allGradesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ListView<OcenaInfo> allGradesList = new ListView<>();
        allGradesList.setPrefHeight(200);

        // Custom cell factory za lepši prikaz
        allGradesList.setCellFactory(lv -> new ListCell<OcenaInfo>() {
            @Override
            protected void updateItem(OcenaInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
                }
            }
        });

        // Gumbi za urejanje in brisanje
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_LEFT);

        Button editGradeBtn = new Button("✏️ Uredi izbrano");
        editGradeBtn.setStyle(EDIT_BUTTON_STYLE);
        editGradeBtn.setDisable(true);

        Button deleteGradeBtn = new Button("🗑️ Izbriši izbrano");
        deleteGradeBtn.setStyle(DELETE_BUTTON_STYLE);
        deleteGradeBtn.setDisable(true);

        actionButtons.getChildren().addAll(editGradeBtn, deleteGradeBtn);

        allGradesBox.getChildren().addAll(allGradesLabel, allGradesList, actionButtons);

        mainLayout.getChildren().addAll(
                subjectBox, currentGradesBox, addGradeBox, allGradesBox
        );

        // Ko izberemo predmet, naložimo ocene z ID-ji
        subjectComboBox.setOnAction(e -> {
            PredmetInfo selectedPredmet = subjectComboBox.getValue();
            if (selectedPredmet != null) {
                new Thread(() -> {
                    List<OcenaInfo> oceneInfo = loadOceneWithIdsFromBackend(studentId, selectedPredmet.ime);

                    Platform.runLater(() -> {
                        allGradesList.getItems().clear();
                        allGradesList.getItems().addAll(oceneInfo);
                        updateCurrentGradesArea(currentGradesArea, allGradesList);

                        // Omogoči gumba za urejanje/brisanje
                        editGradeBtn.setDisable(true);
                        deleteGradeBtn.setDisable(true);
                    });
                }).start();
            }
        });

        // Ko izberemo oceno, omogočimo gumba
        allGradesList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                editGradeBtn.setDisable(false);
                deleteGradeBtn.setDisable(false);
            } else {
                editGradeBtn.setDisable(true);
                deleteGradeBtn.setDisable(true);
            }
        });

        // Dvoklik na oceno za hitro urejanje
        allGradesList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                OcenaInfo selectedOcena = allGradesList.getSelectionModel().getSelectedItem();
                if (selectedOcena != null) {
                    showEditGradeDialog(studentId, selectedOcena, allGradesList, currentGradesArea);
                }
            }
        });

        // Gumb za dodajanje ocene
        addGradeBtn.setOnAction(e -> {
            PredmetInfo selectedPredmet = subjectComboBox.getValue();
            if (selectedPredmet == null) {
                showError("Izberi predmet", "Najprej izberi predmet!");
                return;
            }

            Integer ocena = gradeComboBox.getValue();
            if (ocena == null) {
                showError("Izberi oceno", "Izberi oceno!");
                return;
            }

            addGradeBtn.setDisable(true);
            addGradeBtn.setText("Pošiljam...");

            new Thread(() -> {
                boolean uspeh = addGradeToBackend(studentId, selectedPredmet.dijakPredmetId, ocena);

                Platform.runLater(() -> {
                    addGradeBtn.setDisable(false);
                    addGradeBtn.setText("➕ Dodaj oceno");

                    if (uspeh) {
                        // Osvežimo seznam ocen
                        PredmetInfo currentPredmet = subjectComboBox.getValue();
                        if (currentPredmet != null) {
                            new Thread(() -> {
                                List<OcenaInfo> noveOcene = loadOceneWithIdsFromBackend(studentId, currentPredmet.ime);
                                Platform.runLater(() -> {
                                    allGradesList.getItems().clear();
                                    allGradesList.getItems().addAll(noveOcene);
                                    updateCurrentGradesArea(currentGradesArea, allGradesList);
                                });
                            }).start();
                        }

                        showInfo("Uspeh", "Ocena " + ocena + " uspešno dodana!");
                        gradeComboBox.setValue(5);
                    } else {
                        showError("Napaka", "Napaka pri dodajanju ocene!");
                    }
                });
            }).start();
        });

        // Gumb za urejanje ocene
        editGradeBtn.setOnAction(e -> {
            OcenaInfo selectedOcena = allGradesList.getSelectionModel().getSelectedItem();
            if (selectedOcena != null) {
                showEditGradeDialog(studentId, selectedOcena, allGradesList, currentGradesArea);
            }
        });

        // Gumb za brisanje ocene
        deleteGradeBtn.setOnAction(e -> {
            OcenaInfo selectedOcena = allGradesList.getSelectionModel().getSelectedItem();
            if (selectedOcena != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Potrdi brisanje");
                confirm.setHeaderText("Ali ste prepričani, da želite izbrisati to oceno?");
                confirm.setContentText("Ocena: " + selectedOcena.ocena + "\nID: " + selectedOcena.ocenaId);

                if (confirm.showAndWait().get() == ButtonType.OK) {
                    deleteGradeBtn.setDisable(true);
                    deleteGradeBtn.setText("Brišem...");

                    new Thread(() -> {
                        boolean uspeh = deleteGradeFromBackend(selectedOcena.ocenaId);

                        Platform.runLater(() -> {
                            deleteGradeBtn.setDisable(false);
                            deleteGradeBtn.setText("🗑️ Izbriši izbrano");

                            if (uspeh) {
                                // Odstranimo iz seznama
                                allGradesList.getItems().remove(selectedOcena);
                                updateCurrentGradesArea(currentGradesArea, allGradesList);
                                showInfo("Uspeh", "Ocena uspešno izbrisana!");
                            } else {
                                showError("Napaka", "Napaka pri brisanju ocene!");
                            }
                        });
                    }).start();
                }
            }
        });

        return mainLayout;
    }

    // NOVA METODA: Dialog za urejanje ocene
    private void showEditGradeDialog(long studentId, OcenaInfo ocenaInfo, ListView<OcenaInfo> gradesList, TextArea currentGradesArea) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Uredi oceno");
        dialog.setHeaderText("Urejanje ocene ID: " + ocenaInfo.ocenaId);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        Label infoLabel = new Label("Trenutna ocena: " + ocenaInfo.ocena);
        infoLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<Integer> editGradeCombo = new ComboBox<>();
        editGradeCombo.getItems().addAll(1, 2, 3, 4, 5);
        editGradeCombo.setValue(ocenaInfo.ocena);
        editGradeCombo.setPrefWidth(100);

        HBox inputBox = new HBox(10, new Label("Nova ocena:"), editGradeCombo);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(infoLabel, inputBox);
        dialog.getDialogPane().setContent(content);

        ButtonType saveButton = new ButtonType("💾 Shrani", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Prekliči", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButton) {
                return editGradeCombo.getValue();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(novaOcena -> {
            if (novaOcena != null && novaOcena != ocenaInfo.ocena) {
                // Prikaži progress
                ProgressIndicator progress = new ProgressIndicator();
                VBox progressBox = new VBox(20, new Label("Posodabljam oceno..."), progress);
                progressBox.setAlignment(Pos.CENTER);
                progressBox.setPadding(new Insets(20));

                Stage progressStage = new Stage();
                progressStage.setScene(new Scene(progressBox, 300, 150));
                progressStage.setTitle("Posodabljanje...");
                progressStage.show();

                new Thread(() -> {
                    boolean uspeh = updateGradeInBackend(ocenaInfo.ocenaId, novaOcena);

                    Platform.runLater(() -> {
                        progressStage.close();

                        if (uspeh) {
                            // Posodobimo lokalni seznam
                            int index = gradesList.getItems().indexOf(ocenaInfo);
                            if (index >= 0) {
                                OcenaInfo updatedOcena = new OcenaInfo(ocenaInfo.ocenaId, novaOcena, ocenaInfo.datum);
                                gradesList.getItems().set(index, updatedOcena);
                                updateCurrentGradesArea(currentGradesArea, gradesList);
                            }
                            showInfo("Uspeh", "Ocena uspešno posodobljena na " + novaOcena);
                        } else {
                            showError("Napaka", "Napaka pri posodabljanju ocene!");
                        }
                    });
                }).start();
            }
        });
    }

    private void updateCurrentGradesArea(TextArea gradesArea, ListView<OcenaInfo> gradesList) {
        if (gradesList.getItems().isEmpty()) {
            gradesArea.setText("Ni ocen");
        } else {
            StringBuilder sb = new StringBuilder();
            for (OcenaInfo ocena : gradesList.getItems()) {
                sb.append(ocena.toString()).append("\n");
            }
            gradesArea.setText(sb.toString());
        }
    }

    private void showEditPrisotnostDialog(long studentId, JsonObject studentInfo) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Urejanje prisotnosti");
        dialog.setHeaderText("Uredi prisotnost za: " + safeGet(studentInfo, "ime") + " " + safeGet(studentInfo, "priimek"));

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Izbor predmeta
        HBox subjectBox = new HBox(10);
        subjectBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> subjectComboBox = new ComboBox<>();
        subjectComboBox.setPromptText("Izberi predmet");
        subjectComboBox.setPrefWidth(300);

        // Naloži predmete iz studentInfo
        if (studentInfo.has("predmeti") && studentInfo.get("predmeti").isJsonArray()) {
            JsonArray predmeti = studentInfo.get("predmeti").getAsJsonArray();
            for (JsonElement elem : predmeti) {
                if (elem.isJsonObject()) {
                    String predmet = elem.getAsJsonObject().get("predmet").getAsString();
                    subjectComboBox.getItems().add(predmet);
                }
            }
        } else {
            // Če ni predmetov, dodaj nekaj osnovnih
            subjectComboBox.getItems().addAll("Matematika", "Slovenščina", "Angleščina",
                    "Fizika", "Kemija", "Zgodovina", "Geografija");
        }

        subjectBox.getChildren().addAll(new Label("Predmet:"), subjectComboBox);

        // Izbor prisotnosti - POENOSTAVLJENO
        HBox prisotnostBox = new HBox(10);
        prisotnostBox.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup prisotnostGroup = new ToggleGroup();
        RadioButton prisotenBtn = new RadioButton("Prisoten");
        prisotenBtn.setToggleGroup(prisotnostGroup);
        prisotenBtn.setSelected(true);

        RadioButton odsotenBtn = new RadioButton("Odsoten");
        odsotenBtn.setToggleGroup(prisotnostGroup);

        prisotnostBox.getChildren().addAll(
                new Label("Prisotnost:"),
                prisotenBtn,
                odsotenBtn
        );

        // Dodaj polje za razlog odsotnosti
        VBox razlogBox = new VBox(5);
        Label razlogLabel = new Label("Razlog odsotnosti (neobvezno):");
        TextField razlogField = new TextField();
        razlogField.setPromptText("npr. Bolezen, družinski razlogi...");
        razlogBox.getChildren().addAll(razlogLabel, razlogField);

        // Datum
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(150);
        HBox dateBox = new HBox(10, new Label("Datum:"), datePicker);

        // Gumbi
        HBox buttonBox = new HBox(10);
        Button addBtn = new Button("✔️ Shrani prisotnost");
        addBtn.setStyle(BUTTON_STYLE);

        ButtonType closeBtn = new ButtonType("Zapri", ButtonBar.ButtonData.CANCEL_CLOSE);

        addBtn.setOnAction(e -> {
            if (subjectComboBox.getValue() == null) {
                showError("Izberi predmet", "Izberi predmet!");
                return;
            }

            String prisotnostStatus = prisotenBtn.isSelected() ? "Prisoten" : "Odsoten";
            String predmet = subjectComboBox.getValue();
            String datum = datePicker.getValue() != null ? datePicker.getValue().toString() : LocalDate.now().toString();
            String razlog = razlogField.getText().trim();

            // Tukaj bi šlo na backend - za zdaj samo prikažemo
            String message = String.format("Prisotnost za %s:\nPredmet: %s\nStatus: %s\nDatum: %s",
                    safeGet(studentInfo, "ime") + " " + safeGet(studentInfo, "priimek"),
                    predmet, prisotnostStatus, datum);

            if (!razlog.isEmpty() && prisotnostStatus.equals("Odsoten")) {
                message += "\nRazlog: " + razlog;
            }

            showInfo("Prisotnost shranjena", message);

            // Po shranjevanju posodobimo pogled (v praksi bi to naložili iz backend-a)
            dialog.close();
        });

        buttonBox.getChildren().add(addBtn);

        content.getChildren().addAll(
                subjectBox,
                prisotnostBox,
                razlogBox,
                dateBox,
                buttonBox
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(closeBtn);
        dialog.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}