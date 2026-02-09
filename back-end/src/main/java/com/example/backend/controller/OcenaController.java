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

    // 1. GET vseh ocen za dijaka in predmet - POPRAVLJENA
    @GetMapping("/dijak/{dijakId}/predmet/{predmetIme}")
    public ResponseEntity<?> getOceneForDijakAndPredmet(
            @PathVariable Long dijakId,
            @PathVariable String predmetIme) {

        try {
            System.out.println("DEBUG Backend: Pridobivam ocene za dijakId=" + dijakId + ", predmet=" + predmetIme);

            // Uporabi metodo iz repository-ja (mora biti definirana v OcenaRepository!)
            List<Ocena> ocene = ocenaRepository.findByDijakIdAndPredmetIme(dijakId, predmetIme);

            if (ocene == null) {
                ocene = new ArrayList<>();
            }

            List<Map<String, Object>> oceneList = new ArrayList<>();

            for (Ocena ocena : ocene) {
                Map<String, Object> ocenaMap = new HashMap<>();
                ocenaMap.put("id", ocena.getId());
                ocenaMap.put("ocena", ocena.getOcena());
                ocenaMap.put("dijakPredmetId", ocena.getDijakPredmet().getId());
                // ODSTRANI createdAt ker ga ni v entiteti
                // ocenaMap.put("createdAt", ocena.getCreatedAt());
                oceneList.add(ocenaMap);
            }

            System.out.println("DEBUG Backend: Skupno najdenih ocen: " + oceneList.size());

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

    // 2. GET vseh predmetov za dijaka - POPRAVLJENA
    @GetMapping("/dijak/{dijakId}/predmeti")
    public ResponseEntity<?> getPredmetiForDijak(@PathVariable Long dijakId) {
        try {
            System.out.println("DEBUG Backend: Pridobivam predmete za dijakId=" + dijakId);

            // Uporabi pravilno ime metode (mora biti v DijakPredmetRepository!)
            List<DijakPredmet> povezave = dijakPredmetRepository.findByDijakIdWithPredmet(dijakId);
            System.out.println("DEBUG Backend: Število povezav: " + (povezave != null ? povezave.size() : 0));

            List<Map<String, Object>> predmetiList = new ArrayList<>();
            Set<String> uniquePredmeti = new HashSet<>();

            if (povezave != null) {
                for (DijakPredmet dp : povezave) {
                    if (dp.getPredmet() != null && dp.getPredmet().getIme() != null) {
                        String predmetIme = dp.getPredmet().getIme();

                        // Dodaj samo unikatne predmete
                        if (!uniquePredmeti.contains(predmetIme)) {
                            uniquePredmeti.add(predmetIme);

                            Map<String, Object> predmetMap = new HashMap<>();
                            predmetMap.put("ime", predmetIme);
                            predmetMap.put("dijakPredmetId", dp.getId());

                            // Pridobi ocene za ta predmet
                            List<Integer> ocene = new ArrayList<>();
                            List<Ocena> oceneZaPredmet = ocenaRepository.findByDijakPredmetId(dp.getId());

                            if (oceneZaPredmet != null) {
                                for (Ocena ocena : oceneZaPredmet) {
                                    ocene.add(ocena.getOcena());
                                }
                            }

                            if (!ocene.isEmpty()) {
                                predmetMap.put("trenutneOcene", ocene);
                            }

                            predmetiList.add(predmetMap);
                        }
                    }
                }
            }

            System.out.println("DEBUG Backend: Skupno predmetov: " + predmetiList.size());

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

    // 3. POST - dodaj novo oceno
    @PostMapping("/dodaj")
    public ResponseEntity<?> addOcenaDodaj(@RequestBody Map<String, Object> ocenaData) {
        try {
            System.out.println("DEBUG Backend: Prejemanje ocene na /dodaj");
            System.out.println("DEBUG Backend: Podatki: " + ocenaData);

            if (!ocenaData.containsKey("dijakPredmetId") || !ocenaData.containsKey("ocena")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Manjkajo zahtevani podatki (dijakPredmetId, ocena)");
                return ResponseEntity.badRequest().body(error);
            }

            Long dpId;
            Integer ocenaValue;

            try {
                dpId = Long.parseLong(ocenaData.get("dijakPredmetId").toString());
                ocenaValue = Integer.parseInt(ocenaData.get("ocena").toString());
            } catch (NumberFormatException e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Neveljavna oblika podatkov");
                return ResponseEntity.badRequest().body(error);
            }

            if (ocenaValue < 1 || ocenaValue > 5) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Ocena mora biti med 1 in 5");
                return ResponseEntity.badRequest().body(error);
            }

            System.out.println("DEBUG Backend: Iščem DijakPredmet z ID: " + dpId);
            DijakPredmet dp = dijakPredmetRepository.findById(dpId)
                    .orElseThrow(() -> new RuntimeException("DijakPredmet z ID " + dpId + " ne obstaja"));

            System.out.println("DEBUG Backend: Najden DijakPredmet, dijakId=" + dp.getDijak().getId() +
                    ", predmet=" + (dp.getPredmet() != null ? dp.getPredmet().getIme() : "null"));

            if (ocenaData.containsKey("dijakId")) {
                try {
                    Long expectedDijakId = Long.parseLong(ocenaData.get("dijakId").toString());
                    if (!dp.getDijak().getId().equals(expectedDijakId)) {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "DijakPredmet ne pripaja temu dijaku");
                        return ResponseEntity.badRequest().body(error);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("DEBUG Backend: Neveljaven dijakId format");
                }
            }

            Ocena o = new Ocena();
            o.setOcena(ocenaValue);
            o.setDijakPredmet(dp);

            Ocena savedOcena = ocenaRepository.save(o);

            System.out.println("DEBUG Backend: Ocena shranjena z ID: " + savedOcena.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ocena uspešno dodana");
            response.put("id", savedOcena.getId());
            response.put("ocena", savedOcena.getOcena());
            response.put("predmet", dp.getPredmet() != null ? dp.getPredmet().getIme() : "Neznano");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka pri dodajanju ocene: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 4. PUT - posodobi oceno
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOcena(@PathVariable Long id, @RequestBody Map<String, Object> ocenaData) {
        try {
            System.out.println("DEBUG Backend: Posodabljam oceno ID=" + id);

            if (!ocenaData.containsKey("ocena")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Manjka nova ocena");
                return ResponseEntity.badRequest().body(error);
            }

            Integer novaOcena = Integer.parseInt(ocenaData.get("ocena").toString());

            if (novaOcena < 1 || novaOcena > 5) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Ocena mora biti med 1 in 5");
                return ResponseEntity.badRequest().body(error);
            }

            Ocena ocena = ocenaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Ocena z ID " + id + " ne obstaja"));

            ocena.setOcena(novaOcena);
            ocenaRepository.save(ocena);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ocena uspešno posodobljena");
            response.put("id", ocena.getId());
            response.put("ocena", ocena.getOcena());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka pri posodabljanju ocene: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 5. DELETE - izbriši oceno
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOcena(@PathVariable Long id) {
        try {
            if (!ocenaRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Ocena z ID " + id + " ne obstaja");
                return ResponseEntity.status(404).body(error);
            }

            ocenaRepository.deleteById(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Ocena uspešno izbrisana");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka pri brisanju ocene: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 6. GET - zdravje endpoint
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Ocena Management API");
        response.put("timestamp", new Date().toString());
        return ResponseEntity.ok(response);
    }

    // 7. GET - najdi dijakPredmetId za dijaka in predmet
    @GetMapping("/dijak/{dijakId}/predmet/{predmetIme}/povezava")
    public ResponseEntity<?> getDijakPredmetId(
            @PathVariable Long dijakId,
            @PathVariable String predmetIme) {

        try {
            List<DijakPredmet> povezave = dijakPredmetRepository.findByDijakIdWithPredmet(dijakId);

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
            error.put("error", "Dijak ni vpisan na predmet: " + predmetIme);
            return ResponseEntity.status(404).body(error);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka pri iskanju povezave: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 8. GET - testni endpoint za debug
    @GetMapping("/test/{dijakId}")
    public ResponseEntity<?> testDijakPredmet(@PathVariable Long dijakId) {
        try {
            List<DijakPredmet> povezave = dijakPredmetRepository.findByDijakIdWithPredmet(dijakId);

            List<Map<String, Object>> result = new ArrayList<>();

            for (DijakPredmet dp : povezave) {
                Map<String, Object> map = new HashMap<>();
                map.put("dijakPredmetId", dp.getId());
                map.put("dijakId", dp.getDijak().getId());
                map.put("predmet", dp.getPredmet() != null ? dp.getPredmet().getIme() : "null");

                // Pridobi ocene
                List<Ocena> ocene = ocenaRepository.findByDijakPredmetId(dp.getId());
                map.put("stOcen", ocene != null ? ocene.size() : 0);

                if (ocene != null && !ocene.isEmpty()) {
                    List<Integer> oceneList = new ArrayList<>();
                    for (Ocena o : ocene) {
                        oceneList.add(o.getOcena());
                    }
                    map.put("ocene", oceneList);
                }

                result.add(map);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka pri testiranju: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}