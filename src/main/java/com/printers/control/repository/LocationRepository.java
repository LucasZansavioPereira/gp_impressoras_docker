package com.printers.control.repository;

import com.printers.control.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, String> {
    Optional<Location> findByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCase(String nome);
}
