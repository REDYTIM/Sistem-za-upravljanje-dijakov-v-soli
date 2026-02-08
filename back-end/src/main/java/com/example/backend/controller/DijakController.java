package com.example.backend.controller;

import com.example.backend.entity.Dijak;
import com.example.backend.entity.Razred;
import com.example.backend.repository.DijakRepository;
import com.example.backend.repository.RazredRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/dijaki")
public class DijakController {

    @Autowired
    private DijakRepository dijakRepository;

    @Autowired
    private RazredRepository razredRepository;

    // 1. GET vseh dijakov z osnovnimi podatki
    @GetMapping("")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllDijaki() {
        try {
            List<Dijak> dijaki = dijakRepository.findAll();
            List<Map<String, Object>> result = new ArrayList<>();

            for (Dijak d : dijaki) {
                Map<String, Object> dijakMap = new HashMap<>();
                dijakMap.put("id", d.getId());
                dijakMap.put("ime", d.getIme());
                dijakMap.put("priimek", d.getPriimek());
                dijakMap.put("emso", d.getEmso());

                // Telefonska (če obstaja)
                if (d.getTelefonska() != null && !d.getTelefonska().isEmpty()) {
                    dijakMap.put("telefonska", d.getTelefonska());
                }

                // Datum rojstva (če obstaja)
                if (d.getDatumRojstva() != null) {
                    dijakMap.put("datumRojstva", d.getDatumRojstva().toString());
                }

                // Razred - samo ime razreda kot string
                if (d.getRazred() != null) {
                    dijakMap.put("razred", d.getRazred().getImeRazreda());
                } else {
                    dijakMap.put("razred", "Ni razreda");
                }

                result.add(dijakMap);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // 2. GET podrobnosti o posameznem dijak
    @GetMapping("/{id}/info")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDijakInfo(@PathVariable Long id) {
        try {
            Optional<Dijak> dijakOpt = dijakRepository.findById(id);

            if (dijakOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Dijak z ID " + id + " ne obstaja");
                return ResponseEntity.status(404).body(error);
            }

            Dijak d = dijakOpt.get();
            Map<String, Object> result = new HashMap<>();

            // Osnovni podatki
            result.put("id", d.getId());
            result.put("ime", d.getIme());
            result.put("priimek", d.getPriimek());
            result.put("emso", d.getEmso());

            if (d.getTelefonska() != null && !d.getTelefonska().isEmpty()) {
                result.put("telefonska", d.getTelefonska());
            }

            if (d.getDatumRojstva() != null) {
                result.put("datumRojstva", d.getDatumRojstva().toString());
            }

            // Razred - samo ime razreda kot string
            if (d.getRazred() != null) {
                result.put("razred", d.getRazred().getImeRazreda());
            } else {
                result.put("razred", "Ni razreda");
            }

            // Predmeti in ocene - združeni po predmetih
            Map<String, List<Integer>> predmetiOceneMap = new HashMap<>();

            if (d.getDijakPredmeti() != null && !d.getDijakPredmeti().isEmpty()) {
                for (var dp : d.getDijakPredmeti()) {
                    if (dp != null && dp.getPredmet() != null && dp.getPredmet().getIme() != null) {
                        String predmetIme = dp.getPredmet().getIme();

                        // Inicializiraj seznam ocen za ta predmet, če še ne obstaja
                        if (!predmetiOceneMap.containsKey(predmetIme)) {
                            predmetiOceneMap.put(predmetIme, new ArrayList<>());
                        }

                        // Dodaj vse ocene za ta predmet
                        if (dp.getOcene() != null && !dp.getOcene().isEmpty()) {
                            for (var ocena : dp.getOcene()) {
                                if (ocena != null && ocena.getOcena() != null) {
                                    predmetiOceneMap.get(predmetIme).add(ocena.getOcena());
                                }
                            }
                        }
                    }
                }
            }

            // Pretvori mapo v seznam za frontend
            List<Map<String, Object>> predmetiList = new ArrayList<>();

            for (Map.Entry<String, List<Integer>> entry : predmetiOceneMap.entrySet()) {
                Map<String, Object> predmetMap = new HashMap<>();
                predmetMap.put("predmet", entry.getKey());

                if (entry.getValue().isEmpty()) {
                    predmetMap.put("ocene", "Ni ocen");
                } else {
                    predmetMap.put("ocene", entry.getValue());
                }

                predmetiList.add(predmetMap);
            }

            result.put("predmeti", predmetiList);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Napaka pri pridobivanju podatkov o dijak: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 3. POST - ustvari novega dijaka
    @PostMapping("")
    public ResponseEntity<?> createDijak(@RequestBody Map<String, Object> dijakData) {
        try {
            // Validacija zahtevanih podatkov
            if (!dijakData.containsKey("ime") || !dijakData.containsKey("priimek") || !dijakData.containsKey("emso")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Manjkajo zahtevani podatki (ime, priimek, emso)");
                return ResponseEntity.badRequest().body(error);
            }

            Dijak dijak = new Dijak();
            dijak.setIme(dijakData.get("ime").toString());
            dijak.setPriimek(dijakData.get("priimek").toString());
            dijak.setEmso(dijakData.get("emso").toString());

            // Opcijski podatki
            if (dijakData.containsKey("telefonska") && dijakData.get("telefonska") != null) {
                dijak.setTelefonska(dijakData.get("telefonska").toString());
            }

            if (dijakData.containsKey("datumRojstva") && dijakData.get("datumRojstva") != null) {
                try {
                    dijak.setDatumRojstva(LocalDate.parse(dijakData.get("datumRojstva").toString()));
                } catch (Exception e) {
                    // Ignore date parsing errors
                }
            }

            // Razred (če je podan)
            if (dijakData.containsKey("razredId") && dijakData.get("razredId") != null) {
                try {
                    Long razredId = null;
                    Object razredIdObj = dijakData.get("razredId");

                    if (razredIdObj instanceof Integer) {
                        razredId = ((Integer) razredIdObj).longValue();
                    } else if (razredIdObj instanceof Long) {
                        razredId = (Long) razredIdObj;
                    } else if (razredIdObj instanceof String) {
                        razredId = Long.parseLong((String) razredIdObj);
                    }

                    if (razredId != null) {
                        Optional<Razred> razredOpt = razredRepository.findById(razredId);
                        if (razredOpt.isPresent()) {
                            dijak.setRazred(razredOpt.get());
                        }
                    }
                } catch (Exception e) {
                    // Ignore razred parsing errors
                }
            }

            Dijak savedDijak = dijakRepository.save(dijak);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Dijak uspešno ustvarjen");
            response.put("id", savedDijak.getId());
            response.put("ime", savedDijak.getIme());
            response.put("priimek", savedDijak.getPriimek());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Napaka pri ustvarjanju dijaka: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 4. PUT - posodobi dijaka
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDijak(@PathVariable Long id, @RequestBody Map<String, Object> dijakData) {
        try {
            Optional<Dijak> dijakOpt = dijakRepository.findById(id);

            if (dijakOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Dijak z ID " + id + " ne obstaja");
                return ResponseEntity.status(404).body(error);
            }

            Dijak dijak = dijakOpt.get();

            // Posodobi podatke (če so podani)
            if (dijakData.containsKey("ime")) {
                dijak.setIme(dijakData.get("ime").toString());
            }

            if (dijakData.containsKey("priimek")) {
                dijak.setPriimek(dijakData.get("priimek").toString());
            }

            if (dijakData.containsKey("emso")) {
                dijak.setEmso(dijakData.get("emso").toString());
            }

            if (dijakData.containsKey("telefonska")) {
                dijak.setTelefonska(dijakData.get("telefonska").toString());
            }

            if (dijakData.containsKey("datumRojstva") && dijakData.get("datumRojstva") != null) {
                try {
                    dijak.setDatumRojstva(LocalDate.parse(dijakData.get("datumRojstva").toString()));
                } catch (Exception e) {
                    // Ignore date parsing errors
                }
            }

            // Razred (če je podan)
            if (dijakData.containsKey("razredId") && dijakData.get("razredId") != null) {
                try {
                    Long razredId = null;
                    Object razredIdObj = dijakData.get("razredId");

                    if (razredIdObj instanceof Integer) {
                        razredId = ((Integer) razredIdObj).longValue();
                    } else if (razredIdObj instanceof Long) {
                        razredId = (Long) razredIdObj;
                    } else if (razredIdObj instanceof String) {
                        razredId = Long.parseLong((String) razredIdObj);
                    }

                    if (razredId != null) {
                        Optional<Razred> razredOpt = razredRepository.findById(razredId);
                        if (razredOpt.isPresent()) {
                            dijak.setRazred(razredOpt.get());
                        } else {
                            // Če razred ne obstaja, nastavi na null
                            dijak.setRazred(null);
                        }
                    }
                } catch (Exception e) {
                    // Ignore razred parsing errors
                }
            }

            dijakRepository.save(dijak);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Dijak uspešno posodobljen");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Napaka pri posodabljanju dijaka: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 5. DELETE - izbriši dijaka
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDijak(@PathVariable Long id) {
        try {
            if (!dijakRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Dijak z ID " + id + " ne obstaja");
                return ResponseEntity.status(404).body(error);
            }

            dijakRepository.deleteById(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Dijak uspešno izbrisan");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Napaka pri brisanju dijaka: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 6. GET - preveri zdravje API-ja
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", new Date().toString());
        response.put("service", "Dijak Management API");
        return ResponseEntity.ok(response);
    }
}