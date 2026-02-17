package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log")
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ukaz", columnDefinition = "TEXT")
    private String ukaz;

    @Column(name = "cas")
    private LocalDateTime cas;

    // Konstruktorji
    public Log() {}

    public Log(String ukaz) {
        this.ukaz = ukaz;
        this.cas = LocalDateTime.now();
    }

    // Getterji in setterji
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUkaz() {
        return ukaz;
    }

    public void setUkaz(String ukaz) {
        this.ukaz = ukaz;
    }

    public LocalDateTime getCas() {
        return cas;
    }

    public void setCas(LocalDateTime cas) {
        this.cas = cas;
    }

    @PrePersist
    protected void onCreate() {
        cas = LocalDateTime.now();
    }
}