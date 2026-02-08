package com.example.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ocene")
public class Ocena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer ocena;

    @ManyToOne
    @JoinColumn(name = "dijak_predmet_id")
    private DijakPredmet dijakPredmet;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getOcena() { return ocena; }
    public void setOcena(Integer ocena) { this.ocena = ocena; }

    public DijakPredmet getDijakPredmet() { return dijakPredmet; }
    public void setDijakPredmet(DijakPredmet dijakPredmet) { this.dijakPredmet = dijakPredmet; }
}