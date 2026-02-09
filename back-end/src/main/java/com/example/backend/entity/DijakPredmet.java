package com.example.backend.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "dijak_predmet") // ALI "relationship6" - preveri ime tabele!
public class DijakPredmet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dijak_id") // ALI "dijak_id" - preveri ime stolpca!
    private Dijak dijak;

    @ManyToOne
    @JoinColumn(name = "predmeti_id")
    private Predmet predmet;

    @OneToMany(mappedBy = "dijakPredmet", fetch = FetchType.LAZY)
    private List<Ocena> ocene;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Dijak getDijak() { return dijak; }
    public void setDijak(Dijak dijak) { this.dijak = dijak; }

    public Predmet getPredmet() { return predmet; }
    public void setPredmet(Predmet predmet) { this.predmet = predmet; }

    public List<Ocena> getOcene() { return ocene; }
    public void setOcene(List<Ocena> ocene) { this.ocene = ocene; }
}