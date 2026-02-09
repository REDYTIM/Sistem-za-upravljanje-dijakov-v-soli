package com.example.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "razredi")
public class Razred {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ime_razreda")
    private String imeRazreda;

    @Column(name = "letnik")
    private Integer letnik;

    @Column(name = "smer")
    private String smer;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImeRazreda() { return imeRazreda; }
    public void setImeRazreda(String imeRazreda) { this.imeRazreda = imeRazreda; }

    public Integer getLetnik() { return letnik; }
    public void setLetnik(Integer letnik) { this.letnik = letnik; }

    public String getSmer() { return smer; }
    public void setSmer(String smer) { this.smer = smer; }
}