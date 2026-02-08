package com.example.backend.controller;

import com.example.backend.entity.Predmet;
import com.example.backend.repository.PredmetRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predmeti")
public class PredmetController {

    private final PredmetRepository predmetRepository;

    public PredmetController(PredmetRepository predmetRepository) {
        this.predmetRepository = predmetRepository;
    }

    @GetMapping
    public List<Predmet> getAllPredmeti() {
        return predmetRepository.findAll();
    }

    @PostMapping
    public Predmet addPredmet(@RequestBody Predmet predmet) {
        return predmetRepository.save(predmet);
    }

    @PutMapping("/{id}")
    public Predmet updatePredmet(@PathVariable Long id, @RequestBody Predmet updated) {
        return predmetRepository.findById(id).map(p -> {
            p.setIme(updated.getIme());
            return predmetRepository.save(p);
        }).orElseThrow(() -> new RuntimeException("Predmet not found"));
    }

    @DeleteMapping("/{id}")
    public void deletePredmet(@PathVariable Long id) {
        predmetRepository.deleteById(id);
    }
}
