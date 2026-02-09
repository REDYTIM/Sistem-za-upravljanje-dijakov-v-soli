package com.example.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ocene") // ALI "relationship12" - preveri ime tabele!
public class Ocena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ocena")
    private Integer ocena;

    @ManyToOne
    @JoinColumn(name = "dijak_predmet_id") // ALI "dijak_predmet_id" - preveri ime stolpca!
    private DijakPredmet dijakPredmet;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getOcena() { return ocena; }
    public void setOcena(Integer ocena) { this.ocena = ocena; }

    public DijakPredmet getDijakPredmet() { return dijakPredmet; }
    public void setDijakPredmet(DijakPredmet dijakPredmet) { this.dijakPredmet = dijakPredmet; }
}