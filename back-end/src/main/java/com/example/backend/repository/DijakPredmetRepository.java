package com.example.backend.repository;

import com.example.backend.entity.DijakPredmet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DijakPredmetRepository extends JpaRepository<DijakPredmet, Long> {

    @Query("SELECT dp FROM DijakPredmet dp " +
            "LEFT JOIN FETCH dp.predmet " +
            "LEFT JOIN FETCH dp.ocene " +
            "WHERE dp.dijak.id = :dijakId")
    List<DijakPredmet> findByDijakIdWithDetails(@Param("dijakId") Long dijakId);

    // Dodajte to metodo za poizvedbo po predmetu
    @Query("SELECT dp FROM DijakPredmet dp " +
            "LEFT JOIN FETCH dp.predmet " +
            "LEFT JOIN FETCH dp.ocene " +
            "WHERE dp.dijak.id = :dijakId AND dp.predmet.ime = :predmetIme")
    List<DijakPredmet> findByDijakIdAndPredmetIme(
            @Param("dijakId") Long dijakId,
            @Param("predmetIme") String predmetIme
    );
}