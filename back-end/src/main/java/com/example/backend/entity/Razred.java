package com.example.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "razredi")
public class Razred {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imeRazreda;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImeRazreda() { return imeRazreda; }
    public void setImeRazreda(String imeRazreda) { this.imeRazreda = imeRazreda; }
}