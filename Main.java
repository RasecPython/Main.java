import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

enum Categoria {
    FESTA,
    ESPORTIVO,
    SHOW,
    FEIRA,
    PALESTRA,
    TEATRO,
    OUTRO;

    public static void exibirCategorias() {
        Categoria[] categorias = Categoria.values();
        for (int i = 0; i < categorias.length; i++) {
            System.out.println((i + 1) + " - " + categorias[i]);
        }
    }

    public static Categoria porIndice(int indice) {
        Categoria[] categorias = Categoria.values();
        if (indice < 1 || indice > categorias.length) {
            return OUTRO;
        }
        return categorias[indice - 1];
    }
}

final class Usuario {
    private final String nome;
    private final String email;
    private final String cidade;
    private final String telefone;

    public Usuario(String nome, String email, String cidade, String telefone) {
        this.nome = nome;
        this.email = email;
        this.cidade = cidade;
        this.telefone = telefone;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getCidade() {
        return cidade;
    }

    public String getTelefone() {
        return telefone;
    }

    @Override
    public String toString() {
        return "Nome: " + nome +
                " | Email: " + email +
                " | Cidade: " + cidade +
                " | Telefone: " + telefone;
    }
}

final class Evento {
    private static final DateTimeFormatter EXIBICAO_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final long DURACAO_PADRAO_HORAS = 2;

    private final int id;
    private final String nome;
    private final String endereco;
    private final Categoria categoria;
    private final LocalDateTime horario;
    private final String descricao;
    private final Set<String> participantes;

