package br.com.urbaniza.service;

import br.com.urbaniza.dao.CrudDao;
import br.com.urbaniza.dao.CrudDaoPorRegiao;
import br.com.urbaniza.dao.LoteamentoDao;
import br.com.urbaniza.dao.RegiaoDao;
import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.Lote;
import br.com.urbaniza.model.Loteamento;
import br.com.urbaniza.model.ParametrosUrbanisticos;
import br.com.urbaniza.model.Regiao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Regras de cadastro de loteamentos (RF02, RF06):
 * <ul>
 *   <li>RN17 — a região precisa estar previamente cadastrada;</li>
 *   <li>ao menos um lote;</li>
 *   <li>RF06 / RN05 — todos os lotes com área &gt;= 125 m² e frente &gt;= 5 m (validado em Java ANTES do banco);</li>
 *   <li>soma das áreas dos lotes &lt;= área da gleba.</li>
 * </ul>
 */
public class LoteamentoService {

    private static final int TAMANHO_MAXIMO_DENOMINACAO = 150;
    private static final int TAMANHO_MAXIMO_QUADRA_NUMERO = 20;
    private static final Locale LOCALE_BR = Locale.of("pt", "BR");

    private final CrudDaoPorRegiao<Loteamento> loteamentoDao;
    private final CrudDao<Regiao> regiaoDao;

    /** Construtor padrão com os DAOs JDBC reais. */
    public LoteamentoService() {
        this(new LoteamentoDao(), new RegiaoDao());
    }

    /** Construtor com injeção dos DAOs (permite testar com DAOs falsos). */
    public LoteamentoService(CrudDaoPorRegiao<Loteamento> loteamentoDao, CrudDao<Regiao> regiaoDao) {
        this.loteamentoDao = loteamentoDao;
        this.regiaoDao = regiaoDao;
    }

    /** Cadastra um loteamento com seus lotes, após validar RN17, RF06/RN05 e a área da gleba. */
    public Loteamento inserir(Loteamento loteamento) {
        validar(loteamento);
        return loteamentoDao.inserir(loteamento);
    }

    public Loteamento buscar(long id) {
        return loteamentoDao.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Loteamento " + id + " não cadastrado."));
    }

    public List<Loteamento> listar() {
        return loteamentoDao.listarTodos();
    }

    public List<Loteamento> listarPorRegiao(long regiaoId) {
        return loteamentoDao.listarPorRegiao(regiaoId);
    }

    /** Atualiza o loteamento e substitui os seus lotes, com as mesmas validações do cadastro. */
    public void atualizar(Loteamento loteamento) {
        Validacao.exigirId(loteamento.getId(), "do loteamento");
        buscar(loteamento.getId());
        validar(loteamento);
        loteamentoDao.atualizar(loteamento);
    }

    public void excluir(long id) {
        buscar(id);
        loteamentoDao.excluir(id);
    }

    /** RN13 — registra a aprovação do loteamento (a partir daí ele entra na população projetada se não ocupado). */
    public void aprovar(long id, LocalDate dataAprovacao) {
        if (dataAprovacao == null) {
            throw new RegraNegocioException("Informe a data de aprovação.");
        }
        if (dataAprovacao.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data de aprovação não pode ser futura.");
        }
        Loteamento loteamento = buscar(id);
        loteamento.setDataAprovacao(dataAprovacao);
        loteamentoDao.atualizar(loteamento);
    }

    /** RN13 — marca o loteamento como ocupado (sai da população projetada; já está na população atual). */
    public void marcarComoOcupado(long id) {
        Loteamento loteamento = buscar(id);
        if (!loteamento.isAprovado()) {
            throw new RegraNegocioException("Somente loteamentos aprovados podem ser marcados como ocupados.");
        }
        loteamento.setOcupado(true);
        loteamentoDao.atualizar(loteamento);
    }

    private void validar(Loteamento loteamento) {
        if (loteamento == null) {
            throw new RegraNegocioException("Informe os dados do loteamento.");
        }
        validarRegiao(loteamento.getRegiaoId());
        loteamento.setDenominacao(Validacao.textoObrigatorio(loteamento.getDenominacao(),
                "a denominação do loteamento", TAMANHO_MAXIMO_DENOMINACAO));
        if (!(loteamento.getAreaGlebaM2() > 0)) {
            throw new RegraNegocioException("A área da gleba deve ser maior que zero.");
        }
        List<Lote> lotes = loteamento.getLotes();
        if (lotes.isEmpty()) {
            throw new RegraNegocioException("O loteamento deve ter ao menos um lote.");
        }
        validarIdentificacaoLotes(lotes);
        validarDimensoesLotes(lotes);
        double areaLoteavel = loteamento.calcularAreaLoteavel();
        if (areaLoteavel > loteamento.getAreaGlebaM2()) {
            throw new RegraNegocioException("A soma das áreas dos lotes (" + decimal(areaLoteavel)
                    + " m²) é maior que a área da gleba (" + decimal(loteamento.getAreaGlebaM2()) + " m²).");
        }
    }

    /** RN17 — o loteamento só pode ser vinculado a região previamente cadastrada. */
    private void validarRegiao(Long regiaoId) {
        if (regiaoId == null || regiaoDao.buscarPorId(regiaoId).isEmpty()) {
            throw new RegraNegocioException("Região " + regiaoId
                    + " não cadastrada. Cadastre a região antes do loteamento (RN17)");
        }
    }

    private static void validarIdentificacaoLotes(List<Lote> lotes) {
        Set<String> identificacoes = new HashSet<>();
        for (Lote lote : lotes) {
            lote.setQuadra(Validacao.textoObrigatorio(lote.getQuadra(), "a quadra do lote", TAMANHO_MAXIMO_QUADRA_NUMERO));
            lote.setNumero(Validacao.textoObrigatorio(lote.getNumero(), "o número do lote", TAMANHO_MAXIMO_QUADRA_NUMERO));
            if (!identificacoes.add(lote.getQuadra() + "/" + lote.getNumero())) {
                throw new RegraNegocioException("Lote repetido: quadra " + lote.getQuadra()
                        + ", número " + lote.getNumero() + ".");
            }
        }
    }

    /** RF06 / RN05 — rejeita citando quadra e número de CADA lote abaixo dos mínimos. */
    private static void validarDimensoesLotes(List<Lote> lotes) {
        List<String> invalidos = new ArrayList<>();
        for (Lote lote : lotes) {
            if (!lote.atendeDimensoesMinimas()) {
                invalidos.add("quadra " + lote.getQuadra() + ", número " + lote.getNumero()
                        + " (área " + decimal(lote.getAreaM2()) + " m², frente " + decimal(lote.getFrenteM()) + " m)");
            }
        }
        if (!invalidos.isEmpty()) {
            throw new RegraNegocioException("Lote(s) abaixo das dimensões mínimas de "
                    + decimal(ParametrosUrbanisticos.LOTE_AREA_MINIMA_M2) + " m² de área e "
                    + decimal(ParametrosUrbanisticos.LOTE_FRENTE_MINIMA_M)
                    + " m de frente (RF06/RN05 — Lei 6.766/79, art. 4º, II): " + String.join("; ", invalidos));
        }
    }

    private static String decimal(double valor) {
        return String.format(LOCALE_BR, "%,.2f", valor);
    }
}
