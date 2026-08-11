package com.printers.control.service;

import com.printers.control.model.Printer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PrinterAuditLoggerService {

    private static final Logger log = LoggerFactory.getLogger(PrinterAuditLoggerService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Path LOG_DIR = Paths.get("Log");
    private static final Path LOG_FILE = LOG_DIR.resolve("modificacoes_impressoras.txt");

    public PrinterAuditLoggerService() {
        initLogFile();
    }

    private synchronized void initLogFile() {
        try {
            if (!Files.exists(LOG_DIR)) {
                Files.createDirectories(LOG_DIR);
            }
            if (!Files.exists(LOG_FILE)) {
                Files.createFile(LOG_FILE);
                writeLine("===========================================================================================");
                writeLine("LOG DE MODIFICAÇÕES DE IMPRESSORAS — CONTROLE DE IMPRESSORAS");
                writeLine("");
                writeLine("Arquivo gerado automaticamente para registrar todas as alterações realizadas pelos usuários.");
                writeLine("============================================================================================\n");
            }
        } catch (IOException e) {
            log.error("Erro ao inicializar arquivo de log de auditoria: {}", e.getMessage(), e);
        }
    }

    public synchronized void logCreate(String username, Printer printer) {
        String user = (username != null && !username.isBlank()) ? username : "Sistema";
        String time = LocalDateTime.now().format(DATE_FORMATTER);
        String msg = String.format("[%s] [USUÁRIO: %s] [AÇÃO: CADASTRO] Impressora '%s' cadastrada | Modelo: %s | Status: %s | Setor: %s | IP: %s | MAC: %s",
                time, user,
                printer.getCodigo(),
                printer.getModelo() != null ? printer.getModelo() : "-",
                printer.getStatus() != null ? printer.getStatus() : "-",
                printer.getSetorNovo() != null ? printer.getSetorNovo() : "-",
                printer.getIp() != null ? printer.getIp() : "-",
                printer.getMarcaModelo() != null ? printer.getMarcaModelo() : "-");
        writeLine(msg);
    }

    public synchronized void logUpdate(String username, Printer existing, Printer changes) {
        String user = (username != null && !username.isBlank()) ? username : "Sistema";
        String time = LocalDateTime.now().format(DATE_FORMATTER);

        List<String> diffs = new ArrayList<>();

        if (changes.getCodigo() != null && !Objects.equals(existing.getCodigo(), changes.getCodigo())) {
            diffs.add(String.format("Nome/Código [%s -> %s]", existing.getCodigo(), changes.getCodigo()));
        }
        if (changes.getModelo() != null && !Objects.equals(existing.getModelo(), changes.getModelo())) {
            diffs.add(String.format("Modelo [%s -> %s]", existing.getModelo(), changes.getModelo()));
        }
        if (changes.getStatus() != null && existing.getStatus() != changes.getStatus()) {
            diffs.add(String.format("Status [%s -> %s]", existing.getStatus(), changes.getStatus()));
        }
        if (changes.getSetorAntigo() != null && !Objects.equals(existing.getSetorAntigo(), changes.getSetorAntigo())) {
            diffs.add(String.format("Localização [%s -> %s]", existing.getSetorAntigo(), changes.getSetorAntigo()));
        }
        if (changes.getSetorNovo() != null && !Objects.equals(existing.getSetorNovo(), changes.getSetorNovo())) {
            diffs.add(String.format("Setor [%s -> %s]", existing.getSetorNovo(), changes.getSetorNovo()));
        }
        if (changes.getIp() != null && !Objects.equals(existing.getIp(), changes.getIp())) {
            diffs.add(String.format("IP [%s -> %s]", existing.getIp(), changes.getIp()));
        }
        if (changes.getMarcaModelo() != null && !Objects.equals(existing.getMarcaModelo(), changes.getMarcaModelo())) {
            diffs.add(String.format("MAC [%s -> %s]", existing.getMarcaModelo(), changes.getMarcaModelo()));
        }
        if (changes.getConnectionType() != null && existing.getConnectionType() != changes.getConnectionType()) {
            diffs.add(String.format("Tipo Conexão [%s -> %s]", existing.getConnectionType(), changes.getConnectionType()));
        }
        if (changes.getProblema() != null && !Objects.equals(existing.getProblema(), changes.getProblema())) {
            diffs.add(String.format("Problema [%s -> %s]", existing.getProblema(), changes.getProblema()));
        }

        String diffStr = diffs.isEmpty() ? "Nenhum campo alterado." : String.join(", ", diffs);
        String msg = String.format("[%s] [USUÁRIO: %s] [AÇÃO: EDIÇÃO] Impressora '%s' editada | Alterações: %s",
                time, user, existing.getCodigo(), diffStr);
        writeLine(msg);
    }

    public synchronized void logDelete(String username, Printer printer) {
        String user = (username != null && !username.isBlank()) ? username : "Sistema";
        String time = LocalDateTime.now().format(DATE_FORMATTER);
        String msg = String.format("[%s] [USUÁRIO: %s] [AÇÃO: EXCLUSÃO] Impressora '%s' (Modelo: %s, Setor: %s, IP: %s) foi EXCLUÍDA.",
                time, user,
                printer.getCodigo(),
                printer.getModelo() != null ? printer.getModelo() : "-",
                printer.getSetorNovo() != null ? printer.getSetorNovo() : "-",
                printer.getIp() != null ? printer.getIp() : "-");
        writeLine(msg);
    }

    private void writeLine(String content) {
        try {
            if (!Files.exists(LOG_FILE)) {
                initLogFile();
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE.toFile(), true))) {
                bw.write(content);
                bw.newLine();
            }
        } catch (IOException e) {
            log.error("Erro ao escrever no arquivo de log de auditoria: {}", e.getMessage(), e);
        }
    }
}
