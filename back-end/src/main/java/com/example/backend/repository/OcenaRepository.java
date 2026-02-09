package com.example.backend.repository;

import com.example.backend.entity.Ocena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OcenaRepository extends JpaRepository<Ocena, Long> {

    // TE METODE DODAJ
    @Query("SELECT o FROM Ocena o WHERE o.dijakPredmet.id = :dijakPredmetId")
    List<Ocena> findByDijakPredmetId(@Param("dijakPredmetId") Long dijakPredmetId);

    @Query("SELECT o FROM Ocena o WHERE o.dijakPredmet.dijak.id = :dijakId AND o.dijakPredmet.predmet.ime = :predmetIme")
    List<Ocena> findByDijakIdAndPredmetIme(@Param("dijakId") Long dijakId,
                                           @Param("predmetIme") String predmetIme);

    @Query("SELECT p.ime, o.ocena FROM Ocena o " +
            "JOIN o.dijakPredmet dp " +
            "JOIN dp.predmet p " +
            "JOIN dp.dijak d " +
            "WHERE d.id = :dijakId")
    List<Object[]> findOceneWithPredmetByDijakId(@Param("dijakId") Long dijakId);
}