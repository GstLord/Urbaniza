package br.com.urbaniza.model;

/**
 * Servidor responsável pela emissão das análises (RF07 / RN16).
 */
public class Analista {

    private Long id;
    private String nome;
    private String matricula;
    private String setor;

    public Analista() {
    }

    public Analista(Long id, String nome, String matricula, String setor) {
        this.id = id;
        this.nome = nome;
        this.matricula = matricula;
        this.setor = setor;
    }

    public Long getId() {
        return id;
    }

    public final void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public final void setNome(String nome) {
        this.nome = nome;
    }

    public String getMatricula() {
        return matricula;
    }

    public final void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getSetor() {
        return setor;
    }

    public final void setSetor(String setor) {
        this.setor = setor;
    }

    @Override
    public String toString() {
        return "Analista[id=" + id + ", nome=" + nome + ", matricula=" + matricula + ", setor=" + setor + "]";
    }
}
