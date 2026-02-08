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

    // POMOŽNE METODE
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void sendStudent(JsonObject student, String method, String endpoint) {
        try {
            URL url = new URL(BACKEND_URL + "/dijaki" + endpoint);
            System.out.println("DEBUG: Pošiljam na: " + url);
            System.out.println("DEBUG: Metoda: " + method);
            System.out.println("DEBUG: Podatki: " + student.toString());

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
            System.out.println("DEBUG: Status odgovora: " + status);

            if (status == 200 || status == 201) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                System.out.println("DEBUG: Uspešen odgovor: " + response.toString());

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
                System.out.println("DEBUG: Napaka - status: " + status);

                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    System.out.println("DEBUG: Napaka od strežnika: " + errorResponse.toString());

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

    // Nova metoda za pošiljanje ocene na backend
    private boolean addGradeToBackend(long studentId, long dijakPredmetId, int ocena) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("dijakPredmetId", dijakPredmetId);
            request.addProperty("ocena", ocena);
            request.addProperty("dijakId", studentId);

            // DODANO: Datum za zgodovino ocen
            String datum = LocalDate.now().toString();
            request.addProperty("datumVpisa", datum);

            System.out.println("DEBUG: Pošiljam oceno: " + request.toString());

            // Uporabi pravilno metodo POST
            String response = sendPostRequest("/api/ocene/dodaj", request.toString());
            System.out.println("DEBUG: Odgovor strežnika: " + response);

            // Preveri odgovor
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                return true;
            } else if (jsonResponse.has("message") && jsonResponse.get("message").getAsString().toLowerCase().contains("uspešno")) {
                return true;
            } else if (jsonResponse.has("id")) { // Če vrne ID nove ocene
                return true;
            }

            return false;

        } catch (Exception e) {
            System.out.println("DEBUG: Napaka pri dodajanju ocene: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Nova metoda za brisanje ocene
    private boolean deleteGradeFromBackend(long ocenaId) {
        try {
            URL url = new URL(BACKEND_URL + "/api/ocene/" + ocenaId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int status = conn.getResponseCode();
            return status == 200;

        } catch (Exception e) {
            System.out.println("DEBUG: Napaka pri brisanju ocene: " + e.getMessage());
            return false;
        }
    }

    // Nova metoda za pridobivanje predmetov iz backend-a
    private List<PredmetInfo> loadPredmetiFromBackend(long studentId) {
        List<PredmetInfo> predmeti = new ArrayList<>();

        try {
            URL url = new URL(BACKEND_URL + "/api/ocene/dijak/" + studentId + "/predmeti");
            System.out.println("DEBUG: Nalagam predmete za studenta: " + studentId);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                System.out.println("DEBUG: Token poslan za predmete");
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG: Status za predmete: " + status);

            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                System.out.println("DEBUG: Odgovor za predmete: " + response.toString());

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
                                System.out.println("DEBUG: Dodan predmet: " + ime + " (ID: " + dpId + ")");
                            }
                        }
                    }
                }
            } else {
                // Preberi napako
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) errorResponse.append(errorLine);
                errorReader.close();
                System.out.println("DEBUG: Napaka pri predmetih: " + errorResponse.toString());
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Napaka pri nalaganju predmetov: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("DEBUG: Skupaj predmetov: " + predmeti.size());
        return predmeti;
    }

    // Nova metoda za pridobivanje ocen iz backend-a
    private List<Integer> loadOceneFromBackend(long studentId, String predmetIme) {
        List<Integer> ocene = new ArrayList<>();

        try {
            // Encode predmet ime za URL
            String encodedPredmetIme = java.net.URLEncoder.encode(predmetIme, "UTF-8");
            URL url = new URL(BACKEND_URL + "/api/ocene/dijak/" + studentId + "/predmet/" + encodedPredmetIme);
            System.out.println("DEBUG: Nalagam ocene za predmet: " + predmetIme);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG: Status za ocene: " + status);

            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                System.out.println("DEBUG: Odgovor za ocene: " + response.toString());

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                if (json.has("ocene") && json.get("ocene").isJsonArray()) {
                    JsonArray oceneArray = json.get("ocene").getAsJsonArray();
                    for (JsonElement elem : oceneArray) {
                        if (elem.isJsonObject()) {
                            JsonObject ocenaObj = elem.getAsJsonObject();
                            if (ocenaObj.has("ocena")) {
                                ocene.add(ocenaObj.get("ocena").getAsInt());
                            }
                        }
                    }
                }
            } else {
                // Preberi napako
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) errorResponse.append(errorLine);
                errorReader.close();
                System.out.println("DEBUG: Napaka pri ocenah: " + errorResponse.toString());
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Napaka pri nalaganju ocen: " + e.getMessage());
            e.printStackTrace();
        }

        return ocene;
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
            System.out.println("DEBUG: Login response: " + response);

            if ("Prijava uspešna".equalsIgnoreCase(response) || "Login successful".equalsIgnoreCase(response)) {
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

            System.out.println("DEBUG: Pošiljam login za: " + email);
            String response = sendPostRequest("/auth/login", obj.toString());
            System.out.println("DEBUG: Login odgovor: " + response);

            // Tukaj je kĺjučni popravek - pravilno parsaj JSON odgovor
            JsonElement jsonResponse = JsonParser.parseString(response);

            if (jsonResponse.isJsonObject()) {
                JsonObject responseObj = jsonResponse.getAsJsonObject();

                // Preveri za token
                if (responseObj.has("token")) {
                    authToken = responseObj.get("token").getAsString();
                    System.out.println("DEBUG: Token shranjen, dolžina: " + authToken.length());
                    return "Prijava uspešna";
                }
                // Preveri za message
                else if (responseObj.has("message")) {
                    String message = responseObj.get("message").getAsString();
                    if (message.contains("uspešna") || message.contains("successful")) {
                        // Če je token v drugem polju
                        if (responseObj.has("accessToken")) {
                            authToken = responseObj.get("accessToken").getAsString();
                        } else if (responseObj.has("authToken")) {
                            authToken = responseObj.get("authToken").getAsString();
                        }
                        return "Prijava uspešna";
                    }
                    return message;
                }
            }

            return "Napaka: nepričakovan odgovor od strežnika";

        } catch (Exception e) {
            e.printStackTrace();
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
            System.out.println("DEBUG: Pošiljam na URL: " + url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // Vedno pošlji token, če je na voljo
            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                System.out.println("DEBUG: Pošiljam token z zahtevo");
            }

            try(OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes());
                os.flush();
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG: Status odgovora za " + endpoint + ": " + status);

            InputStream inputStream = (status < 400) ? conn.getInputStream() : conn.getErrorStream();

            try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                System.out.println("DEBUG: Odgovor od strežnika: " + response.toString());
                return response.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("DEBUG: Napaka pri povezavi s strežnikom: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private void showMainScene() {
        System.out.println("DEBUG: Prikazujem glavno sceno, token: " + (authToken != null ? "PRISOTEN" : "ODSOTEN"));

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
            System.out.println("DEBUG: Nalagam dijake iz: " + url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                System.out.println("DEBUG: Token poslan za dijake");
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG: Status za dijake: " + status);

            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                System.out.println("DEBUG: Uspešno naloženi dijaki");
                displayStudents(response.toString());
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) errorResponse.append(errorLine);
                errorReader.close();

                System.out.println("DEBUG: Napaka pri nalaganju dijakov: " + errorResponse.toString());
                showError("Napaka pri nalaganju", "Strežnik je vrnil status: " + status + "\n" + errorResponse.toString());
            }

        } catch (Exception e) {
            showError("Napaka pri povezavi", "Ne morem se povezati s strežnikom.\nPreveri, ali je backend zagnan na " + BACKEND_URL + "\nNapaka: " + e.getMessage());
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

                System.out.println("DEBUG: Pošiljam dijaka: " + obj.toString());
                return obj;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(student -> {
            System.out.println("DEBUG: Dijak za pošiljanje: " + student.toString());
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

                System.out.println("DEBUG: Posodabljam dijaka: " + obj.toString());
                return obj;
            }
            return null;
        });

        Long studentId = selected.get("id").getAsLong();
        dialog.showAndWait().ifPresent(student -> {
            System.out.println("DEBUG: Pošiljam posodobitev: " + student.toString());
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
            System.out.println("DEBUG: Nalagam info za dijaka: " + id);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (authToken != null && !authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int status = conn.getResponseCode();
            System.out.println("DEBUG: Status za info: " + status);

            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JsonObject info = JsonParser.parseString(response.toString()).getAsJsonObject();
                showStudentInfoWithGrades(id, info);

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

    private void showStudentInfoWithGrades(long studentId, JsonObject studentInfo) {
        // Ustvari dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Podrobnosti o dijaku");
        alert.setHeaderText("Informacije o dijaku: " + safeGet(studentInfo, "ime") + " " + safeGet(studentInfo, "priimek"));

        // Glavni VBox
        VBox dialogContent = new VBox(15);
        dialogContent.setPadding(new Insets(15));

        // 1. OSNOVNI PODATKI
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

        // 2. PREDMETI IN OCENE z gumbom za urejanje
        VBox gradesBox = new VBox(10);
        gradesBox.setPadding(new Insets(15));
        gradesBox.setStyle("-fx-border-color: #ccc; -fx-border-radius: 8; -fx-background-color: #f9f9f9;");

        // Gumb za urejanje ocen
        Button editGradesBtn = new Button("✏️ UREDI OCENE");
        editGradesBtn.setStyle(EDIT_BUTTON_STYLE);
        editGradesBtn.setMaxWidth(Double.MAX_VALUE);

        editGradesBtn.setOnAction(e -> {
            alert.close();
            showEditGradesDialog(studentId, studentInfo);
        });

        // Naslov za ocene
        Label gradesTitle = new Label("📚 PREDMETI IN OCENE:");
        gradesTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");

        // Tabela s predmeti in ocenami
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

                        // Ocene
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

                                        // Izračun povprečja
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

        // Dodaj vse v gradesBox
        gradesBox.getChildren().addAll(gradesTitle, gradesList, editGradesBtn);

        // 3. Dodaj vse v dialog
        dialogContent.getChildren().addAll(basicInfoBox, gradesBox);

        ScrollPane scrollPane = new ScrollPane(dialogContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(700, 500);

        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().setPrefSize(720, 520);

        // Gumb za zapiranje
        ButtonType closeButton = new ButtonType("Zapri", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(closeButton);

        alert.showAndWait();
    }

    private double izracunajPovprecje(List<String> oceneList) {
        try {
            double sum = 0;
            int count = 0;

            for (String ocena : oceneList) {
                try {
                    // Poskusimo pridobiti številčno vrednost (ignore črke)
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

    // Nova popravljena metoda za urejanje ocen
    private void showEditGradesDialog(long studentId, JsonObject studentInfo) {
        // Ustvari dialog TAKOJ - ne čakaj
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Urejanje ocen");
        dialog.setHeaderText("Uredi ocene za: " + safeGet(studentInfo, "ime") + " " + safeGet(studentInfo, "priimek"));

        // DODANO: Nastavi minimalno in preferirano velikost dialoga
        dialog.getDialogPane().setMinSize(800, 600);
        dialog.getDialogPane().setPrefSize(900, 650);

        // Loading screen - pokaži takoj
        VBox loadingBox = new VBox(20);
        loadingBox.setPadding(new Insets(30));
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setMinSize(400, 300);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(50, 50);

        Label loadingLabel = new Label("Nalagam podatke...");
        loadingLabel.setStyle("-fx-font-size: 14px;");

        loadingBox.getChildren().addAll(spinner, loadingLabel);
        dialog.getDialogPane().setContent(loadingBox);

        // Gumb za zapiranje
        ButtonType closeButton = new ButtonType("Zapri", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        // DODANO: Nastavi, da se dialog prilagodi vsebini
        dialog.setResizable(true);

        // Prikaži dialog TAKOJ
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setMinWidth(800);
        stage.setMinHeight(600);

        dialog.show();

        // Naloži podatke v ozadju
        new Thread(() -> {
            try {
                // 1. Pridobi predmete iz backend-a
                List<PredmetInfo> predmetiInfo = loadPredmetiFromBackend(studentId);

                // 2. Če backend ne deluje, uporabi podatke iz studentInfo
                if (predmetiInfo.isEmpty()) {
                    predmetiInfo = extractPredmetiFromStudentInfo(studentInfo);
                }

                // 3. Če še vedno ni, uporabi dummy podatke
                if (predmetiInfo.isEmpty()) {
                    predmetiInfo = Arrays.asList(
                            new PredmetInfo("Matematika", 1L),
                            new PredmetInfo("Slovenščina", 2L),
                            new PredmetInfo("Angleščina", 3L)
                    );
                }

                // 4. Posodobi UI v JavaFX thread-u
                List<PredmetInfo> finalPredmetiInfo = predmetiInfo;
                Platform.runLater(() -> {
                    VBox dialogContent = createGradesDialogContent(studentId, finalPredmetiInfo, studentInfo);

                    // DODANO: Nastavi velikost vsebine
                    dialogContent.setMinSize(750, 550);
                    dialogContent.setPrefSize(800, 600);

                    dialog.getDialogPane().setContent(dialogContent);

                    // Dodaj gumb za shranjevanje
                    ButtonType saveButton = new ButtonType("💾 Shrani", ButtonBar.ButtonData.OK_DONE);
                    dialog.getDialogPane().getButtonTypes().clear();
                    dialog.getDialogPane().getButtonTypes().addAll(saveButton, closeButton);

                    // DODANO: Posodobi velikost dialoga glede na novo vsebino
                    dialog.getDialogPane().getScene().getWindow().sizeToScene();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Napaka", "Ne morem naložiti podatkov: " + e.getMessage());

                    // Prikaži vsaj dummy vsebino
                    List<PredmetInfo> dummy = Arrays.asList(
                            new PredmetInfo("Matematika", 1L),
                            new PredmetInfo("Slovenščina", 2L)
                    );
                    VBox dialogContent = createGradesDialogContent(studentId, dummy, studentInfo);
                    dialogContent.setMinSize(750, 550);
                    dialogContent.setPrefSize(800, 600);
                    dialog.getDialogPane().setContent(dialogContent);
                });
            }
        }).start();
    }

    // Nova metoda za ekstrakcijo predmetov iz studentInfo
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
                        // Uporabi index + 1 kot dummy ID
                        predmeti.add(new PredmetInfo(predmetIme, (long) (i + 1)));
                    }
                }
            }
        }

        return predmeti;
    }

    // Nova metoda za ustvarjanje vsebine dialoga
    private VBox createGradesDialogContent(long studentId, List<PredmetInfo> predmetiInfo, JsonObject studentInfo) {
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));

        // 1. Izbira predmeta
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
                return null; // Ni potrebno
            }
        });

        // Dodaj predmete v ComboBox
        subjectComboBox.getItems().addAll(predmetiInfo);

        Button refreshBtn = new Button("🔄");
        refreshBtn.setTooltip(new Tooltip("Osveži seznam"));
        refreshBtn.setOnAction(e -> {
            // Ponovno naloži predmete
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

        // 2. Trenutne ocene
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

        // 3. Dodajanje nove ocene
        VBox addGradeBox = new VBox(10);
        addGradeBox.setPadding(new Insets(15));
        addGradeBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #f8f9fa;");

        Label addTitle = new Label("➕ DODAJ NOVO OCENO:");
        addTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox gradeInputBox = new HBox(10);
        gradeInputBox.setAlignment(Pos.CENTER_LEFT);

        // Ocena
        ComboBox<Integer> gradeComboBox = new ComboBox<>();
        gradeComboBox.getItems().addAll(1, 2, 3, 4, 5);
        gradeComboBox.setValue(5);
        gradeComboBox.setPrefWidth(80);

        // Gumb za dodajanje
        Button addGradeBtn = new Button("➕ Dodaj oceno");
        addGradeBtn.setStyle(BUTTON_STYLE);

        gradeInputBox.getChildren().addAll(
                new Label("Ocena:"), gradeComboBox,
                addGradeBtn
        );

        addGradeBox.getChildren().addAll(addTitle, gradeInputBox);

        // 4. Seznam vseh ocen
        VBox allGradesBox = new VBox(10);
        allGradesBox.setPadding(new Insets(15));
        allGradesBox.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #f8f9fa;");

        Label allGradesLabel = new Label("📝 VSE OCENE:");
        allGradesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ListView<String> allGradesList = new ListView<>();
        allGradesList.setPrefHeight(200);
        allGradesList.setMinHeight(150);

        allGradesBox.getChildren().addAll(allGradesLabel, allGradesList);

        // 5. Dodaj vse v glavni layout
        mainLayout.getChildren().addAll(
                subjectBox, currentGradesBox, addGradeBox, allGradesBox
        );

        // 6. Event handler za izbiro predmeta
        subjectComboBox.setOnAction(e -> {
            PredmetInfo selectedPredmet = subjectComboBox.getValue();
            if (selectedPredmet != null) {
                // Naloži ocene za izbrani predmet
                new Thread(() -> {
                    List<Integer> ocene = loadOceneFromBackend(studentId, selectedPredmet.ime);

                    Platform.runLater(() -> {
                        allGradesList.getItems().clear();
                        for (Integer ocena : ocene) {
                            allGradesList.getItems().add("Ocena: " + ocena);
                        }
                        updateCurrentGradesArea(currentGradesArea, allGradesList);
                    });
                }).start();
            }
        });

        // 7. Event handler za dodajanje ocene
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

            // DODANO: Disable gumb med pošiljanjem
            addGradeBtn.setDisable(true);
            addGradeBtn.setText("Pošiljam...");

            // Dodaj oceno v backend
            new Thread(() -> {
                boolean uspeh = addGradeToBackend(studentId, selectedPredmet.dijakPredmetId, ocena);

                Platform.runLater(() -> {
                    addGradeBtn.setDisable(false);
                    addGradeBtn.setText("➕ Dodaj oceno");

                    if (uspeh) {
                        // Osveži seznam ocen
                        allGradesList.getItems().add("Ocena: " + ocena + " (dodano: " + LocalDate.now() + ")");
                        updateCurrentGradesArea(currentGradesArea, allGradesList);
                        showInfo("Uspeh", "Ocena " + ocena + " uspešno dodana za predmet " + selectedPredmet.ime);

                        // Ponastavi izbiro ocene
                        gradeComboBox.setValue(5);

                        // DODANO: Ponovno naloži ocene iz backend-a za posodobljen seznam
                        new Thread(() -> {
                            List<Integer> ocene = loadOceneFromBackend(studentId, selectedPredmet.ime);
                            Platform.runLater(() -> {
                                allGradesList.getItems().clear();
                                for (Integer o : ocene) {
                                    allGradesList.getItems().add("Ocena: " + o);
                                }
                                updateCurrentGradesArea(currentGradesArea, allGradesList);
                            });
                        }).start();
                    } else {
                        showError("Napaka", "Napaka pri dodajanju ocene! Preverite konzolo za podrobnosti.");
                    }
                });
            }).start();
        });

        // 8. Event handler za brisanje ocene
        allGradesList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedGrade = allGradesList.getSelectionModel().getSelectedItem();
                if (selectedGrade != null) {
                    // V praksi bi tukaj dobili ID ocene iz backend-a
                    // Za zdaj samo odstranimo iz seznama
                    allGradesList.getItems().remove(selectedGrade);
                    updateCurrentGradesArea(currentGradesArea, allGradesList);
                    showInfo("Uspeh", "Ocena odstranjena (lokalen seznam)");
                }
            }
        });

        return mainLayout;
    }

    private void updateCurrentGradesArea(TextArea gradesArea, ListView<String> gradesList) {
        if (gradesList.getItems().isEmpty()) {
            gradesArea.setText("Ni ocen");
        } else {
            gradesArea.setText(String.join("\n", gradesList.getItems()));
        }
    }

    public static void main(String[] args) {
        launch();
    }
}