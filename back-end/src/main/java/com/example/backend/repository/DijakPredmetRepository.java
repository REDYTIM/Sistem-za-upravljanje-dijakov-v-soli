package com.example.backend.repository;

import com.example.backend.entity.DijakPredmet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DijakPredmetRepository extends JpaRepository<DijakPredmet, Long> {

    // TO METODO DODAJ
    @Query("SELECT dp FROM DijakPredmet dp " +
            "JOIN FETCH dp.predmet " +
            "WHERE dp.dijak.id = :dijakId")
    List<DijakPredmet> findByDijakIdWithPredmet(@Param("dijakId") Long dijakId);
}