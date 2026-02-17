package com.example.backend.controller;

import com.example.backend.entity.Dijak;
import com.example.backend.entity.Razred;
import com.example.backend.repository.DijakRepository;
import com.example.backend.repository.LogRepository;
import com.example.backend.repository.OcenaRepository;
import com.example.backend.repository.RazredRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/dijaki")
public class DijakController {

    @Autowired
    private DijakRepository dijakRepository;

    @Autowired
    private OcenaRepository ocenaRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private RazredRepository razredRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ========== POMOŽNE METODE ==========

    private Map<String, String> createError(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    private ResponseEntity<?> handleDatabaseException(Exception e) {
        e.printStackTrace();

        String errorMessage = "Napaka v podatkih";
        Throwable cause = e;

        // Pojdi do root cause
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        if (cause.getMessage() != null) {
            errorMessage = cause.getMessage();

            // Čiščenje napake - odstrani oklepaje z dodatnimi podatki
            if (errorMessage.contains("(") && errorMessage.contains(")")) {
                int startIndex = errorMessage.indexOf("(");
                int endIndex = errorMessage.indexOf(")");
                if (startIndex < endIndex) {
                    String toRemove = errorMessage.substring(startIndex, endIndex + 1);
                    errorMessage = errorMessage.replace(toRemove, "").trim();
                }
            }

            // Standardiziraj sporočila za EMSO
            if (errorMessage.contains("EMSO mora imeti točno") && !errorMessage.contains("znakov")) {
                errorMessage += " znakov";
            }

            // Standardiziraj sporočila za telefonsko
            if (errorMessage.contains("telefonska") || errorMessage.contains("števk") || errorMessage.contains("Telefonska")) {
                if (!errorMessage.startsWith("Telefonska")) {
                    errorMessage = "Telefonska številka: " + errorMessage;
                }
            }
        }

        return ResponseEntity.badRequest().body(createError(errorMessage));
    }

    // ========== VALIDACIJSKE METODE ==========

    private String validateEmso(String emso) {
        if (emso == null || emso.trim().isEmpty()) {
            return "EMSO je obvezno polje";
        }

        emso = emso.trim();

        // Preveri dolžino
        if (emso.length() != 13) {
            return "EMSO mora imeti točno 13 znakov. Vnešeno: " + emso.length() + " znakov";
        }

        // Preveri, če so samo številke
        if (!emso.matches("\\d{13}")) {
            return "EMSO sme vsebovati samo številke (0-9)";
        }

        return null; // Ni napake
    }

    private String validateTelefonska(String telefonska) {
        if (telefonska == null || telefonska.trim().isEmpty()) {
            return null; // Telefonska ni obvezna
        }

        telefonska = telefonska.trim();
        if (telefonska.isEmpty()) {
            return null; // Prazna je OK
        }

        // Odstrani vse ne-številske znake (razen + na začetku)
        String cleanTel = telefonska.replaceAll("[^0-9+]", "");

        // Preveri dolžino
        if (cleanTel.length() < 8 || cleanTel.length() > 12) {
            return "Telefonska številka mora imeti 8-12 števk (brez formatiranja)";
        }

        // Preveri, če se začne z 0 ali +386
        if (!cleanTel.startsWith("0") && !cleanTel.startsWith("+386")) {
            return "Telefonska številka se mora začeti z 0 (Slovenija) ali +386 (mednarodno)";
        }

        return null; // Ni napake
    }

    // ========== GET METODE ==========

    @GetMapping("")
    public ResponseEntity<?> getAllDijaki() {
        try {
            List<Dijak> dijaki = dijakRepository.findAll();
            List<Map<String, Object>> result = new ArrayList<>();

            for (Dijak d : dijaki) {
                Map<String, Object> dijakMap = new HashMap<>();
                dijakMap.put("id", d.getId());
                dijakMap.put("ime", d.getIme());
                dijakMap.put("priimek", d.getPriimek());
                dijakMap.put("emso", d.getEmso());
                dijakMap.put("telefonska", d.getTelefonska());
                dijakMap.put("datumRojstva", d.getDatumRojstva());

                if (d.getRazred() != null) {
                    dijakMap.put("razred", d.getRazred().getImeRazreda());
                    dijakMap.put("razredId", d.getRazred().getId());
                } else {
                    dijakMap.put("razred", "Ni razreda");
                    dijakMap.put("razredId", null);
                }

                result.add(dijakMap);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDijak(@PathVariable Long id) {
        try {
            Optional<Dijak> dijakOpt = dijakRepository.findById(id);

            if (dijakOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Dijak d = dijakOpt.get();
            Map<String, Object> result = new HashMap<>();
            result.put("id", d.getId());
            result.put("ime", d.getIme());
            result.put("priimek", d.getPriimek());
            result.put("emso", d.getEmso());
            result.put("telefonska", d.getTelefonska());
            result.put("datumRojstva", d.getDatumRojstva());

            if (d.getRazred() != null) {
                result.put("razred", d.getRazred().getImeRazreda());
                result.put("razredId", d.getRazred().getId());
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    @GetMapping("/{id}/info")
    public ResponseEntity<?> getDijakInfo(@PathVariable Long id) {
        try {
            Optional<Dijak> dijakOpt = dijakRepository.findById(id);

            if (dijakOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Dijak d = dijakOpt.get();
            Map<String, Object> result = new HashMap<>();

            result.put("id", d.getId());
            result.put("ime", d.getIme());
            result.put("priimek", d.getPriimek());
            result.put("emso", d.getEmso());
            result.put("telefonska", d.getTelefonska());
            result.put("datumRojstva", d.getDatumRojstva());

            if (d.getRazred() != null) {
                result.put("razred", d.getRazred().getImeRazreda());
                result.put("razredId", d.getRazred().getId());
            } else {
                result.put("razred", "Ni razreda");
                result.put("razredId", null);
            }

            List<Map<String, Object>> predmetiList = new ArrayList<>();
            Map<String, List<Integer>> ocenePoPredmetih = new HashMap<>();

            List<Object[]> oceneResult = ocenaRepository.findOceneWithPredmetByDijakId(id);

            for (Object[] row : oceneResult) {
                String predmetIme = (String) row[0];
                Integer ocena = (Integer) row[1];

                if (!ocenePoPredmetih.containsKey(predmetIme)) {
                    ocenePoPredmetih.put(predmetIme, new ArrayList<>());
                }
                ocenePoPredmetih.get(predmetIme).add(ocena);
            }

            for (Map.Entry<String, List<Integer>> entry : ocenePoPredmetih.entrySet()) {
                Map<String, Object> predmetMap = new HashMap<>();
                predmetMap.put("predmet", entry.getKey());
                predmetMap.put("ocene", entry.getValue());

                double povprecje = entry.getValue().stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);
                predmetMap.put("povprecje", Math.round(povprecje * 100.0) / 100.0);

                predmetiList.add(predmetMap);
            }

            result.put("predmeti", predmetiList);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDijaki(
            @RequestParam(required = false) String ime,
            @RequestParam(required = false) String priimek,
            @RequestParam(required = false) String emso) {
        try {
            List<Dijak> dijaki;

            if (ime != null && priimek != null) {
                dijaki = dijakRepository.findByImeContainingIgnoreCaseAndPriimekContainingIgnoreCase(ime, priimek);
            } else if (ime != null) {
                dijaki = dijakRepository.findByImeContainingIgnoreCase(ime);
            } else if (priimek != null) {
                dijaki = dijakRepository.findByPriimekContainingIgnoreCase(priimek);
            } else if (emso != null) {
                dijaki = dijakRepository.findByEmsoContaining(emso);
            } else {
                dijaki = dijakRepository.findAll();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Dijak d : dijaki) {
                Map<String, Object> dijakMap = new HashMap<>();
                dijakMap.put("id", d.getId());
                dijakMap.put("ime", d.getIme());
                dijakMap.put("priimek", d.getPriimek());
                dijakMap.put("emso", d.getEmso());
                dijakMap.put("telefonska", d.getTelefonska());
                dijakMap.put("datumRojstva", d.getDatumRojstva());

                if (d.getRazred() != null) {
                    dijakMap.put("razred", d.getRazred().getImeRazreda());
                    dijakMap.put("razredId", d.getRazred().getId());
                }

                result.add(dijakMap);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("stevilo", result.size());
            response.put("rezultati", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    // ========== POST /dijaki - USTVARI DIJAKA ==========

    @PostMapping("")
    @Transactional
    public ResponseEntity<?> createDijak(@RequestBody Map<String, Object> dijakData) {
        try {
            // 1. Preveri obvezna polja
            if (!dijakData.containsKey("ime") || dijakData.get("ime") == null || dijakData.get("ime").toString().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createError("Polje 'ime' je obvezno in ne sme biti prazno"));
            }
            if (!dijakData.containsKey("priimek") || dijakData.get("priimek") == null || dijakData.get("priimek").toString().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createError("Polje 'priimek' je obvezno in ne sme biti prazno"));
            }
            if (!dijakData.containsKey("emso") || dijakData.get("emso") == null || dijakData.get("emso").toString().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createError("Polje 'emso' je obvezno in ne sme biti prazno"));
            }

            // 2. Validiraj podatke
            String ime = dijakData.get("ime").toString().trim();
            String priimek = dijakData.get("priimek").toString().trim();
            String emso = dijakData.get("emso").toString().trim();

            // Validiraj EMSO
            String emsoError = validateEmso(emso);
            if (emsoError != null) {
                return ResponseEntity.badRequest().body(createError(emsoError));
            }

            // Validiraj telefonsko (če je podana)
            String telefonska = null;
            if (dijakData.containsKey("telefonska") && dijakData.get("telefonska") != null) {
                telefonska = dijakData.get("telefonska").toString().trim();
                String telError = validateTelefonska(telefonska);
                if (telError != null) {
                    return ResponseEntity.badRequest().body(createError(telError));
                }
            }

            // 3. Ustvari dijaka
            Dijak dijak = new Dijak();
            dijak.setIme(ime);
            dijak.setPriimek(priimek);
            dijak.setEmso(emso);

            if (telefonska != null && !telefonska.isEmpty()) {
                dijak.setTelefonska(telefonska);
            }

            // Datum rojstva
            if (dijakData.containsKey("datumRojstva") && dijakData.get("datumRojstva") != null) {
                String datumStr = dijakData.get("datumRojstva").toString().trim();
                if (!datumStr.isEmpty()) {
                    try {
                        LocalDate datum = LocalDate.parse(datumStr);
                        dijak.setDatumRojstva(datum);
                    } catch (DateTimeParseException e) {
                        return ResponseEntity.badRequest().body(createError("Neveljaven format datuma. Uporabi YYYY-MM-DD"));
                    }
                }
            }

            // Razred
            if (dijakData.containsKey("razredId") && dijakData.get("razredId") != null) {
                try {
                    Long razredId = Long.parseLong(dijakData.get("razredId").toString());
                    Optional<Razred> razredOpt = razredRepository.findById(razredId);
                    razredOpt.ifPresent(dijak::setRazred);
                } catch (NumberFormatException e) {
                    // Neveljaven razredId, ignoriraj
                }
            }

            // 4. Shrani dijaka
            Dijak savedDijak = dijakRepository.save(dijak);

            // 5. Vrni uspešen odgovor
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Dijak uspešno ustvarjen");
            response.put("id", savedDijak.getId());
            response.put("ime", savedDijak.getIme());
            response.put("priimek", savedDijak.getPriimek());
            response.put("emso", savedDijak.getEmso());

            if (savedDijak.getTelefonska() != null) {
                response.put("telefonska", savedDijak.getTelefonska());
            }
            if (savedDijak.getDatumRojstva() != null) {
                response.put("datumRojstva", savedDijak.getDatumRojstva().toString());
            }
            if (savedDijak.getRazred() != null) {
                response.put("razredId", savedDijak.getRazred().getId());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    // ========== PUT /dijaki/{id} - POSODABLJANJE ==========

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateDijak(@PathVariable Long id, @RequestBody Map<String, Object> dijakData) {
        try {
            Optional<Dijak> dijakOpt = dijakRepository.findById(id);

            if (dijakOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Dijak dijak = dijakOpt.get();
            boolean isChanged = false;

            // Ime
            if (dijakData.containsKey("ime") && dijakData.get("ime") != null) {
                String ime = dijakData.get("ime").toString().trim();
                if (!ime.isEmpty() && !ime.equals(dijak.getIme())) {
                    dijak.setIme(ime);
                    isChanged = true;
                }
            }

            // Priimek
            if (dijakData.containsKey("priimek") && dijakData.get("priimek") != null) {
                String priimek = dijakData.get("priimek").toString().trim();
                if (!priimek.isEmpty() && !priimek.equals(dijak.getPriimek())) {
                    dijak.setPriimek(priimek);
                    isChanged = true;
                }
            }

            // EMSO - validacija pred nastavitvijo
            if (dijakData.containsKey("emso") && dijakData.get("emso") != null) {
                String emso = dijakData.get("emso").toString().trim();
                if (!emso.isEmpty() && !emso.equals(dijak.getEmso())) {
                    String emsoError = validateEmso(emso);
                    if (emsoError != null) {
                        return ResponseEntity.badRequest().body(createError(emsoError));
                    }
                    dijak.setEmso(emso);
                    isChanged = true;
                }
            }

            // Telefonska - validacija pred nastavitvijo
            if (dijakData.containsKey("telefonska")) {
                String telefonska = dijakData.get("telefonska") != null ?
                        dijakData.get("telefonska").toString().trim() : null;

                if (telefonska == null || telefonska.isEmpty()) {
                    if (dijak.getTelefonska() != null) {
                        dijak.setTelefonska(null);
                        isChanged = true;
                    }
                } else {
                    String telError = validateTelefonska(telefonska);
                    if (telError != null) {
                        return ResponseEntity.badRequest().body(createError(telError));
                    }
                    if (!telefonska.equals(dijak.getTelefonska())) {
                        dijak.setTelefonska(telefonska);
                        isChanged = true;
                    }
                }
            }

            // Datum rojstva
            if (dijakData.containsKey("datumRojstva")) {
                if (dijakData.get("datumRojstva") != null) {
                    try {
                        String datumStr = dijakData.get("datumRojstva").toString().trim();
                        if (!datumStr.isEmpty()) {
                            LocalDate datum = LocalDate.parse(datumStr);
                            if (!datum.equals(dijak.getDatumRojstva())) {
                                dijak.setDatumRojstva(datum);
                                isChanged = true;
                            }
                        }
                    } catch (DateTimeParseException e) {
                        return ResponseEntity.badRequest().body(createError("Neveljaven format datuma. Uporabi YYYY-MM-DD"));
                    }
                } else {
                    if (dijak.getDatumRojstva() != null) {
                        dijak.setDatumRojstva(null);
                        isChanged = true;
                    }
                }
            }

            // Razred
            if (dijakData.containsKey("razredId")) {
                if (dijakData.get("razredId") != null) {
                    try {
                        Long razredId = Long.parseLong(dijakData.get("razredId").toString());
                        Optional<Razred> razredOpt = razredRepository.findById(razredId);

                        Razred novRazred = razredOpt.orElse(null);
                        Razred stariRazred = dijak.getRazred();

                        if ((novRazred == null && stariRazred != null) ||
                                (novRazred != null && !novRazred.equals(stariRazred))) {
                            dijak.setRazred(novRazred);
                            isChanged = true;
                        }
                    } catch (NumberFormatException e) {
                        if (dijak.getRazred() != null) {
                            dijak.setRazred(null);
                            isChanged = true;
                        }
                    }
                } else {
                    if (dijak.getRazred() != null) {
                        dijak.setRazred(null);
                        isChanged = true;
                    }
                }
            }

            if (isChanged) {
                dijakRepository.save(dijak);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Dijak uspešno posodobljen");
            response.put("id", id);
            response.put("spremembe", isChanged ? "Da" : "Ne");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    // ========== DELETE /dijaki/{id} ==========

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteDijak(@PathVariable Long id) {
        try {
            if (!dijakRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            Optional<Dijak> dijakOpt = dijakRepository.findById(id);
            String imePriimek = dijakOpt.map(d -> d.getIme() + " " + d.getPriimek()).orElse("Neznano");

            dijakRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Dijak uspešno izbrisan");
            response.put("id", id);
            response.put("imePriimek", imePriimek);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    // ========== LOG METODE ==========

    @GetMapping("/{id}/logs")
    public ResponseEntity<?> getDijakLogs(@PathVariable Long id) {
        try {
            boolean dijakExists = dijakRepository.existsById(id);

            if (!dijakExists) {
                String checkLogsSQL = """
                    SELECT COUNT(*) FROM log 
                    WHERE ukaz LIKE '%ID=' || ? || '%'
                """;

                Long logCount = (Long) entityManager.createNativeQuery(checkLogsSQL)
                        .setParameter(1, id)
                        .getSingleResult();

                if (logCount == 0) {
                    return ResponseEntity.status(404).body(createError("Dijak z ID " + id + " ni bil najden niti v logih"));
                }
            }

            String logsSQL = """
                SELECT ukaz, cas 
                FROM log 
                WHERE ukaz LIKE '%ID=' || ? || '%' 
                OR ukaz LIKE '%' || ? || '%'
                ORDER BY cas DESC
                LIMIT 50
            """;

            List<Object[]> logs = entityManager.createNativeQuery(logsSQL)
                    .setParameter(1, id)
                    .setParameter(2, "ID=" + id)
                    .getResultList();

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : logs) {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("ukaz", row[0]);
                logMap.put("cas", row[1]);
                result.add(logMap);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("dijakId", id);
            response.put("dijakObstaja", dijakExists);
            response.put("steviloLogov", result.size());
            response.put("logi", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    @GetMapping("/logs/recent")
    public ResponseEntity<?> getRecentLogs(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String operacija) {
        try {
            String sql;
            List<Object[]> logs;

            if (operacija != null && !operacija.isEmpty()) {
                sql = """
                    SELECT ukaz, cas 
                    FROM log 
                    WHERE ukaz LIKE ? 
                    ORDER BY cas DESC 
                    LIMIT ?
                """;
                logs = entityManager.createNativeQuery(sql)
                        .setParameter(1, "%" + operacija.toUpperCase() + "%")
                        .setParameter(2, limit)
                        .getResultList();
            } else {
                sql = """
                    SELECT ukaz, cas 
                    FROM log 
                    ORDER BY cas DESC 
                    LIMIT ?
                """;
                logs = entityManager.createNativeQuery(sql)
                        .setParameter(1, limit)
                        .getResultList();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            int insertCount = 0, updateCount = 0, deleteCount = 0;

            for (Object[] row : logs) {
                Map<String, Object> logMap = new HashMap<>();
                logMap.put("ukaz", row[0]);
                logMap.put("cas", row[1]);
                result.add(logMap);

                String ukaz = (String) row[0];
                if (ukaz.contains("INSERT")) insertCount++;
                else if (ukaz.contains("UPDATE")) updateCount++;
                else if (ukaz.contains("DELETE")) deleteCount++;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("steviloLogov", result.size());
            response.put("insert", insertCount);
            response.put("update", updateCount);
            response.put("delete", deleteCount);
            response.put("logi", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    // ========== TRIGGER METODE ==========

    @PostMapping("/test/trigger")
    @Transactional
    public ResponseEntity<?> testTrigger() {
        try {
            Map<String, Object> result = new HashMap<>();
            List<String> opombe = new ArrayList<>();

            // 1. Test INSERT
            opombe.add("1. Test INSERT...");
            Dijak testDijak = new Dijak();
            testDijak.setIme("Test");
            testDijak.setPriimek("Trigger");
            testDijak.setEmso("9999999999999");
            testDijak.setTelefonska("041123456");
            testDijak.setDatumRojstva(LocalDate.now().minusYears(18));

            Dijak savedDijak = dijakRepository.save(testDijak);
            opombe.add("   Ustvarjen dijak ID: " + savedDijak.getId());

            // 2. Test UPDATE
            opombe.add("2. Test UPDATE...");
            savedDijak.setPriimek("Trigger-Spremenjen");
            savedDijak.setTelefonska("041654321");
            dijakRepository.save(savedDijak);
            opombe.add("   Dijak posodobljen");

            // 3. Test DELETE
            opombe.add("3. Test DELETE...");
            Long dijakId = savedDijak.getId();
            dijakRepository.delete(savedDijak);
            opombe.add("   Dijak izbrisan");

            // 4. Preveri log vnose
            opombe.add("4. Preverjanje logov...");
            String checkLogsSQL = """
                SELECT ukaz, cas 
                FROM log 
                WHERE ukaz LIKE '%ID=' || ? || '%' 
                ORDER BY cas DESC
            """;

            List<Object[]> logs = entityManager.createNativeQuery(checkLogsSQL)
                    .setParameter(1, dijakId)
                    .getResultList();

            opombe.add("   Najdenih logov: " + logs.size());

            List<Map<String, String>> logEntries = new ArrayList<>();
            for (Object[] row : logs) {
                Map<String, String> log = new HashMap<>();
                log.put("ukaz", (String) row[0]);
                log.put("cas", row[1].toString());
                logEntries.add(log);
            }

            result.put("status", "USPEŠNO");
            result.put("testniDijakId", dijakId);
            result.put("steviloLogov", logs.size());
            result.put("logi", logEntries);
            result.put("opombe", opombe);
            result.put("message", "Trigger test uspešno izveden");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    @GetMapping("/check-trigger")
    public ResponseEntity<?> checkTrigger() {
        try {
            String sql = """
                SELECT 
                    trigger_name,
                    event_manipulation,
                    action_timing
                FROM information_schema.triggers
                WHERE event_object_table = 'dijaki'
                ORDER BY trigger_name
            """;

            List<Object[]> triggers = entityManager.createNativeQuery(sql).getResultList();

            List<Map<String, String>> triggerList = new ArrayList<>();
            for (Object[] row : triggers) {
                Map<String, String> trigger = new HashMap<>();
                trigger.put("ime", (String) row[0]);
                trigger.put("operacija", (String) row[1]);
                trigger.put("cas", (String) row[2]);
                triggerList.add(trigger);
            }

            boolean triggerExists = triggerList.stream()
                    .anyMatch(t -> "trigger_dijak_all".equals(t.get("ime")));

            Map<String, Object> response = new HashMap<>();
            response.put("triggerDijakAll", triggerExists ? "OBSTAJA" : "NE OBSTAJA");
            response.put("steviloTriggerjev", triggerList.size());
            response.put("triggerji", triggerList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    @PostMapping("/test-triggers")
    @Transactional
    public ResponseEntity<?> testTriggers() {
        try {
            Map<String, Object> result = new HashMap<>();
            List<String> testi = new ArrayList<>();

            // Test 1: Veljaven dijak
            testi.add("1. Test veljavnega dijaka...");
            Dijak dijak1 = new Dijak();
            dijak1.setIme("Test");
            dijak1.setPriimek("Validni");
            dijak1.setEmso("0101006500001");
            dijak1.setTelefonska("041123456");
            dijak1.setDatumRojstva(LocalDate.of(2000, 1, 1));

            Dijak saved1 = dijakRepository.save(dijak1);
            testi.add("   ✓ Dijak ustvarjen z ID: " + saved1.getId());

            // Test 2: Neveljaven EMSO (prekratek)
            testi.add("\n2. Test prekratkega EMSO (6 znakov)...");
            try {
                Dijak dijak2 = new Dijak();
                dijak2.setIme("Test");
                dijak2.setPriimek("Prekratek");
                dijak2.setEmso("123456"); // 6 znakov
                dijakRepository.save(dijak2);
                testi.add("   ✗ Pričakovana napaka ni bila vržena!");
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg.contains("13 znakov")) {
                    testi.add("   ✓ NAPAKA: " + errorMsg);
                } else {
                    testi.add("   ✗ NAPAČNA NAPAKA: " + errorMsg);
                }
            }

            // Test 3: Predolg EMSO
            testi.add("\n3. Test predolgega EMSO (14 znakov)...");
            try {
                Dijak dijak3 = new Dijak();
                dijak3.setIme("Test");
                dijak3.setPriimek("Predolg");
                dijak3.setEmso("12345678901234"); // 14 znakov
                dijakRepository.save(dijak3);
                testi.add("   ✗ Pričakovana napaka ni bila vržena!");
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg.contains("13 znakov")) {
                    testi.add("   ✓ NAPAKA: " + errorMsg);
                } else {
                    testi.add("   ✗ NAPAČNA NAPAKA: " + errorMsg);
                }
            }

            // Test 4: Neveljavna telefonska
            testi.add("\n4. Test prekratke telefonske (3 znaki)...");
            try {
                Dijak dijak4 = new Dijak();
                dijak4.setIme("Test");
                dijak4.setPriimek("SlabaTel");
                dijak4.setEmso("0101006500002");
                dijak4.setTelefonska("123"); // 3 znaki
                dijakRepository.save(dijak4);
                testi.add("   ✗ Pričakovana napaka ni bila vržena!");
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg.contains("telefonska") || errorMsg.contains("števk")) {
                    testi.add("   ✓ NAPAKA: " + errorMsg);
                } else {
                    testi.add("   ✗ NAPAČNA NAPAKA: " + errorMsg);
                }
            }

            result.put("testi", testi);
            result.put("status", "Testiranje končano");
            result.put("skupnoDijakov", dijakRepository.count());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    @GetMapping("/check-db-triggers")
    public ResponseEntity<?> checkDbTriggers() {
        try {
            String sql = """
                SELECT 
                    trigger_name,
                    event_manipulation,
                    action_timing,
                    action_statement
                FROM information_schema.triggers
                WHERE event_object_table = 'dijaki'
                ORDER BY trigger_name
            """;

            List<Object[]> triggers = entityManager.createNativeQuery(sql).getResultList();

            if (triggers.isEmpty()) {
                return ResponseEntity.ok(createError("V bazi NI triggerjev za tabelo 'dijaki'"));
            }

            List<Map<String, String>> triggerList = new ArrayList<>();
            for (Object[] row : triggers) {
                Map<String, String> trigger = new HashMap<>();
                trigger.put("ime", (String) row[0]);
                trigger.put("operacija", (String) row[1]);
                trigger.put("cas", (String) row[2]);
                trigger.put("sql", (String) row[3]);
                triggerList.add(trigger);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("steviloTriggerjev", triggerList.size());
            response.put("triggerji", triggerList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }

    // ========== TEST VALIDACIJE ==========

    @PostMapping("/test-validation")
    public ResponseEntity<?> testValidation() {
        List<Map<String, Object>> testResults = new ArrayList<>();

        // Test 1: Veljaven dijak
        Map<String, Object> test1 = new HashMap<>();
        test1.put("test", "Veljaven dijak");
        test1.put("status", "✓ PRIČAKOVAN USPEH");
        testResults.add(test1);

        // Test 2: Prekratek EMSO
        Map<String, Object> test2 = new HashMap<>();
        test2.put("test", "Prekratek EMSO (6 znakov)");
        test2.put("status", "✗ PRIČAKOVANA NAPAKA: EMSO mora imeti točno 13 znakov");
        testResults.add(test2);

        // Test 3: Predolg EMSO
        Map<String, Object> test3 = new HashMap<>();
        test3.put("test", "Predolg EMSO (14 znakov)");
        test3.put("status", "✗ PRIČAKOVANA NAPAKA: EMSO mora imeti točno 13 znakov");
        testResults.add(test3);

        // Test 4: EMSO s črkami
        Map<String, Object> test4 = new HashMap<>();
        test4.put("test", "EMSO s črkami");
        test4.put("status", "✗ PRIČAKOVANA NAPAKA: EMSO sme vsebovati samo številke");
        testResults.add(test4);

        // Test 5: Prekratka telefonska
        Map<String, Object> test5 = new HashMap<>();
        test5.put("test", "Prekratka telefonska (3 znaki)");
        test5.put("status", "✗ PRIČAKOVANA NAPAKA: Telefonska mora imeti 8-12 števk");
        testResults.add(test5);

        // Test 6: Telefonska brez 0
        Map<String, Object> test6 = new HashMap<>();
        test6.put("test", "Telefonska brez 0");
        test6.put("status", "✗ PRIČAKOVANA NAPAKA: Telefonska se mora začeti z 0 ali +386");
        testResults.add(test6);

        Map<String, Object> result = new HashMap<>();
        result.put("testi", testResults);
        result.put("opozorilo", "Pošlji dejanske POST zahteve za testiranje");

        return ResponseEntity.ok(result);
    }

    // ========== PREVERJANJE PODATKOV ==========

    @PostMapping("/check-data")
    public ResponseEntity<?> checkData(@RequestBody Map<String, Object> data) {
        try {
            List<String> napake = new ArrayList<>();

            if (data.containsKey("emso")) {
                String emso = data.get("emso").toString();
                String emsoError = validateEmso(emso);
                if (emsoError != null) {
                    napake.add(emsoError);
                }
            }

            if (data.containsKey("telefonska")) {
                String telefonska = data.get("telefonska").toString();
                String telError = validateTelefonska(telefonska);
                if (telError != null) {
                    napake.add(telError);
                }
            }

            if (napake.isEmpty()) {
                return ResponseEntity.ok(createError("Vsi podatki so veljavni"));
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("napake", napake);
                response.put("steviloNapak", napake.size());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            return handleDatabaseException(e);
        }
    }
}