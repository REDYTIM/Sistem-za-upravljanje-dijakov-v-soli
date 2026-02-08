package com.example.backend.dto;

import java.util.List;

public class PredmetDTO {

    private Long id;
    private String ime;
    private List<Integer> ocene;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIme() { return ime; }
    public void setIme(String ime) { this.ime = ime; }

    public List<Integer> getOcene() { return ocene; }
    public void setOcene(List<Integer> ocene) { this.ocene = ocene; }
}
