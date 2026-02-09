package com.example.backend.controller;

import com.example.backend.entity.Dijak;
import com.example.backend.repository.DijakRepository;
import com.example.backend.repository.OcenaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/dijaki")
public class DijakController {

    @Autowired
    private DijakRepository dijakRepository;

    @Autowired
    private OcenaRepository ocenaRepository;

    // GET /dijaki - vrne vse dijake
    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> getAllDijaki() {
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
            } else {
                dijakMap.put("razred", "Ni razreda");
            }

            result.add(dijakMap);
        }

        return ResponseEntity.ok(result);
    }

    // GET /dijaki/{id}/info - popravljena, poenostavljena
    @GetMapping("/{id}/info")
    public ResponseEntity<?> getDijakInfo(@PathVariable Long id) {
        try {
            Optional<Dijak> dijakOpt = dijakRepository.findById(id);

            if (dijakOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Dijak d = dijakOpt.get();
            Map<String, Object> result = new HashMap<>();

            // Osnovni podatki
            result.put("id", d.getId());
            result.put("ime", d.getIme());
            result.put("priimek", d.getPriimek());
            result.put("emso", d.getEmso());
            result.put("telefonska", d.getTelefonska());
            result.put("datumRojstva", d.getDatumRojstva());

            if (d.getRazred() != null) {
                result.put("razred", d.getRazred().getImeRazreda());
            } else {
                result.put("razred", "Ni razreda");
            }

            // Pridobi ocene direktno iz OcenaRepository
            List<Map<String, Object>> predmetiList = new ArrayList<>();
            Map<String, List<Integer>> ocenePoPredmetih = new HashMap<>();

            // Pridobi vse ocene za tega dijaka
            List<Object[]> oceneResult = ocenaRepository.findOceneWithPredmetByDijakId(id);

            // Združi ocene po predmetih
            for (Object[] row : oceneResult) {
                String predmetIme = (String) row[0];
                Integer ocena = (Integer) row[1];

                if (!ocenePoPredmetih.containsKey(predmetIme)) {
                    ocenePoPredmetih.put(predmetIme, new ArrayList<>());
                }
                ocenePoPredmetih.get(predmetIme).add(ocena);
            }

            // Ustavi seznam predmetov
            for (Map.Entry<String, List<Integer>> entry : ocenePoPredmetih.entrySet()) {
                Map<String, Object> predmetMap = new HashMap<>();
                predmetMap.put("predmet", entry.getKey());
                predmetMap.put("ocene", entry.getValue());
                predmetiList.add(predmetMap);
            }

            result.put("predmeti", predmetiList);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Napaka: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // OSTALE METODE (POST, PUT, DELETE) - ohrani kot so
    @PostMapping("")
    public ResponseEntity<?> createDijak(@RequestBody Map<String, Object> dijakData) {
        try {
            Dijak dijak = new Dijak();
            dijak.setIme((String) dijakData.get("ime"));
            dijak.setPriimek((String) dijakData.get("priimek"));
            dijak.setEmso((String) dijakData.get("emso"));

            if (dijakData.containsKey("telefonska")) {
                dijak.setTelefonska((String) dijakData.get("telefonska"));
            }

            if (dijakData.containsKey("datumRojstva") && dijakData.get("datumRojstva") != null) {
                dijak.setDatumRojstva(java.time.LocalDate.parse((String) dijakData.get("datumRojstva")));
            }

            Dijak savedDijak = dijakRepository.save(dijak);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Dijak uspešno ustvarjen");
            response.put("id", savedDijak.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Napaka pri ustvarjanju dijaka: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDijak(@PathVariable Long id, @RequestBody Map<String, Object> dijakData) {
        Optional<Dijak> dijakOpt = dijakRepository.findById(id);

        if (dijakOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dijak dijak = dijakOpt.get();

        if (dijakData.containsKey("ime")) {
            dijak.setIme((String) dijakData.get("ime"));
        }
        if (dijakData.containsKey("priimek")) {
            dijak.setPriimek((String) dijakData.get("priimek"));
        }
        if (dijakData.containsKey("emso")) {
            dijak.setEmso((String) dijakData.get("emso"));
        }
        if (dijakData.containsKey("telefonska")) {
            dijak.setTelefonska((String) dijakData.get("telefonska"));
        }
        if (dijakData.containsKey("datumRojstva") && dijakData.get("datumRojstva") != null) {
            dijak.setDatumRojstva(java.time.LocalDate.parse((String) dijakData.get("datumRojstva")));
        }

        dijakRepository.save(dijak);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Dijak uspešno posodobljen");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDijak(@PathVariable Long id) {
        if (!dijakRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        dijakRepository.deleteById(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Dijak uspešno izbrisan");

        return ResponseEntity.ok(response);
    }
}