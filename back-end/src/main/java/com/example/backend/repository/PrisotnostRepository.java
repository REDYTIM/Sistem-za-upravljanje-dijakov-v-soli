package com.example.backend.repository;

import com.example.backend.entity.Prisotnost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrisotnostRepository extends JpaRepository<Prisotnost, Long> {

    // Native query za direktno delo s tvojo strukturo
    @Query(value = "SELECT * FROM prisotnost WHERE dijak_predmet_id = :dijakPredmetId ORDER BY datum DESC",
            nativeQuery = true)
    List<Prisotnost> findByDijakPredmetId(@Param("dijakPredmetId") Long dijakPredmetId);

    // Pridobi prisotnosti za dijaka in predmet
    @Query(value = "SELECT p.* FROM prisotnost p " +
            "JOIN dijak_predmet dp ON p.dijak_predmet_id = dp.id " +
            "JOIN predmeti pr ON dp.predmeti_id = pr.id " +
            "WHERE dp.dijak_id = :dijakId AND pr.ime = :predmetIme " +
            "ORDER BY p.datum DESC",
            nativeQuery = true)
    List<Prisotnost> findByDijakIdAndPredmetIme(@Param("dijakId") Long dijakId,
                                                @Param("predmetIme") String predmetIme);

    // Pridobi povprečno prisotnost za dijaka in predmet
    @Query(value = "SELECT COALESCE(AVG(p.prisotnost), 0) FROM prisotnost p " +
            "JOIN dijak_predmet dp ON p.dijak_predmet_id = dp.id " +
            "JOIN predmeti pr ON dp.predmeti_id = pr.id " +
            "WHERE dp.dijak_id = :dijakId AND pr.ime = :predmetIme",
            nativeQuery = true)
    Double findPovprecnaPrisotnost(@Param("dijakId") Long dijakId,
                                   @Param("predmetIme") String predmetIme);

    // Pridobi prisotnosti za določen datum
    @Query(value = "SELECT p.* FROM prisotnost p " +
            "JOIN dijak_predmet dp ON p.dijak_predmet_id = dp.id " +
            "WHERE dp.dijak_id = :dijakId AND p.datum = :datum",
            nativeQuery = true)
    List<Prisotnost> findByDijakIdAndDatum(@Param("dijakId") Long dijakId,
                                           @Param("datum") LocalDate datum);

    // Preveri ali že obstaja vnos za dani datum in dijak_predmet
    @Query(value = "SELECT COUNT(*) > 0 FROM prisotnost " +
            "WHERE dijak_predmet_id = :dijakPredmetId AND datum = :datum",
            nativeQuery = true)
    boolean existsByDijakPredmetIdAndDatum(@Param("dijakPredmetId") Long dijakPredmetId,
                                           @Param("datum") LocalDate datum);
}