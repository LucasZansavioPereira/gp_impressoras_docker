package com.printers.control.controller;

import com.printers.control.model.Printer;
import com.printers.control.service.PrinterConnectivityService;
import com.printers.control.service.PrinterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/printers")
public class PrinterController {

    private final PrinterService service;
    private final PrinterConnectivityService connectivityService;

    public PrinterController(PrinterService service, PrinterConnectivityService connectivityService) {
        this.service = service;
        this.connectivityService = connectivityService;
    }

    @GetMapping
    public List<Printer> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Printer findById(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Printer create(@Valid @RequestBody Printer printer,
                         @RequestHeader(value = "X-User-Name", required = false) String username) {
        return service.create(printer, username);
    }

    @PutMapping("/{id}")
    public Printer update(@PathVariable String id,
                         @Valid @RequestBody Printer printer,
                         @RequestHeader(value = "X-User-Name", required = false) String username) {
        return service.update(id, printer, username);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id,
                       @RequestHeader(value = "X-User-Name", required = false) String username) {
        service.delete(id, username);
    }

    /**
     * Força a verificação de conectividade (ping/IP) de uma impressora específica,
     * sem precisar esperar a próxima execução automática do Scheduler.
     * Não altera o status operacional da impressora.
     */
    @PostMapping("/{id}/verificar-conectividade")
    public Printer verificarConectividade(@PathVariable String id) {
        return connectivityService.verificarPorId(id);
    }

    /**
     * Força a verificação de conectividade (ping/IP) de todas as impressoras
     * cadastradas de uma só vez, sem precisar esperar o Scheduler ou verificar
     * uma por uma manualmente. Impressoras sem IP cadastrado são ignoradas.
     */
    @PostMapping("/verificar-conectividade")
    public List<Printer> verificarConectividadeTodas(@RequestParam(required = false) String location) {
        if (location != null && !location.isBlank()) {
            return connectivityService.verificarPorLocalizacao(location);
        }
        return connectivityService.verificarTodasImpressoras();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", ex.getMessage()));
    }
}
