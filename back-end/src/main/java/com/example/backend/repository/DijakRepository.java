package com.example.backend.repository;

import com.example.backend.entity.Dijak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DijakRepository extends JpaRepository<Dijak, Long> {
    // Spring Data JPA sam implementira metode
}