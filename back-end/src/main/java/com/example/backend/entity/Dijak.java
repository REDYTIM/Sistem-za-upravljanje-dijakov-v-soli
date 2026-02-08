package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
public class Dijak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ime;
    private String priimek;
    private String emso;
    private String telefonska;
    private LocalDate datumRojstva;

    @ManyToOne
    @JoinColumn(name = "razred_id")
    private Razred razred;

    @OneToMany(mappedBy = "dijak", fetch = FetchType.LAZY)
    private List<DijakPredmet> dijakPredmeti;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIme() { return ime; }
    public void setIme(String ime) { this.ime = ime; }

    public String getPriimek() { return priimek; }
    public void setPriimek(String priimek) { this.priimek = priimek; }

    public String getEmso() { return emso; }
    public void setEmso(String emso) { this.emso = emso; }

    public String getTelefonska() { return telefonska; }
    public void setTelefonska(String telefonska) { this.telefonska = telefonska; }

    public LocalDate getDatumRojstva() { return datumRojstva; }
    public void setDatumRojstva(LocalDate datumRojstva) { this.datumRojstva = datumRojstva; }

    public Razred getRazred() { return razred; }
    public void setRazred(Razred razred) { this.razred = razred; }

    public List<DijakPredmet> getDijakPredmeti() { return dijakPredmeti; }
    public void setDijakPredmeti(List<DijakPredmet> dijakPredmeti) { this.dijakPredmeti = dijakPredmeti; }
}