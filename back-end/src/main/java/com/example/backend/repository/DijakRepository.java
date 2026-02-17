package com.example.backend.repository;

import com.example.backend.entity.Dijak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DijakRepository extends JpaRepository<Dijak, Long> {

    // Iskanje po imenu (case insensitive)
    @Query("SELECT d FROM Dijak d WHERE LOWER(d.ime) LIKE LOWER(CONCAT('%', :ime, '%'))")
    List<Dijak> findByImeContainingIgnoreCase(@Param("ime") String ime);

    // Iskanje po priimku (case insensitive)
    @Query("SELECT d FROM Dijak d WHERE LOWER(d.priimek) LIKE LOWER(CONCAT('%', :priimek, '%'))")
    List<Dijak> findByPriimekContainingIgnoreCase(@Param("priimek") String priimek);

    // Iskanje po imenu in priimku (case insensitive)
    @Query("SELECT d FROM Dijak d WHERE LOWER(d.ime) LIKE LOWER(CONCAT('%', :ime, '%')) AND LOWER(d.priimek) LIKE LOWER(CONCAT('%', :priimek, '%'))")
    List<Dijak> findByImeContainingIgnoreCaseAndPriimekContainingIgnoreCase(
            @Param("ime") String ime,
            @Param("priimek") String priimek);

    // Iskanje po EMSO
    @Query("SELECT d FROM Dijak d WHERE d.emso LIKE CONCAT('%', :emso, '%')")
    List<Dijak> findByEmsoContaining(@Param("emso") String emso);

    // Ali pa uporabite JPA query metode (če imate pravilno imenovanje polj)
    // List<Dijak> findByImeContainingIgnoreCase(String ime);
    // List<Dijak> findByPriimekContainingIgnoreCase(String priimek);
    // List<Dijak> findByEmsoContaining(String emso);
}