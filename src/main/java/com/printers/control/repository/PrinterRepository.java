package com.printers.control.repository;

import com.printers.control.model.Printer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrinterRepository extends JpaRepository<Printer, String> {
    List<Printer> findAllByOrderByCodigoAsc();
    List<Printer> findAllByOrderByUpdatedAtDesc();
    List<Printer> findByCodigo(String codigo);
    Optional<Printer> findByCodigoAndStatus(String codigo, Printer.Status status);
    List<Printer> findBySetorAntigoIgnoreCase(String setorAntigo);
}
