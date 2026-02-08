package com.example.backend.repository;

import com.example.backend.entity.Razred;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RazredRepository extends JpaRepository<Razred, Long> {
}