    public Evento(int id,
                  String nome,
                  String endereco,
                  Categoria categoria,
                  LocalDateTime horario,
                  String descricao) {
        this.id = id;
        this.nome = nome;
        this.endereco = endereco;
        this.categoria = categoria;
        this.horario = horario;
        this.descricao = descricao;
        this.participantes = new HashSet<>();
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public LocalDateTime getHorario() {
        return horario;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getQuantidadeParticipantes() {
        return participantes.size();
    }

    public boolean estaOcorrendoAgora() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime fim = horario.plusHours(DURACAO_PADRAO_HORAS);
        return (agora.isEqual(horario) || agora.isAfter(horario)) && agora.isBefore(fim);
    }

    public boolean jaOcorreu() {
        return LocalDateTime.now().isAfter(horario.plusHours(DURACAO_PADRAO_HORAS));
    }

    public boolean participa(String email) {
        return participantes.contains(email.toLowerCase());
    }

    public void adicionarParticipante(String email) {
        participantes.add(email.toLowerCase());
    }

    public void removerParticipante(String email) {
        participantes.remove(email.toLowerCase());
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("|", "\\|");
    }

    private static List<String> dividirEscapado(String linha) {
        List<String> partes = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean escape = false;

        for (char c : linha.toCharArray()) {
            if (escape) {
                atual.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '|') {
                partes.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }

        partes.add(atual.toString());
        return partes;
    }

    public String serializar() {
        String participantesTexto = String.join(",", participantes);
        return id + "|" +
                escapar(nome) + "|" +
                escapar(endereco) + "|" +
                categoria.name() + "|" +
                horario.toString() + "|" +
                escapar(descricao) + "|" +
                escapar(participantesTexto);
    }

    public static Evento desserializar(String linha) {
        List<String> partes = dividirEscapado(linha);
        if (partes.size() < 7) {
            return null;
        }

        int id = Integer.parseInt(partes.get(0));
        String nome = partes.get(1);
        String endereco = partes.get(2);
        Categoria categoria = Categoria.valueOf(partes.get(3));
        LocalDateTime horario = LocalDateTime.parse(partes.get(4));
        String descricao = partes.get(5);
        String participantesTexto = partes.get(6);

        Evento evento = new Evento(id, nome, endereco, categoria, horario, descricao);

        if (!participantesTexto.isBlank()) {
            String[] itens = participantesTexto.split(",");
            for (String item : itens) {
                String email = item.trim();
                if (!email.isBlank()) {
                    evento.adicionarParticipante(email);
                }
            }
        }

        return evento;
    }

    @Override
    public String toString() {
        String status;
        if (estaOcorrendoAgora()) {
            status = "OCORRENDO AGORA";
        } else if (jaOcorreu()) {
            status = "JÁ OCORREU";
        } else {
            status = "PRÓXIMO";
        }

        return "ID: " + id +
                "\nNome: " + nome +
                "\nEndereço: " + endereco +
                "\nCategoria: " + categoria +
                "\nHorário: " + horario.format(EXIBICAO_FORMATTER) +
                "\nDescrição: " + descricao +
                "\nParticipantes confirmados: " + getQuantidadeParticipantes() +
                "\nStatus: " + status;
    }
}

final class SistemaEventos {
    private static final String ARQUIVO = "events.data";
    private static final DateTimeFormatter INPUT_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final List<Evento> eventos;
    private final Scanner scanner;
    private Usuario usuarioAtual;

    public SistemaEventos() {
        this.eventos = new ArrayList<>();
        this.scanner = new Scanner(System.in);
    }

    public void iniciar() {
        carregarEventos();
        cadastrarUsuario();
        menu();
    }

    private void cadastrarUsuario() {
        System.out.println("=== CADASTRO DO USUÁRIO ===");

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine();

        System.out.print("Cidade: ");
        String cidade = scanner.nextLine();

        System.out.print("Telefone: ");
        String telefone = scanner.nextLine();

        usuarioAtual = new Usuario(nome, email, cidade, telefone);

        System.out.println("\nUsuário cadastrado com sucesso.");
        System.out.println(usuarioAtual);
    }

    private void menu() {
        int opcao;

        do {
            System.out.println("\n=== SISTEMA DE EVENTOS ===");
            System.out.println("Usuário: " + usuarioAtual.getNome() + " | Cidade: " + usuarioAtual.getCidade());
            System.out.println("1 - Cadastrar evento");
            System.out.println("2 - Listar todos os eventos");
            System.out.println("3 - Listar próximos eventos");
            System.out.println("4 - Listar eventos ocorrendo agora");
            System.out.println("5 - Listar eventos passados");
            System.out.println("6 - Confirmar participação");
            System.out.println("7 - Ver eventos confirmados");
            System.out.println("8 - Cancelar participação");
            System.out.println("9 - Salvar eventos");
            System.out.println("0 - Sair");
            System.out.print("Escolha uma opção: ");

            opcao = lerInteiro();

            switch (opcao) {
                case 1 -> cadastrarEvento();
                case 2 -> listarTodosEventos();
                case 3 -> listarProximosEventos();
                case 4 -> listarEventosOcorrendoAgora();
                case 5 -> listarEventosPassados();
                case 6 -> confirmarParticipacao();
                case 7 -> listarEventosConfirmados();
                case 8 -> cancelarParticipacao();
                case 9 -> {
                    salvarEventos();
                    System.out.println("Eventos salvos com sucesso.");
                }
                case 0 -> {
                    salvarEventos();
                    System.out.println("Encerrando o programa.");
                }
                default -> System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    private void cadastrarEvento() {
        System.out.println("\n=== CADASTRO DE EVENTO ===");

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        System.out.print("Endereço: ");
        String endereco = scanner.nextLine();

        System.out.println("Categorias:");
        Categoria.exibirCategorias();
        System.out.print("Escolha a categoria: ");
        int indiceCategoria = lerInteiro();
        Categoria categoria = Categoria.porIndice(indiceCategoria);

        LocalDateTime horario = lerDataHora();

        System.out.print("Descrição: ");
        String descricao = scanner.nextLine();

        int id = gerarNovoId();
        Evento evento = new Evento(id, nome, endereco, categoria, horario, descricao);

        eventos.add(evento);
        ordenarEventos();
        salvarEventos();

        System.out.println("Evento cadastrado com sucesso.");
    }

    private void listarTodosEventos() {
        System.out.println("\n=== TODOS OS EVENTOS ===");
        listarEventos(eventos);
    }

    private void listarProximosEventos() {
        System.out.println("\n=== PRÓXIMOS EVENTOS ===");
        List<Evento> proximos = new ArrayList<>();
        for (Evento evento : eventos) {
            if (!evento.jaOcorreu()) {
                proximos.add(evento);
            }
        }
        listarEventos(proximos);
    }

    private void listarEventosOcorrendoAgora() {
        System.out.println("\n=== EVENTOS OCORRENDO AGORA ===");
        List<Evento> atuais = new ArrayList<>();
        for (Evento evento : eventos) {
            if (evento.estaOcorrendoAgora()) {
                atuais.add(evento);
            }
        }
        listarEventos(atuais);
    }

    private void listarEventosPassados() {
        System.out.println("\n=== EVENTOS PASSADOS ===");
        List<Evento> passados = new ArrayList<>();
        for (Evento evento : eventos) {
            if (evento.jaOcorreu()) {
                passados.add(evento);
            }
        }
        listarEventos(passados);
    }

    private void listarEventosConfirmados() {
        System.out.println("\n=== EVENTOS CONFIRMADOS ===");
        List<Evento> confirmados = new ArrayList<>();
        for (Evento evento : eventos) {
            if (evento.participa(usuarioAtual.getEmail())) {
                confirmados.add(evento);
            }
        }
        listarEventos(confirmados);
    }

    private void listarEventos(List<Evento> lista) {
        if (lista.isEmpty()) {
            System.out.println("Nenhum evento encontrado.");
            return;
        }

        List<Evento> copia = new ArrayList<>(lista);
        copia.sort(Comparator.comparing(Evento::getHorario));

        for (Evento evento : copia) {
            System.out.println("\n----------------------------");
            System.out.println(evento);
            if (evento.participa(usuarioAtual.getEmail())) {
                System.out.println("Participação confirmada por você.");
            }
        }
    }

    private void confirmarParticipacao() {
        if (eventos.isEmpty()) {
            System.out.println("Não há eventos cadastrados.");
            return;
        }

        listarTodosEventos();
        System.out.print("\nDigite o ID do evento: ");
        int id = lerInteiro();

        Evento evento = buscarEventoPorId(id);
        if (evento == null) {
            System.out.println("Evento não encontrado.");
            return;
        }

        if (evento.jaOcorreu()) {
            System.out.println("Não é possível participar de um evento já encerrado.");
            return;
        }

        evento.adicionarParticipante(usuarioAtual.getEmail());
        salvarEventos();
        System.out.println("Participação confirmada com sucesso.");
    }

    private void cancelarParticipacao() {
        List<Evento> confirmados = new ArrayList<>();
        for (Evento evento : eventos) {
            if (evento.participa(usuarioAtual.getEmail())) {
                confirmados.add(evento);
            }
        }

        if (confirmados.isEmpty()) {
            System.out.println("Você não possui participações confirmadas.");
            return;
        }

        listarEventos(confirmados);
        System.out.print("\nDigite o ID do evento para cancelar: ");
        int id = lerInteiro();

        Evento evento = buscarEventoPorId(id);
        if (evento == null || !evento.participa(usuarioAtual.getEmail())) {
            System.out.println("Evento inválido.");
            return;
        }

        evento.removerParticipante(usuarioAtual.getEmail());
        salvarEventos();
        System.out.println("Participação cancelada com sucesso.");
    }

    private Evento buscarEventoPorId(int id) {
        for (Evento evento : eventos) {
            if (evento.getId() == id) {
                return evento;
            }
        }
        return null;
    }

    private int gerarNovoId() {
        int maiorId = 0;
        for (Evento evento : eventos) {
            if (evento.getId() > maiorId) {
                maiorId = evento.getId();
            }
        }
        return maiorId + 1;
    }

    private void ordenarEventos() {
        eventos.sort(Comparator.comparing(Evento::getHorario));
    }

    private int lerInteiro() {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Digite um número válido: ");
            }
        }
    }

    private LocalDateTime lerDataHora() {
        while (true) {
            try {
                System.out.print("Horário do evento (dd/MM/yyyy HH:mm): ");
                String entrada = scanner.nextLine();
                return LocalDateTime.parse(entrada, INPUT_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("Formato inválido.");
            }
        }
    }

    private void salvarEventos() {
        Path caminho = Paths.get(ARQUIVO);

        try (BufferedWriter writer = Files.newBufferedWriter(caminho)) {
            for (Evento evento : eventos) {
                writer.write(evento.serializar());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Erro ao salvar eventos: " + e.getMessage());
        }
    }

    private void carregarEventos() {
        Path caminho = Paths.get(ARQUIVO);

        if (!Files.exists(caminho)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(caminho)) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (!linha.isBlank()) {
                    Evento evento = Evento.desserializar(linha);
                    if (evento != null) {
                        eventos.add(evento);
                    }
                }
            }
            ordenarEventos();
        } catch (IOException e) {
            System.out.println("Erro ao carregar eventos: " + e.getMessage());
        }
    }
}

public class Main {
    public static void main(String[] args) {
        SistemaEventos sistema = new SistemaEventos();
        sistema.iniciar();
    }
}