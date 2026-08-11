package com.printers.control.service;

import com.printers.control.model.Printer;
import com.printers.control.repository.PrinterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class PrinterService {

    private final PrinterRepository repository;
    private final PrinterConnectivityService connectivityService;
    private final PrinterAuditLoggerService auditLoggerService;

    public PrinterService(PrinterRepository repository, PrinterConnectivityService connectivityService, PrinterAuditLoggerService auditLoggerService) {
        this.repository = repository;
        this.connectivityService = connectivityService;
        this.auditLoggerService = auditLoggerService;
    }

    public List<Printer> findAll() {
        return repository.findAllByOrderByCodigoAsc();
    }

    public Printer findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Impressora não encontrada: " + id));
    }

    public Printer create(Printer printer) {
        return create(printer, null);
    }

    public Printer create(Printer printer, String username) {
        printer.setId(null);
        if (printer.getStatus() == null) {
            printer.setStatus(Printer.Status.FUNCIONANDO);
        }
        if (printer.getCodigo() == null || printer.getCodigo().isBlank()) {
            throw new IllegalArgumentException("O código da impressora é obrigatório");
        }
        if (printer.getModelo() == null || printer.getModelo().isBlank()) {
            throw new IllegalArgumentException("O modelo da impressora é obrigatório");
        }
        validateUniqueCodigoStatus(printer.getCodigo(), printer.getStatus(), printer.getModelo(), printer.getSetorAntigo(), null);
        if (printer.getConnectionType() == null) {
            printer.setConnectionType(Printer.ConnectionType.ETHERNET);
        }
        if (printer.getConnectionType() == Printer.ConnectionType.USB) {
            printer.setIp(null);
            printer.setMarcaModelo(null);
            printer.setConnectivityStatus(Printer.ConnectivityStatus.NAO_VERIFICADO);
        } else {
            validateIpAndMac(printer.getIp(), printer.getMarcaModelo());
        }

        Printer savedPrinter = repository.save(printer);
        Printer verifiedPrinter = connectivityService.verificarImpressora(savedPrinter);
        auditLoggerService.logCreate(username, verifiedPrinter);
        return verifiedPrinter;
    }

    public Printer update(String id, Printer changes) {
        return update(id, changes, null);
    }

    public Printer update(String id, Printer changes, String username) {
        Printer existing = findById(id);
        Printer existingCopy = new Printer();
        existingCopy.setCodigo(existing.getCodigo());
        existingCopy.setModelo(existing.getModelo());
        existingCopy.setStatus(existing.getStatus());
        existingCopy.setProblema(existing.getProblema());
        existingCopy.setSetorAntigo(existing.getSetorAntigo());
        existingCopy.setSetorNovo(existing.getSetorNovo());
        existingCopy.setIp(existing.getIp());
        existingCopy.setMarcaModelo(existing.getMarcaModelo());
        existingCopy.setConnectionType(existing.getConnectionType());

        String finalCodigo = changes.getCodigo() != null ? changes.getCodigo() : existing.getCodigo();
        Printer.Status finalStatus = changes.getStatus() != null ? changes.getStatus() : existing.getStatus();
        String finalModelo = changes.getModelo() != null ? changes.getModelo() : existing.getModelo();
        String finalSetorAntigo = changes.getSetorAntigo() != null ? changes.getSetorAntigo() : existing.getSetorAntigo();
        Printer.ConnectionType finalConnType = changes.getConnectionType() != null ? changes.getConnectionType() : existing.getConnectionType();
        String finalIp = changes.getIp() != null ? changes.getIp() : existing.getIp();
        String finalMac = changes.getMarcaModelo() != null ? changes.getMarcaModelo() : existing.getMarcaModelo();

        if (finalModelo == null || finalModelo.isBlank()) {
            throw new IllegalArgumentException("O modelo da impressora é obrigatório");
        }
        validateUniqueCodigoStatus(finalCodigo, finalStatus, finalModelo, finalSetorAntigo, id);

        existing.setCodigo(finalCodigo);
        existing.setStatus(finalStatus);
        existing.setProblema(changes.getProblema());
        existing.setSetorAntigo(changes.getSetorAntigo());
        existing.setSetorNovo(changes.getSetorNovo());
        existing.setModelo(finalModelo);
        existing.setMarcaModelo(finalMac);
        existing.setIp(finalIp);
        existing.setConnectionType(finalConnType);

        if (existing.getConnectionType() == Printer.ConnectionType.USB) {
            existing.setIp(null);
            existing.setMarcaModelo(null);
            existing.setConnectivityStatus(Printer.ConnectivityStatus.NAO_VERIFICADO);
        } else {
            validateIpAndMac(existing.getIp(), existing.getMarcaModelo());
        }

        Printer savedPrinter = repository.save(existing);
        Printer verifiedPrinter = connectivityService.verificarImpressora(savedPrinter);
        auditLoggerService.logUpdate(username, existingCopy, changes);
        return verifiedPrinter;
    }

    private static final String IP_REGEX = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final String MAC_REGEX = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$|^([0-9A-Fa-f]{12})$";

    private void validateIpAndMac(String ip, String mac) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("Informe o endereço IP para conexão Ethernet");
        }
        if (!ip.trim().matches(IP_REGEX)) {
            throw new IllegalArgumentException("Endereço IP inválido. Digite um IP no formato correto (ex: 192.168.1.50).");
        }
        if (mac == null || mac.isBlank()) {
            throw new IllegalArgumentException("Informe o endereço MAC para conexão Ethernet");
        }
        if (!mac.trim().matches(MAC_REGEX)) {
            throw new IllegalArgumentException("Endereço MAC inválido. Digite um MAC no formato correto (ex: AA:BB:CC:DD:EE:FF).");
        }
    }

    private void validateUniqueCodigoStatus(String codigo, Printer.Status status, String modelo, String setorAntigo, String currentId) {
        if (status == Printer.Status.BACKUP) {
            Optional<Printer> backupWithCodigo = repository.findByCodigoAndStatus(codigo, Printer.Status.BACKUP);
            if (backupWithCodigo.isPresent() && (currentId == null || !backupWithCodigo.get().getId().equals(currentId))) {
                throw new IllegalArgumentException("Já existe uma impressora BACKUP cadastrada com esse código.");
            }
            return;
        }

        List<Printer> sameCodeSameStatus = repository.findByCodigo(codigo).stream()
                .filter(p -> p.getStatus() == status)
                .toList();

        boolean duplicateModelo = sameCodeSameStatus.stream()
                .anyMatch(p -> p.getModelo() != null && p.getModelo().equalsIgnoreCase(modelo)
                        && (p.getSetorAntigo() == null ? setorAntigo == null : p.getSetorAntigo().equalsIgnoreCase(setorAntigo))
                        && (currentId == null || !p.getId().equals(currentId)));

        if (duplicateModelo) {
            throw new IllegalArgumentException("Já existe uma impressora com mesmo nome, status, modelo e localização.");
        }
    }

    public void delete(String id) {
        delete(id, null);
    }

    public void delete(String id, String username) {
        Printer existing = findById(id);
        repository.delete(existing);
        auditLoggerService.logDelete(username, existing);
    }
}
