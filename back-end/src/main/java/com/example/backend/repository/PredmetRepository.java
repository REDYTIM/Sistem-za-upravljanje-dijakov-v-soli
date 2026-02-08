package com.example.backend.repository;

import com.example.backend.entity.Predmet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredmetRepository extends JpaRepository<Predmet, Long> {
}
