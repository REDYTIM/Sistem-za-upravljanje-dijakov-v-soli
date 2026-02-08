package com.example.backend.controller;

import com.example.backend.entity.DijakPredmet;
import com.example.backend.entity.Ocena;
import com.example.backend.repository.DijakPredmetRepository;
import com.example.backend.repository.OcenaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/ocene")
@CrossOrigin(origins = "*")
public class OcenaController {

    @Autowired
    private OcenaRepository ocenaRepository;

    @Autowired
    private DijakPredmetRepository dijakPredmetRepository;

    public OcenaController(OcenaRepository ocenaRepository,
                           DijakPredmetRepository dijakPredmetRepository) {
        this.ocenaRepository = ocenaRepository;
        this.dijakPredmetRepository = dijakPredmetRepository;
    }

    // 1. GET vseh ocen za dijaka in predmet (UPORABITE VAŠO METODO)
    @GetMapping("/dijak/{dijakId}/predmet/{predmetIme}")
    public ResponseEntity<?> getOceneForDijakAndPredmet(
            @PathVariable Long dijakId,
            @PathVariable String predmetIme) {

        try {
            // Uporabite vašo obstoječo metodo
            List<DijakPredmet> povezave = dijakPredmetRepository.findByDijakIdWithDetails(dijakId);

            // Filtriraj po imenu predmeta in zberi vse ocene
            List<Map<String, Object>> oceneList = new ArrayList<>();

            for (DijakPredmet dp : povezave) {
                if (dp.getPredmet() != null &&
                        dp.getPredmet().getIme() != null &&
                        dp.getPredmet().getIme().equalsIgnoreCase(predmetIme)) {

                    // Dodaj vse ocene za to povezavo
                    if (dp.getOcene() != null && !dp.getOcene().isEmpty()) {
                        for (Ocena ocena : dp.getOcene()) {
                            Map<String, Object> ocenaMap = new HashMap<>();
                            ocenaMap.put("id", ocena.getId());
                            ocenaMap.put("ocena", ocena.getOcena());
                            ocenaMap.put("dijakPredmetId", dp.getId());
                            oceneList.add(ocenaMap);
                        }
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("dijakId", dijakId);
            response.put("predmet", predmetIme);
            response.put("ocene", oceneList);
            response.put("stOcen", oceneList.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Napaka pri pridobivanju ocen: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 2. GET vseh predmetov za dijaka (UPORABITE VAŠO METODO)
    @GetMapping("/dijak/{dijakId}/predmeti")
    public ResponseEntity<?> getPredmetiForDijak(@PathVariable Long dijakId) {
        try {
            List<DijakPredmet> povezave = dijakPredmetRepository.findByDijakIdWithDetails(dijakId);

            List<Map<String, Object>> predmetiList = new ArrayList<>();
            Set<String> uniquePredmeti = new HashSet<>();

            for (DijakPredmet dp : povezave) {
                if (dp.getPredmet() != null && dp.getPredmet().getIme() != null) {
                    String predmetIme = dp.getPredmet().getIme();

                    // Dodaj samo unikatne predmete
                    if (!uniquePredmeti.contains(predmetIme)) {
                        uniquePredmeti.add(predmetIme);

                        Map<String, Object> predmetMap = new HashMap<>();
                        predmetMap.put("ime", predmetIme);
                        predmetMap.put("dijakPredmetId", dp.getId());

                        // Dodaj tudi ocene za ta predmet
                        if (dp.getOcene() != null && !dp.getOcene().isEmpty()) {
                            List<Integer> ocene = new ArrayList<>();
                            for (Ocena ocena : dp.getOcene()) {
                                ocene.add(ocena.getOcena());
                            }
                            predmetMap.put("trenutneOcene", ocene);
                        }

                        predmetiList.add(predmetMap);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("dijakId", dijakId);
            response.put("predmeti", predmetiList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Napaka pri pridobivanju predmetov: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 3. POST - dodaj novo oceno (VAŠA OBSTOJEČA METODA - POPRAVLJENA)
    @PostMapping("")
    public ResponseEntity<?> addOcena(
            @RequestParam(required = false) Long dijakPredmetId,
            @RequestParam(required = false) Integer ocena,
            @RequestBody(required = false) Map<String, Object> ocenaData) {

        try {
            Long dpId;
            Integer ocenaValue;

            // Podpora za obe obliki: params ali JSON body
            if (dijakPredmetId != null && ocena != null) {
                dpId = dijakPredmetId;
                ocenaValue = ocena;
            } else if (ocenaData != null && ocenaData.containsKey("dijakPredmetId") && ocenaData.containsKey("ocena")) {
                dpId = Long.parseLong(ocenaData.get("dijakPredmetId").toString());
                ocenaValue = Integer.parseInt(ocenaData.get("ocena").toString());
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Manjkajo zahtevani podatki (dijakPredmetId, ocena)");
                return ResponseEntity.badRequest().body(error);
            }

            // Preveri, če je ocena veljavna
            if (ocenaValue < 1 || ocenaValue > 5) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Ocena mora biti med 1 in 5");
                return ResponseEntity.badRequest().body(error);
            }

            DijakPredmet dp = dijakPredmetRepository.findById(dpId)
                    .orElseThrow(() -> new RuntimeException("DijakPredmet ne obstaja"));

            // Preveri, če dijakPredmet pripada pravemu dijaku (za varnost)
            if (ocenaData != null && ocenaData.containsKey("dijakId")) {
                Long expectedDijakId = Long.parseLong(ocenaData.get("dijakId").toString());
                if (!dp.getDijak().getId().equals(expectedDijakId)) {
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "DijakPredmet ne pripada temu dijaku");
                    return ResponseEntity.badRequest().body(error);
                }
            }

            Ocena o = new Ocena();
            o.setOcena(ocenaValue);
            o.setDijakPredmet(dp);

            Ocena savedOcena = ocenaRepository.save(o);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Ocena uspešno dodana");
            response.put("id", savedOcena.getId());
            response.put("ocena", savedOcena.getOcena());
            response.put("predmet", dp.getPredmet() != null ? dp.getPredmet().getIme() : "Neznano");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Napaka pri dodajanju ocene: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 4. DELETE - izbriši oceno
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOcena(@PathVariable Long id) {
        try {
            if (!ocenaRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Ocena z ID " + id + " ne obstaja");
                return ResponseEntity.status(404).body(error);
            }

            ocenaRepository.deleteById(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Ocena uspešno izbrisana");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Napaka pri brisanju ocene: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 5. GET - zdravje endpoint
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Ocena Management API");
        response.put("timestamp", new Date().toString());
        return ResponseEntity.ok(response);
    }

    // 6. GET - najdi dijakPredmetId za dijaka in predmet
    @GetMapping("/dijak/{dijakId}/predmet/{predmetIme}/povezava")
    public ResponseEntity<?> getDijakPredmetId(
            @PathVariable Long dijakId,
            @PathVariable String predmetIme) {

        try {
            List<DijakPredmet> povezave = dijakPredmetRepository.findByDijakIdWithDetails(dijakId);

            for (DijakPredmet dp : povezave) {
                if (dp.getPredmet() != null &&
                        dp.getPredmet().getIme() != null &&
                        dp.getPredmet().getIme().equalsIgnoreCase(predmetIme)) {

                    Map<String, Object> response = new HashMap<>();
                    response.put("dijakPredmetId", dp.getId());
                    response.put("dijakId", dijakId);
                    response.put("predmet", predmetIme);
                    response.put("predmetId", dp.getPredmet().getId());

                    return ResponseEntity.ok(response);
                }
            }

            Map<String, String> error = new HashMap<>();
            error.put("message", "Dijak ni vpisan na predmet: " + predmetIme);
            return ResponseEntity.status(404).body(error);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Napaka pri iskanju povezave: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}