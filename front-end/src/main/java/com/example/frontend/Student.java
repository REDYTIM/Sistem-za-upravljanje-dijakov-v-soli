package com.example.frontend;

import javafx.beans.property.*;

public class Student {

    private final LongProperty id;
    private final StringProperty ime;
    private final StringProperty priimek;
    private final IntegerProperty emso;
    private final StringProperty datumRojstva;

    public Student(long id, String ime, String priimek, int emso, String datumRojstva) {
        this.id = new SimpleLongProperty(id);
        this.ime = new SimpleStringProperty(ime);
        this.priimek = new SimpleStringProperty(priimek);
        this.emso = new SimpleIntegerProperty(emso);
        this.datumRojstva = new SimpleStringProperty(datumRojstva);
    }

    public long getId() { return id.get(); }
    public LongProperty idProperty() { return id; }

    public String getIme() { return ime.get(); }
    public StringProperty imeProperty() { return ime; }

    public String getPriimek() { return priimek.get(); }
    public StringProperty priimekProperty() { return priimek; }

    public int getEmso() { return emso.get(); }
    public IntegerProperty emsoProperty() { return emso; }

    public String getDatumRojstva() { return datumRojstva.get(); }
    public StringProperty datumRojstvaProperty() { return datumRojstva; }
}
