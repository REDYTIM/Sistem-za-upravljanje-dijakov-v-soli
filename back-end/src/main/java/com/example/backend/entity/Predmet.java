package com.example.backend.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "predmeti")
public class Predmet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ime;

    @OneToMany(mappedBy = "predmet")
    private List<DijakPredmet> dijakPredmeti;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIme() { return ime; }
    public void setIme(String ime) { this.ime = ime; }

    public List<DijakPredmet> getDijakPredmeti() { return dijakPredmeti; }
    public void setDijakPredmeti(List<DijakPredmet> dijakPredmeti) { this.dijakPredmeti = dijakPredmeti; }
}