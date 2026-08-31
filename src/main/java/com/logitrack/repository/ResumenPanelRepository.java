package com.logitrack.repository;

import com.logitrack.model.ResumenPanel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ResumenPanelRepository extends JpaRepository<ResumenPanel, Long> {
    Optional<ResumenPanel> findByFecha(LocalDate fecha);
    Optional<ResumenPanel> findTopByOrderByCreatedAtDesc();
}