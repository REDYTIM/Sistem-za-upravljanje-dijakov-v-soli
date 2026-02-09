package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dijak")
public class Dijak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ime")
    private String ime;

    @Column(name = "priimek")
    private String priimek;

    @Column(name = "emso")
    private String emso; // V bazi je Character varying(20)

    @Column(name = "datum_rojstva")
    private LocalDate datumRojstva;

    @Column(name = "telefonska")
    private String telefonska; // V bazi je Integer, ampak za telefon je bolje String

    @ManyToOne
    @JoinColumn(name = "razred_id")
    private Razred razred;

    @OneToMany(mappedBy = "dijak", fetch = FetchType.LAZY)
    private java.util.List<DijakPredmet> dijakPredmeti;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIme() { return ime; }
    public void setIme(String ime) { this.ime = ime; }

    public String getPriimek() { return priimek; }
    public void setPriimek(String priimek) { this.priimek = priimek; }

    public String getEmso() { return emso; }
    public void setEmso(String emso) { this.emso = emso; }

    public LocalDate getDatumRojstva() { return datumRojstva; }
    public void setDatumRojstva(LocalDate datumRojstva) { this.datumRojstva = datumRojstva; }

    public String getTelefonska() { return telefonska; }
    public void setTelefonska(String telefonska) { this.telefonska = telefonska; }

    public Razred getRazred() { return razred; }
    public void setRazred(Razred razred) { this.razred = razred; }

    public java.util.List<DijakPredmet> getDijakPredmeti() { return dijakPredmeti; }
    public void setDijakPredmeti(java.util.List<DijakPredmet> dijakPredmeti) { this.dijakPredmeti = dijakPredmeti; }
}