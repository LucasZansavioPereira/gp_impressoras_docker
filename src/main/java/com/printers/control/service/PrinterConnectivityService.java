package com.printers.control.service;

import com.printers.control.model.Printer;
import com.printers.control.repository.PrinterRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Serviço responsável exclusivamente por monitorar a conectividade de rede
 * das impressoras cadastradas, através do endereço IP.
 *
 * Esta lógica é mantida separada do PrinterService (que trata do CRUD e do
 * status operacional) porque representa uma responsabilidade diferente:
 * o status de conectividade aqui tratado reflete apenas se o equipamento
 * responde na rede (ping / InetAddress.isReachable), e nunca substitui o
 * status operacional (Funcionando, Quebrada, Manutenção), que continua sendo
 * controlado manualmente pela equipe de TI.
 */
@Service
public class PrinterConnectivityService {

    private static final Logger log = LoggerFactory.getLogger(PrinterConnectivityService.class);

    /** Timeout, em milissegundos, para considerar o IP indisponível. */
    private static final int TIMEOUT_MS = 3000;

    /** Limite de verificações simultâneas, para não sobrecarregar a rede ao checar muitas impressoras de uma vez. */
    private static final int MAX_CONCURRENT_CHECKS = 20;

    private final PrinterRepository repository;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_CHECKS);

    public PrinterConnectivityService(PrinterRepository repository) {
        this.repository = repository;
    }

    /**
     * Executa automaticamente a cada 10 minutos (com verificação inicial após 10s da inicialização),
     * percorrendo todas as impressoras cadastradas e atualizando o status de conectividade de cada uma.
     */
    @Scheduled(initialDelay = 10000, fixedRate = 10 * 60 * 1000)
    public void verificarAutomaticamente() {
        log.info("Executando verificação automática agendada de conectividade das impressoras...");
        verificarTodasImpressoras();
    }

    /**
     * Verifica a conectividade de todas as impressoras cadastradas de uma vez,
     * em paralelo (limitado a {@link #MAX_CONCURRENT_CHECKS} checagens simultâneas),
     * e persiste o resultado de cada uma. Usado tanto pelo scheduler quanto por
     * uma checagem manual disparada pelo usuário.
     */
    public List<Printer> verificarListaImpressoras(List<Printer> printers) {
        log.info("Iniciando verificação de conectividade de {} impressora(s)", printers.size());

        List<String> ipsToCheck = printers.stream()
                .filter(p -> p.getConnectionType() != Printer.ConnectionType.USB)
                .map(Printer::getIp)
                .filter(ip -> ip != null && !ip.isBlank())
                .distinct()
                .collect(Collectors.toList());

        List<CompletableFuture<Void>> futures = ipsToCheck.stream()
                .map(ip -> CompletableFuture.runAsync(() -> {
                    boolean online = testarConectividade(ip);
                    resolveIpGroupStatus(ip, online);
                }, executor))
                .collect(Collectors.toList());

        futures.forEach(CompletableFuture::join);

        printers.stream()
                .filter(p -> p.getConnectionType() == Printer.ConnectionType.USB)
                .forEach(p -> {
                    p.setConnectivityStatus(Printer.ConnectivityStatus.NAO_VERIFICADO);
                    p.setLastConnectivityCheck(Instant.now());
                    repository.save(p);
                });

        return printers.stream()
                .map(p -> repository.findById(p.getId()).orElse(p))
                .collect(Collectors.toList());
    }

    private void resolveIpGroupStatus(String ip, boolean isOnline) {
        List<Printer> printers = repository.findByIp(ip);
        if (printers.isEmpty()) return;

        if (!isOnline) {
            for (Printer p : printers) {
                p.setConnectivityStatus(Printer.ConnectivityStatus.INDISPONIVEL);
                p.setLastConnectivityCheck(Instant.now());
                repository.save(p);
            }
            return;
        }

        if (printers.size() == 1) {
            Printer p = printers.get(0);
            p.setConnectivityStatus(Printer.ConnectivityStatus.ONLINE);
            p.setLastConnectivityCheck(Instant.now());
            repository.save(p);
            return;
        }

        List<Printer> funcionando = printers.stream().filter(p -> p.getStatus() == Printer.Status.FUNCIONANDO).toList();
        List<Printer> winners = funcionando.isEmpty() 
            ? printers.stream().filter(p -> p.getStatus() == Printer.Status.BACKUP).toList()
            : funcionando;
            
        if (winners.isEmpty()) {
            winners = List.of(printers.get(0));
        }

        for (Printer p : printers) {
            if (winners.contains(p)) {
                p.setConnectivityStatus(Printer.ConnectivityStatus.ONLINE);
            } else {
                p.setConnectivityStatus(Printer.ConnectivityStatus.INDISPONIVEL);
            }
            p.setLastConnectivityCheck(Instant.now());
            repository.save(p);
        }
    }

    public List<Printer> verificarTodasImpressoras() {
        return verificarListaImpressoras(repository.findAll());
    }

    public List<Printer> verificarPorLocalizacao(String location) {
        if (location == null || location.isBlank()) {
            return verificarTodasImpressoras();
        }
        return verificarListaImpressoras(repository.findBySetorAntigoIgnoreCase(location.trim()));
    }

    /**
     * Verifica a conectividade de uma única impressora e persiste o resultado.
     */
    public Printer verificarImpressora(Printer printer) {
        String ip = printer.getIp();

        if (printer.getConnectionType() == Printer.ConnectionType.USB) {
            printer.setConnectivityStatus(Printer.ConnectivityStatus.NAO_VERIFICADO);
            printer.setLastConnectivityCheck(Instant.now());
            return repository.save(printer);
        }

        if (ip == null || ip.isBlank()) {
            return printer;
        }

        boolean online = testarConectividade(ip);
        resolveIpGroupStatus(ip, online);
        return repository.findById(printer.getId()).orElse(printer);
    }


    /** Verificação manual sob demanda para uma impressora específica (por id). */
    public Printer verificarPorId(String id) {
        Printer printer = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Impressora não encontrada: " + id));
        return verificarImpressora(printer);
    }

    private boolean testarConectividade(String ip) {
        return pingCmd(ip);
    }

    private boolean pingCmd(String ip) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("ping", "-n", "1", "-w", "2000", ip);
            } else {
                pb = new ProcessBuilder("ping", "-c", "1", "-W", "2", ip);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(4, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Ping para {} expirou (timeout)", ip);
                return false;
            }

            String outStr = output.toString().toLowerCase();
            boolean hasTtl = outStr.contains("ttl=");
            boolean ok = (process.exitValue() == 0) && hasTtl;

            log.info("Ping para {}: {} (exit={}, hasTtl={})", ip, ok ? "ONLINE" : "OFFLINE", process.exitValue(), hasTtl);
            return ok;
        } catch (Exception e) {
            log.warn("Erro ao executar ping para {}: {}", ip, e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
