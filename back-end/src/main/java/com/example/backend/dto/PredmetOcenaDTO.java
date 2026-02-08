package com.example.backend.dto;

import java.util.List;

public class PredmetOcenaDTO {
    private String predmetIme;
    private List<Integer> ocene;

    // Getters and Setters
    public String getPredmetIme() { return predmetIme; }
    public void setPredmetIme(String predmetIme) { this.predmetIme = predmetIme; }

    public List<Integer> getOcene() { return ocene; }
    public void setOcene(List<Integer> ocene) { this.ocene = ocene; }
}
