package com.example.backend.dto;

import java.util.List;

public class DijakDTO {

    private Long id;
    private String ime;
    private String priimek;
    private String emso;
    private String datumRojstva;

    private Long razredId;
    private String razredIme;

    private List<PredmetDTO> predmeti;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIme() { return ime; }
    public void setIme(String ime) { this.ime = ime; }

    public String getPriimek() { return priimek; }
    public void setPriimek(String priimek) { this.priimek = priimek; }

    public String getEmso() { return emso; }
    public void setEmso(String emso) { this.emso = emso; }

    public String getDatumRojstva() { return datumRojstva; }
    public void setDatumRojstva(String datumRojstva) { this.datumRojstva = datumRojstva; }

    public Long getRazredId() { return razredId; }
    public void setRazredId(Long razredId) { this.razredId = razredId; }

    public String getRazredIme() { return razredIme; }
    public void setRazredIme(String razredIme) { this.razredIme = razredIme; }

    public List<PredmetDTO> getPredmeti() { return predmeti; }
    public void setPredmeti(List<PredmetDTO> predmeti) { this.predmeti = predmeti; }
}
