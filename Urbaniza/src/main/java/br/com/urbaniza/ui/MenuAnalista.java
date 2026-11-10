package br.com.urbaniza.ui;

import br.com.urbaniza.model.Analista;
import br.com.urbaniza.service.AnalistaService;

/**
 * CRUD dos analistas responsáveis pelas análises (RF07).
 */
public class MenuAnalista extends MenuCrud<Analista> {

    private final AnalistaService analistaService;

    public MenuAnalista(Entrada entrada) {
        this(entrada, new AnalistaService());
    }

    public MenuAnalista(Entrada entrada, AnalistaService analistaService) {
        super(entrada);
        this.analistaService = analistaService;
    }

    @Override
    protected String titulo() {
        return "ANALISTAS";
    }

    @Override
    protected String resumir(Analista a) {
        return String.format("%4d | %-30s | matrícula %-12s | %s", a.getId(), a.getNome(), a.getMatricula(), a.getSetor());
    }

    @Override
    protected void listar() {
        imprimirLista(analistaService.listar(), "Nenhum analista cadastrado.");
    }

    @Override
    protected void detalhar() {
        Analista a = analistaService.buscar(entrada.lerId("Id do analista"));
        System.out.println("Id: " + a.getId());
        System.out.println("Nome: " + a.getNome());
        System.out.println("Matrícula: " + a.getMatricula());
        System.out.println("Setor: " + a.getSetor());
    }

    @Override
    protected void cadastrar() {
        Analista a = new Analista();
        a.setNome(entrada.lerTexto("Nome"));
        a.setMatricula(entrada.lerTexto("Matrícula"));
        a.setSetor(entrada.lerTexto("Setor"));
        analistaService.inserir(a);
        System.out.println("Analista cadastrado com id " + a.getId() + ".");
    }

    @Override
    protected void atualizar() {
        Analista a = analistaService.buscar(entrada.lerId("Id do analista"));
        System.out.println("Pressione ENTER para manter o valor atual.");
        a.setNome(entrada.lerTexto("Nome", a.getNome()));
        a.setMatricula(entrada.lerTexto("Matrícula", a.getMatricula()));
        a.setSetor(entrada.lerTexto("Setor", a.getSetor()));
        analistaService.atualizar(a);
        System.out.println("Analista atualizado.");
    }

    @Override
    protected void excluir() {
        Analista a = analistaService.buscar(entrada.lerId("Id do analista"));
        if (confirmarExclusao("\"" + a.getNome() + "\"")) {
            analistaService.excluir(a.getId());
            System.out.println("Analista excluído.");
        }
    }
}
