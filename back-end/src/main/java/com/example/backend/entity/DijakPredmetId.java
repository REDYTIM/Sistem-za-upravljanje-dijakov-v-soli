package com.example.backend.entity;

import java.io.Serializable;
import java.util.Objects;

public class DijakPredmetId implements Serializable {

    private Long dijak;
    private Long predmet;

    public DijakPredmetId() {}

    public DijakPredmetId(Long dijak, Long predmet) {
        this.dijak = dijak;
        this.predmet = predmet;
    }

    public Long getDijak() { return dijak; }
    public void setDijak(Long dijak) { this.dijak = dijak; }

    public Long getPredmet() { return predmet; }
    public void setPredmet(Long predmet) { this.predmet = predmet; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DijakPredmetId that)) return false;
        return Objects.equals(dijak, that.dijak) &&
                Objects.equals(predmet, that.predmet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dijak, predmet);
    }
}
