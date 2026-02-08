package com.example.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "prisotnost")
public class Prisotnost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double prisotnost; // število neupravičenih ur

    @ManyToOne
    @JoinColumn(name = "dijak_predmet_id")
    private DijakPredmet dijakPredmet;

    // Getterji in setterji
}
