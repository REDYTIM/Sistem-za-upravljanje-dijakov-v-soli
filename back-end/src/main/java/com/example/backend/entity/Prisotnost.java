package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "prisotnost")
public class Prisotnost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prisotnost")
    private Double prisotnost;  // REAL v PostgreSQL = Double v Javi

    @Column(name = "dijak_predmet_id")
    private Long dijakPredmetId;

    @Column(name = "datum")
    private LocalDate datum;

    // Opcijsko: če želiš relacijo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dijak_predmet_id", insertable = false, updatable = false)
    private DijakPredmet dijakPredmet;

    // Konstruktorji
    public Prisotnost() {
        this.datum = LocalDate.now();
    }

    public Prisotnost(Double prisotnost, Long dijakPredmetId) {
        this.prisotnost = prisotnost;
        this.dijakPredmetId = dijakPredmetId;
        this.datum = LocalDate.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getPrisotnost() { return prisotnost; }
    public void setPrisotnost(Double prisotnost) {
        if (prisotnost != null && prisotnost >= 0.0 && prisotnost <= 1.0) {
            this.prisotnost = prisotnost;
        } else {
            throw new IllegalArgumentException("Prisotnost mora biti med 0.0 in 1.0");
        }
    }

    public Long getDijakPredmetId() { return dijakPredmetId; }
    public void setDijakPredmetId(Long dijakPredmetId) { this.dijakPredmetId = dijakPredmetId; }

    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }

    public DijakPredmet getDijakPredmet() { return dijakPredmet; }
    public void setDijakPredmet(DijakPredmet dijakPredmet) { this.dijakPredmet = dijakPredmet; }

    // Pomožne metode
    public String getPrisotnostProcent() {
        if (prisotnost != null) {
            return String.format("%.0f%%", prisotnost * 100);
        }
        return "0%";
    }

    public String getPrisotnostOpis() {
        if (prisotnost == null) return "Ni podatka";
        if (prisotnost >= 0.9) return "Prisoten";
        if (prisotnost >= 0.5) return "Delno prisoten";
        if (prisotnost > 0.0) return "Zamujen";
        return "Odsoten";
    }
}