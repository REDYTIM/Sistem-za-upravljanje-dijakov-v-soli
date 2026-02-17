package com.example.backend.repository;

import com.example.backend.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {

    // Najdi vse log vnose, urejene po času (najnovejši najprej)
    List<Log> findAllByOrderByCasDesc();

    // Najdi log vnose, ki vsebujejo določen tekst
    List<Log> findByUkazContainingIgnoreCase(String searchText);

    // Najdi zadnjih N log vnosov
    @Query(value = "SELECT * FROM log ORDER BY cas DESC LIMIT :limit", nativeQuery = true)
    List<Log> findTopNByOrderByCasDesc(@Param("limit") int limit);

    // Najdi log vnose po tipu operacije
    @Query("SELECT l FROM Log l WHERE l.ukaz LIKE %:operation% ORDER BY l.cas DESC")
    List<Log> findByOperation(@Param("operation") String operation);

    // Najdi log vnose za določenega dijaka
    @Query("SELECT l FROM Log l WHERE l.ukaz LIKE %:dijakId% ORDER BY l.cas DESC")
    List<Log> findByDijakId(@Param("dijakId") String dijakId);
}