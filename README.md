# 🖨️ Sistema de Gestão e Controle de Impressoras — Grupo Pereira

Sistema corporativo para controle centralizado de inventário, monitoramento de rede e gestão do parque de impressoras do **Grupo Pereira**.

---

## 🎯 Finalidade do Projeto

O **PrinterGen** foi desenvolvido com o objetivo de centralizar, organizar e automatizar o controle de todos os equipamentos de impressão distribuídos entre as diferentes plantas e setores do **Grupo Pereira**.

### Principais Objetivos:
- **Monitoramento Ativo de Conectividade:** Identificar instantaneamente quais impressoras de rede estão ativas (`IP Online`) ou sem comunicação (`IP Offline`).
- **Gestão de Inventário e Status Operacional:** Acompanhar o estado físico de cada equipamento (`Normal`, `Quebrada`, `Manutenção` e `Backup`), facilitando a tomada de decisão para substituições e reparos.
- **Rastreabilidade e Localização:** Mapear a movimentação e alocação física dos equipamentos por setor (`Localização Antiga → Setor Novo`).
- **Geração de Etiquetas QR:** Facilitar a identificação rápida em campo através de etiquetas padronizadas para colagem direta nos equipamentos.
- **Relatórios Executivos:** Exportar dados consolidados em planilhas Excel para auditoria interna e tomadas de decisão da equipe de TI.
- **Histórico de Auditoria:** Registrar de forma transparente todas as inserções, alterações de status e exclusões realizadas pelos operadores do sistema.

---

## 🚀 Recursos e Funcionalidades

### 💻 1. Controle de Equipamentos
- **Cadastro Detalhado:** Registro de código do equipamento, modelo, endereço IP, endereço MAC, tipo de conexão (**Ethernet** ou **USB**) e mapeamento de setores.
- **Status Operacionais:** Classificação entre impressoras em uso normal, sob manutenção, quebradas ou em estoque de reserva (*Backup*).
- **Filtros e Busca Rápida:** Filtros dinâmicos por status de conectividade/operação e busca em tempo real por qualquer atributo do equipamento.

### 🌐 2. Verificação de Conectividade IP
- **Teste de Comunicação Nativo:** Varredura via pacotes ICMP (Ping nativo com validação de TTL) para evitar falsos positivos na rede interna.
- **Verificação Instantânea ("Verificar Agora"):** Atualização sob demanda no cadastro da impressora com salvamento automático e feedback visual imediato.
- **Varredura Geral:** Botão para checagem em lote de todas as impressoras cadastradas.

### 🏷️ 3. Emissão de Etiquetas de Campo
- **Gerador de Tags:** Modal estilizado com resumo técnico do equipamento.
- **Impressão Otimizada:** Layout de impressão em página única com regras estritas para evitar desperdício de papel.

### 📊 4. Relatórios e Exportação de Dados
- **Painel de Relatório:** Tabela completa comparativa com informações de conectividade, modelo, setor e data/hora do último teste.
- **Exportação Excel (`.xlsx`):** Geração automática de relatórios em planilha via Apache POI.

### 🔒 5. Segurança e Auditoria
- **Controle de Acesso:** Sistema de autenticação de usuários com proteção contra tentativas seguidas de login (anti-brute force).
- **Auditoria Interna de Alterações:** Geração automática do log `Log/modificacoes_impressoras.txt` gravando quem alterou qual impressora, com data e hora.

---

## 🛠️ Tecnologias Utilizadas

### **Backend**
- **Java 17** / **Spring Boot 3**
- **Spring Data JPA** & **Hibernate** (Persistência e ORM)
- **Spring Security** (Autenticação, autorização e gerenciamento de sessões)
- **H2 Database / JPA** (Armazenamento de dados)
- **Apache POI** (Geração de relatórios em Excel)

### **Frontend**
- **HTML5 & CSS3 Vanilla** (Tema escuro corporativo personalizado para a **Grupo Pereira**)
- **JavaScript (ES6+)** (Arquitetura SPA assíncrona com `fetch` API)
- **Tabler Icons** (Conjunto de ícones vetoriais da interface)

---

## 🏃 Como Executar o Projeto

### Pré-requisitos:
- **Java 17** (JDK) ou superior instalado.
- **Maven** instalado (ou use a IDE de sua preferência como IntelliJ, Eclipse, VS Code).

### Passo a passo para rodar localmente:
1. Clone este repositório para o seu computador.
2. Abra um terminal na pasta raiz do projeto.
3. Para iniciar a aplicação, execute o comando Maven:
   ```bash
   mvn spring-boot:run
   ```
4. Aguarde a mensagem informando que a aplicação iniciou corretamente (ex: `Started PrinterControlApplication`).
5. Abra o seu navegador e acesse:
   ```text
   http://localhost:5080
   ```

---

## 🔑 Primeiro Acesso

Ao rodar a aplicação em um banco de dados novo ou vazio pela primeira vez, o sistema não terá nenhum usuário cadastrado. Para evitar que você fique "trancado" para fora, o sistema cria automaticamente um usuário padrão administrador:

- **Usuário:** `admin`
- **Senha:** `admin`

### 🛡️ Regras do Usuário Padrão
1. **Segurança:** Recomenda-se alterar a senha assim que fizer o primeiro login.
2. **Edição Exclusiva:** Diferente dos demais usuários, o "usuário padrão" pode ter o seu **nome de usuário alterado** via interface. Caso prefira usar outro nome de login em vez de "admin", você tem liberdade para trocar.
3. **Exclusão Bloqueada:** O usuário padrão é protegido pelo sistema e **nunca pode ser excluído**. Isso garante que sempre haverá pelo menos um administrador com acesso ao sistema.

---

## 📁 Estrutura do Projeto

```text
PrinterGen/
│
├── Log/                               # Histórico automático de alterações (auditoria)
│   └── modificacoes_impressoras.txt
│
├── src/
│   ├── main/
│   │   ├── java/com/printers/control/
│   │   │   ├── config/                # Segurança e inicialização
│   │   │   ├── controller/            # Endpoints REST da API
│   │   │   ├── model/                 # Entidades de banco de dados
│   │   │   ├── repository/            # Camada de dados Spring Data JPA
│   │   │   └── service/               # Serviços de conectividade, relatórios e auditoria
│   │   │
│   │   └── resources/
│   │       ├── application.properties # Configurações gerais
│   │       └── static/                # Interface web (HTML, CSS e JS)
│
├── .gitignore                         # Arquivos desconsiderados no versionamento
├── pom.xml                            # Configurações e dependências Maven
└── README.md                          # Documentação do projeto
```

---

## 👤 Autoria e Responsabilidade

Projeto desenvolvido por **Lucas Zansavio Pereira** para atendimento exclusivo às demandas de TI e infraestrutura do Grupo Pereira.
