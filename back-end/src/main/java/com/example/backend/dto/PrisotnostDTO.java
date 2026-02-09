package com.example.backend.dto;

import java.time.LocalDate;

public class PrisotnostDTO {
    private Long id;
    private Double prisotnost;
    private Long dijakPredmetId;
    private Long dijakId;
    private String predmetIme;
    private LocalDate datum;
    private String prisotnostProcent;
    private String prisotnostOpis;

    // Konstruktorji
    public PrisotnostDTO() {}

    public PrisotnostDTO(Long id, Double prisotnost, Long dijakPredmetId,
                         Long dijakId, String predmetIme, LocalDate datum) {
        this.id = id;
        this.prisotnost = prisotnost;
        this.dijakPredmetId = dijakPredmetId;
        this.dijakId = dijakId;
        this.predmetIme = predmetIme;
        this.datum = datum;
        this.prisotnostProcent = formatProcent(prisotnost);
        this.prisotnostOpis = formatOpis(prisotnost);
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getPrisotnost() { return prisotnost; }
    public void setPrisotnost(Double prisotnost) {
        this.prisotnost = prisotnost;
        this.prisotnostProcent = formatProcent(prisotnost);
        this.prisotnostOpis = formatOpis(prisotnost);
    }

    public Long getDijakPredmetId() { return dijakPredmetId; }
    public void setDijakPredmetId(Long dijakPredmetId) { this.dijakPredmetId = dijakPredmetId; }

    public Long getDijakId() { return dijakId; }
    public void setDijakId(Long dijakId) { this.dijakId = dijakId; }

    public String getPredmetIme() { return predmetIme; }
    public void setPredmetIme(String predmetIme) { this.predmetIme = predmetIme; }

    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }

    public String getPrisotnostProcent() { return prisotnostProcent; }
    public String getPrisotnostOpis() { return prisotnostOpis; }

    // Pomožne metode
    private String formatProcent(Double prisotnost) {
        if (prisotnost != null) {
            return String.format("%.0f%%", prisotnost * 100);
        }
        return "0%";
    }

    private String formatOpis(Double prisotnost) {
        if (prisotnost == null) return "Ni podatka";
        if (prisotnost >= 0.9) return "Prisoten";
        if (prisotnost >= 0.5) return "Delno prisoten";
        if (prisotnost > 0.0) return "Zamujen";
        return "Odsoten";
    }
}