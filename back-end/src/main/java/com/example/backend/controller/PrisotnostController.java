package com.example.backend.controller;

import com.example.backend.dto.PrisotnostDTO;
import com.example.backend.entity.Prisotnost;
import com.example.backend.repository.DijakPredmetRepository;
import com.example.backend.repository.PrisotnostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/prisotnost")
@CrossOrigin(origins = "*")
public class PrisotnostController {

    @Autowired
    private PrisotnostRepository prisotnostRepository;

    @Autowired
    private DijakPredmetRepository dijakPredmetRepository;

    // 1. GET vseh prisotnosti za dijaka in predmet
    @GetMapping("/dijak/{dijakId}/predmet/{predmetIme}")
    public ResponseEntity<?> getPrisotnostForDijakAndPredmet(
            @PathVariable Long dijakId,
            @PathVariable String predmetIme) {

        try {
            System.out.println("DEBUG Backend: Pridobivam prisotnost za dijakId=" + dijakId + ", predmet=" + predmetIme);

            List<Prisotnost> prisotnosti = prisotnostRepository.findByDijakIdAndPredmetIme(dijakId, predmetIme);
            List<PrisotnostDTO> prisotnostiDTO = new ArrayList<>();

            for (Prisotnost p : prisotnosti) {
                PrisotnostDTO dto = new PrisotnostDTO(
                        p.getId(),
                        p.getPrisotnost(),
                        p.getDijakPredmetId(),
                        dijakId,
                        predmetIme,
                        p.getDatum()
                );
                prisotnostiDTO.add(dto);
            }

            // Izračunaj povprečje
            Double povprecje = prisotnostRepository.findPovprecnaPrisotnost(dijakId, predmetIme);

            Map<String, Object> response = new HashMap<>();
            response.put("dijakId", dijakId);
            response.put("predmet", predmetIme);
            response.put("prisotnosti", prisotnostiDTO);
            response.put("stVnosov", prisotnostiDTO.size());
            response.put("povprecje", povprecje != null ? povprecje : 0.0);
            response.put("povprecjeProcent", povprecje != null ?
                    String.format("%.0f%%", povprecje * 100) : "0%");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Napaka pri pridobivanju prisotnosti: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 2. GET povprečne prisotnosti za dijaka
    @GetMapping("/dijak/{dijakId}/povprecje")
    public ResponseEntity<?> getPovprecnaPrisotnostForDijak(@PathVariable Long dijakId) {
        try {
            List<Map<String, Object>> result = new ArrayList<>();

            // Pridobi vse predmete za dijaka
            var dijakPredmeti = dijakPredmetRepository.findByDijakIdWithPredmet(dijakId);

            for (var dp : dijakPredmeti) {
                if (dp.getPredmet() != null) {
                    String predmetIme = dp.getPredmet().getIme();
                    Double povprecje = prisotnostRepository.findPovprecnaPrisotnost(dijakId, predmetIme);

                    Map<String, Object> predmetMap = new HashMap<>();
                    predmetMap.put("predmet", predmetIme);
                    predmetMap.put("povprecje", povprecje != null ? povprecje : 0.0);
                    predmetMap.put("povprecjeProcent", povprecje != null ?
                            String.format("%.0f%%", povprecje * 100) : "0%");

                    result.add(predmetMap);
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka pri pridobivanju povprečja: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 3. POST - dodaj novo prisotnost
    @PostMapping("/dodaj")
    public ResponseEntity<?> addPrisotnost(@RequestBody Map<String, Object> prisotnostData) {
        try {
            System.out.println("DEBUG Backend: Dodajam prisotnost: " + prisotnostData);

            // Preveri zahtevane podatke
            if (!prisotnostData.containsKey("dijakPredmetId") || !prisotnostData.containsKey("prisotnost")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Manjkajo zahtevani podatki (dijakPredmetId, prisotnost)");
                return ResponseEntity.badRequest().body(error);
            }

            Long dpId;
            Double prisotnostValue;

            try {
                dpId = Long.parseLong(prisotnostData.get("dijakPredmetId").toString());
                prisotnostValue = Double.parseDouble(prisotnostData.get("prisotnost").toString());
            } catch (NumberFormatException e) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Neveljavna oblika podatkov");
                return ResponseEntity.badRequest().body(error);
            }

            // Preveri veljavnost prisotnosti (0.0 - 1.0)
            if (prisotnostValue < 0.0 || prisotnostValue > 1.0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Prisotnost mora biti med 0.0 in 1.0");
                return ResponseEntity.badRequest().body(error);
            }

            // Preveri ali dijakPredmet obstaja
            var dp = dijakPredmetRepository.findById(dpId)
                    .orElseThrow(() -> new RuntimeException("DijakPredmet z ID " + dpId + " ne obstaja"));

            // Ustvari datum (če ni podan, uporabi današnji)
            LocalDate datum = LocalDate.now();
            if (prisotnostData.containsKey("datum") && prisotnostData.get("datum") != null) {
                try {
                    datum = LocalDate.parse(prisotnostData.get("datum").toString());
                } catch (Exception e) {
                    System.out.println("DEBUG Backend: Neveljaven format datuma, uporabljam današnji datum");
                }
            }

            // Preveri ali že obstaja vnos za ta datum
            if (prisotnostRepository.existsByDijakPredmetIdAndDatum(dpId, datum)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Prisotnost za ta datum že obstaja");
                return ResponseEntity.badRequest().body(error);
            }

            // Ustvari novo prisotnost
            Prisotnost p = new Prisotnost();
            p.setPrisotnost(prisotnostValue);
            p.setDijakPredmetId(dpId);
            p.setDatum(datum);

            Prisotnost savedPrisotnost = prisotnostRepository.save(p);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Prisotnost uspešno dodana");
            response.put("id", savedPrisotnost.getId());
            response.put("prisotnost", savedPrisotnost.getPrisotnost());
            response.put("prisotnostProcent", savedPrisotnost.getPrisotnostProcent());
            response.put("datum", savedPrisotnost.getDatum());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka pri dodajanju prisotnosti: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 4. PUT - posodobi prisotnost
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePrisotnost(@PathVariable Long id,
                                              @RequestBody Map<String, Object> prisotnostData) {
        try {
            System.out.println("DEBUG Backend: Posodabljam prisotnost ID=" + id);

            if (!prisotnostData.containsKey("prisotnost")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Manjka nova vrednost prisotnosti");
                return ResponseEntity.badRequest().body(error);
            }

            Double novaPrisotnost = Double.parseDouble(prisotnostData.get("prisotnost").toString());

            if (novaPrisotnost < 0.0 || novaPrisotnost > 1.0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Prisotnost mora biti med 0.0 in 1.0");
                return ResponseEntity.badRequest().body(error);
            }

            Prisotnost prisotnost = prisotnostRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Prisotnost z ID " + id + " ne obstaja"));

            prisotnost.setPrisotnost(novaPrisotnost);

            // Posodobi datum, če je podan
            if (prisotnostData.containsKey("datum") && prisotnostData.get("datum") != null) {
                try {
                    LocalDate datum = LocalDate.parse(prisotnostData.get("datum").toString());
                    prisotnost.setDatum(datum);
                } catch (Exception e) {
                    // Ignoriraj neveljaven datum
                }
            }

            prisotnostRepository.save(prisotnost);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Prisotnost uspešno posodobljena");
            response.put("id", prisotnost.getId());
            response.put("prisotnost", prisotnost.getPrisotnost());
            response.put("prisotnostProcent", prisotnost.getPrisotnostProcent());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka pri posodabljanju prisotnosti: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 5. DELETE - izbriši prisotnost
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePrisotnost(@PathVariable Long id) {
        try {
            if (!prisotnostRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Prisotnost z ID " + id + " ne obstaja");
                return ResponseEntity.status(404).body(error);
            }

            prisotnostRepository.deleteById(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Prisotnost uspešno izbrisana");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka pri brisanju prisotnosti: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 6. GET prisotnosti za določen datum
    @GetMapping("/dijak/{dijakId}/datum/{datum}")
    public ResponseEntity<?> getPrisotnostByDate(@PathVariable Long dijakId,
                                                 @PathVariable String datum) {
        try {
            LocalDate date = LocalDate.parse(datum);
            List<Prisotnost> prisotnosti = prisotnostRepository.findByDijakIdAndDatum(dijakId, date);

            List<Map<String, Object>> result = new ArrayList<>();
            for (Prisotnost p : prisotnosti) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("prisotnost", p.getPrisotnost());
                map.put("prisotnostProcent", p.getPrisotnostProcent());
                map.put("datum", p.getDatum());

                // Pridobi informacije o predmetu
                if (p.getDijakPredmet() != null && p.getDijakPredmet().getPredmet() != null) {
                    map.put("predmet", p.getDijakPredmet().getPredmet().getIme());
                }

                result.add(map);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka pri pridobivanju prisotnosti: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 7. GET - zdravje endpoint
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Prisotnost Management API");
        response.put("timestamp", new Date().toString());
        return ResponseEntity.ok(response);
    }
}