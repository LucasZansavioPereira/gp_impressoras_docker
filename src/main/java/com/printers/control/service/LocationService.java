package com.printers.control.service;

import com.printers.control.model.Location;
import com.printers.control.model.Printer;
import com.printers.control.repository.LocationRepository;
import com.printers.control.repository.PrinterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final PrinterRepository printerRepository;

    public LocationService(LocationRepository locationRepository, PrinterRepository printerRepository) {
        this.locationRepository = locationRepository;
        this.printerRepository = printerRepository;
    }

    public List<Location> findAll() {
        syncLocationsFromPrinters();
        return locationRepository.findAll();
    }

    @Transactional
    public Location create(Location location) {
        if (location.getNome() == null || location.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da localização é obrigatório");
        }
        String nameTrimmed = location.getNome().trim();
        if (locationRepository.existsByNomeIgnoreCase(nameTrimmed)) {
            throw new IllegalArgumentException("Já existe uma localização com o nome '" + nameTrimmed + "'");
        }
        location.setNome(nameTrimmed);
        return locationRepository.save(location);
    }

    @Transactional
    public void delete(String id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Localização não encontrada"));

        List<Printer> printersWithLoc = printerRepository.findBySetorAntigoIgnoreCase(location.getNome());
        if (!printersWithLoc.isEmpty()) {
            throw new IllegalArgumentException("Não é possível excluir a localização '" + location.getNome() + "' pois existem " + printersWithLoc.size() + " impressora(s) cadastrada(s) nela.");
        }

        locationRepository.delete(location);
    }

    @Transactional
    public void syncLocationsFromPrinters() {
        List<Printer> printers = printerRepository.findAll();
        for (Printer p : printers) {
            if (p.getSetorAntigo() != null && !p.getSetorAntigo().trim().isEmpty()) {
                String locName = p.getSetorAntigo().trim();
                if (!locationRepository.existsByNomeIgnoreCase(locName)) {
                    locationRepository.save(new Location(locName));
                }
            }
        }
    }
}
