package br.com.urbaniza.dao;

import br.com.urbaniza.exception.PersistenciaException;
import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.AnaliseParcelamento;
import br.com.urbaniza.model.Analista;
import br.com.urbaniza.model.Lote;
import br.com.urbaniza.model.Loteamento;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.model.ResultadoCriterio;
import br.com.urbaniza.model.criterio.CriterioAlagamento;
import br.com.urbaniza.model.criterio.CriterioDeclividade;
import br.com.urbaniza.model.criterio.CriterioGeologico;
import br.com.urbaniza.model.criterio.CriterioPreservacao;
import br.com.urbaniza.model.criterio.CriterioVedacao;
import br.com.urbaniza.model.enums.AptidaoParcelamento;
import br.com.urbaniza.model.equipamento.EquipamentoComunitario;
import br.com.urbaniza.model.equipamento.Escola;
import br.com.urbaniza.model.equipamento.UnidadeSaude;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integração contra o PostgreSQL real. Só roda com URBANIZA_IT=true.
 * Todos os registros criados usam o prefixo "IT-" + UUID curto e são removidos ao final,
 * sem tocar nos dados de exemplo da seção C8.
 */
@EnabledIfEnvironmentVariable(named = "URBANIZA_IT", matches = "true")
class DaoIntegracaoTest {

    private final RegiaoDao regiaoDao = new RegiaoDao();
    private final LoteamentoDao loteamentoDao = new LoteamentoDao();
    private final EquipamentoComunitarioDao equipamentoDao = new EquipamentoComunitarioDao();
    private final AnalistaDao analistaDao = new AnalistaDao();
    private final CriterioVedacaoDao criterioDao = new CriterioVedacaoDao();
    private final AnaliseParcelamentoDao analiseDao = new AnaliseParcelamentoDao();

    private final List<Long> analisesCriadas = new ArrayList<>();
    private final List<Long> regioesCriadas = new ArrayList<>();
    private final List<Long> equipamentosCriados = new ArrayList<>();
    private final List<Long> analistasCriados = new ArrayList<>();

    private String prefixo;

    @BeforeEach
    void gerarPrefixo() {
        prefixo = "IT-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @AfterEach
    void limpar() {
        analisesCriadas.forEach(id -> ignorarErro(() -> analiseDao.excluir(id)));
        regioesCriadas.forEach(id -> ignorarErro(() -> regiaoDao.excluir(id)));
        equipamentosCriados.forEach(id -> ignorarErro(() -> equipamentoDao.excluir(id)));
        analistasCriados.forEach(id -> ignorarErro(() -> analistaDao.excluir(id)));
    }

    private static void ignorarErro(Runnable acao) {
        try {
            acao.run();
        } catch (PersistenciaException e) {
            // já removido (ex.: cascata) — nada a limpar
        }
    }

    private Regiao novaRegiao(String sufixo) {
        Regiao regiao = regiaoDao.inserir(new Regiao(null, prefixo + "-" + sufixo, 100_000, 20_000, 30_000, 1_000, 12));
        regioesCriadas.add(regiao.getId());
        return regiao;
    }

    private Analista novoAnalista() {
        Analista analista = analistaDao.inserir(new Analista(null, prefixo + " Analista", prefixo, "Teste"));
        analistasCriados.add(analista.getId());
        return analista;
    }

    @Test
    void cicloCompletoRegiao() {
        Regiao regiao = novaRegiao("regiao");
        assertNotNull(regiao.getId());

        Regiao lida = regiaoDao.buscarPorId(regiao.getId()).orElseThrow();
        assertEquals(regiao.getNome(), lida.getNome());
        assertEquals(1_000, lida.getPopulacaoAtual());
        assertEquals(12.0, lida.getDeclividadeMediaPct());

        lida.setPopulacaoAtual(2_500);
        regiaoDao.atualizar(lida);
        assertEquals(2_500, regiaoDao.buscarPorId(regiao.getId()).orElseThrow().getPopulacaoAtual());

        assertTrue(regiaoDao.listarTodos().stream().anyMatch(r -> r.getId().equals(regiao.getId())));

        PersistenciaException duplicada = assertThrows(PersistenciaException.class,
                () -> regiaoDao.inserir(new Regiao(null, regiao.getNome(), 10, 0, 0, 0, 0)));
        assertTrue(duplicada.getMessage().contains("Já existe"));

        regiaoDao.excluir(regiao.getId());
        assertTrue(regiaoDao.buscarPorId(regiao.getId()).isEmpty());
    }

    @Test
    void checkDoBancoViraMensagemAmigavel() {
        Regiao invalida = new Regiao(null, prefixo + "-check", 100, 80, 0, 0, 0);
        invalida.setAreaImpermeavelM2(50); // verde + impermeável > total
        PersistenciaException e = assertThrows(PersistenciaException.class, () -> regiaoDao.inserir(invalida));
        assertEquals("Valor fora das regras do banco: ck_regiao_soma_areas", e.getMessage());
    }

    @Test
    void cicloCompletoLoteamentoComLotes() {
        Regiao regiao = novaRegiao("lot");
        Loteamento loteamento = new Loteamento(null, regiao.getId(), prefixo + " Loteamento", null, 5_000, 300, false, null);
        loteamento.adicionarLote(new Lote(null, "A", "1", 200, 8));
        loteamento.adicionarLote(new Lote(null, "A", "2", 150, 6));
        loteamentoDao.inserir(loteamento);
        assertNotNull(loteamento.getId());
        assertNotNull(loteamento.getLotes().get(0).getId());

        Loteamento lido = loteamentoDao.buscarPorId(loteamento.getId()).orElseThrow();
        assertEquals(2, lido.getLotes().size());
        assertNull(lido.getDataAprovacao());
        assertEquals(regiao.getId(), lido.getRegiaoId());

        lido.setDataAprovacao(LocalDate.of(2026, 1, 15));
        lido.setLotes(List.of(new Lote(null, "B", "1", 300, 10)));
        loteamentoDao.atualizar(lido);
        Loteamento atualizado = loteamentoDao.buscarPorId(loteamento.getId()).orElseThrow();
        assertEquals(LocalDate.of(2026, 1, 15), atualizado.getDataAprovacao());
        assertEquals(1, atualizado.getLotes().size());
        assertEquals("B", atualizado.getLotes().get(0).getQuadra());

        assertEquals(1, loteamentoDao.listarPorRegiao(regiao.getId()).size());
        assertTrue(loteamentoDao.listarTodos().stream().anyMatch(l -> l.getId().equals(loteamento.getId())));

        loteamentoDao.excluir(loteamento.getId());
        assertTrue(loteamentoDao.buscarPorId(loteamento.getId()).isEmpty());
    }

    @Test
    void loteamentoComLoteInvalidoFazRollback() {
        Regiao regiao = novaRegiao("rollback");
        Loteamento loteamento = new Loteamento(null, regiao.getId(), prefixo + " Rollback", null, 5_000, 10, false, null);
        loteamento.adicionarLote(new Lote(null, "A", "1", 200, 8));
        loteamento.adicionarLote(new Lote(null, "A", "2", 100, 8)); // viola ck_lote_area_minima
        PersistenciaException e = assertThrows(PersistenciaException.class, () -> loteamentoDao.inserir(loteamento));
        assertTrue(e.getMessage().contains("ck_lote_area_minima"));
        assertTrue(loteamentoDao.listarPorRegiao(regiao.getId()).isEmpty(), "a transação deveria ter sido desfeita");
    }

    @Test
    void loteamentoEmRegiaoInexistenteEhRecusado() {
        Loteamento loteamento = new Loteamento(null, -1L, prefixo, null, 5_000, 10, false, null);
        loteamento.adicionarLote(new Lote(null, "A", "1", 200, 8));
        PersistenciaException e = assertThrows(PersistenciaException.class, () -> loteamentoDao.inserir(loteamento));
        assertTrue(e.getMessage().contains("RN17"));
    }

    @Test
    void cicloCompletoEscolaEUnidadeSaude() {
        Regiao regiao = novaRegiao("equip");
        Escola escola = new Escola(null, regiao.getId(), prefixo + " Escola", 500, 16, "Fundamental");
        equipamentoDao.inserir(escola);
        equipamentosCriados.add(escola.getId());
        UnidadeSaude ubs = new UnidadeSaude(null, null, prefixo + " UBS", 3, 3_500, "UBS");
        equipamentoDao.inserir(ubs);
        equipamentosCriados.add(ubs.getId());

        EquipamentoComunitario escolaLida = equipamentoDao.buscarPorId(escola.getId()).orElseThrow();
        Escola e = assertInstanceOf(Escola.class, escolaLida);
        assertEquals(16.0, e.getPercentualIdadeEscolar());
        assertEquals("Fundamental", e.getNivelEnsino());
        assertEquals(regiao.getId(), e.getRegiaoId());

        UnidadeSaude u = assertInstanceOf(UnidadeSaude.class, equipamentoDao.buscarPorId(ubs.getId()).orElseThrow());
        assertNull(u.getRegiaoId());
        assertEquals(3_500, u.getHabitantesPorEquipe());

        e.setCapacidadeInstalada(650);
        e.setNivelEnsino("Médio");
        equipamentoDao.atualizar(e);
        Escola escolaAtualizada = (Escola) equipamentoDao.buscarPorId(escola.getId()).orElseThrow();
        assertEquals(650, escolaAtualizada.getCapacidadeInstalada());
        assertEquals("Médio", escolaAtualizada.getNivelEnsino());

        u.setRegiaoId(regiao.getId());
        u.setHabitantesPorEquipe(3_000);
        equipamentoDao.atualizar(u);
        assertEquals(2, equipamentoDao.listarPorRegiao(regiao.getId()).size());
        assertTrue(equipamentoDao.listarTodos().size() >= 2);

        UnidadeSaude tipoTrocado = new UnidadeSaude(escola.getId(), null, "x", 1, 1, "UBS");
        RegraNegocioException erroTipo = assertThrows(RegraNegocioException.class,
                () -> equipamentoDao.atualizar(tipoTrocado));
        assertEquals("Não é permitido alterar o tipo; exclua e cadastre novamente", erroTipo.getMessage());

        equipamentoDao.excluir(escola.getId());
        equipamentoDao.excluir(ubs.getId());
        assertTrue(equipamentoDao.buscarPorId(escola.getId()).isEmpty());
        assertTrue(equipamentoDao.buscarPorId(ubs.getId()).isEmpty());
    }

    @Test
    void cicloCompletoAnalista() {
        Analista analista = novoAnalista();
        Analista lido = analistaDao.buscarPorId(analista.getId()).orElseThrow();
        assertEquals(prefixo, lido.getMatricula());
        lido.setSetor("Outro setor");
        analistaDao.atualizar(lido);
        assertEquals("Outro setor", analistaDao.buscarPorId(analista.getId()).orElseThrow().getSetor());
        assertTrue(analistaDao.listarTodos().stream().anyMatch(a -> a.getId().equals(analista.getId())));
        PersistenciaException duplicada = assertThrows(PersistenciaException.class,
                () -> analistaDao.inserir(new Analista(null, "Outro", prefixo, "x")));
        assertTrue(duplicada.getMessage().contains("matrícula"));
        analistaDao.excluir(analista.getId());
        assertTrue(analistaDao.buscarPorId(analista.getId()).isEmpty());
    }

    @Test
    void cicloCompletoDosQuatroTiposDeCriterio() {
        Regiao regiao = novaRegiao("crit");
        List<CriterioVedacao> criterios = List.of(
                new CriterioAlagamento(null, regiao.getId(), prefixo + "-ALG", null, false),
                new CriterioDeclividade(null, regiao.getId(), prefixo + "-DEC", null, 30),
                new CriterioGeologico(null, regiao.getId(), prefixo + "-GEO", null, "ADEQUADO: teste"),
                new CriterioPreservacao(null, regiao.getId(), prefixo + "-PRS", null, false));
        for (CriterioVedacao c : criterios) {
            criterioDao.inserir(c);
            assertNotNull(c.getId());
            CriterioVedacao lido = criterioDao.buscarPorId(c.getId()).orElseThrow();
            assertEquals(c.getClass(), lido.getClass());
            assertEquals(c.getTipo(), lido.getTipo());
            assertEquals(c.getFundamentoLegal(), lido.getFundamentoLegal());
        }

        CriterioAlagamento alg = (CriterioAlagamento) criterioDao.buscarPorId(criterios.get(0).getId()).orElseThrow();
        alg.setSaneamentoExecutado(true);
        criterioDao.atualizar(alg);
        assertTrue(((CriterioAlagamento) criterioDao.buscarPorId(alg.getId()).orElseThrow()).isSaneamentoExecutado());

        CriterioDeclividade dec = (CriterioDeclividade) criterioDao.buscarPorId(criterios.get(1).getId()).orElseThrow();
        dec.setDeclividadeLimite(25);
        criterioDao.atualizar(dec);
        assertEquals(25.0, ((CriterioDeclividade) criterioDao.buscarPorId(dec.getId()).orElseThrow()).getDeclividadeLimite());

        CriterioGeologico geo = (CriterioGeologico) criterioDao.buscarPorId(criterios.get(2).getId()).orElseThrow();
        geo.setLaudoGeotecnico("INADEQUADO: alterado");
        criterioDao.atualizar(geo);
        assertEquals("INADEQUADO: alterado",
                ((CriterioGeologico) criterioDao.buscarPorId(geo.getId()).orElseThrow()).getLaudoGeotecnico());

        CriterioPreservacao prs = (CriterioPreservacao) criterioDao.buscarPorId(criterios.get(3).getId()).orElseThrow();
        prs.setAreaProtegida(true);
        criterioDao.atualizar(prs);
        assertTrue(((CriterioPreservacao) criterioDao.buscarPorId(prs.getId()).orElseThrow()).isAreaProtegida());

        assertEquals(4, criterioDao.listarPorRegiao(regiao.getId()).size());
        assertTrue(criterioDao.listarTodos().size() >= 4);

        for (CriterioVedacao c : criterios) {
            criterioDao.excluir(c.getId());
            assertTrue(criterioDao.buscarPorId(c.getId()).isEmpty());
        }
    }

    @Test
    void analiseGravadaELidaComResultadosEBloqueiaExclusao() {
        Regiao regiao = novaRegiao("analise");
        Analista analista = novoAnalista();
        CriterioVedacao alagamento = criterioDao.inserir(
                new CriterioAlagamento(null, regiao.getId(), prefixo + "-A1", null, false));
        CriterioVedacao preservacao = criterioDao.inserir(
                new CriterioPreservacao(null, regiao.getId(), prefixo + "-P1", null, false));

        AnaliseParcelamento analise = new AnaliseParcelamento(regiao, analista, List.of(alagamento, preservacao));
        analise.executar();
        analiseDao.inserir(analise);
        analisesCriadas.add(analise.getId());
        assertNotNull(analise.getId());

        AnaliseParcelamento lida = analiseDao.buscarPorId(analise.getId()).orElseThrow();
        assertEquals(AptidaoParcelamento.APTA_COM_RESTRICAO, lida.getResultado());
        assertEquals(analise.getVersaoDados(), lida.getVersaoDados());
        assertEquals(analise.getDataEmissao(), lida.getDataEmissao());
        assertEquals(analise.getParecerTextual(), lida.getParecerTextual());
        assertEquals(analise.getIndiceAreaVerdeApurado(), lida.getIndiceAreaVerdeApurado());
        assertEquals(regiao.getNome(), lida.getRegiao().getNome());
        assertEquals(analista.getMatricula(), lida.getAnalista().getMatricula());
        assertEquals(2, lida.getResultados().size());
        ResultadoCriterio r1 = lida.getResultados().get(0);
        assertInstanceOf(CriterioAlagamento.class, r1.criterio());
        assertFalse(r1.atendido());
        assertNotNull(r1.descricaoImpedimento());
        ResultadoCriterio r2 = lida.getResultados().get(1);
        assertInstanceOf(CriterioPreservacao.class, r2.criterio());
        assertTrue(r2.atendido());
        assertNull(r2.descricaoImpedimento());
        assertEquals(1, analiseDao.listarPorRegiao(regiao.getId()).size());
        assertTrue(analiseDao.listarTodos().stream().anyMatch(a -> a.getId().equals(analise.getId())));

        PersistenciaException bloqueioRegiao = assertThrows(PersistenciaException.class,
                () -> regiaoDao.excluir(regiao.getId()));
        assertEquals("Não é possível excluir: existem análises emitidas para esta região (rastreabilidade RN16)",
                bloqueioRegiao.getMessage());
        assertThrows(PersistenciaException.class, () -> criterioDao.excluir(alagamento.getId()));
        assertThrows(PersistenciaException.class, () -> analistaDao.excluir(analista.getId()));

        analiseDao.excluir(analise.getId());
        assertTrue(analiseDao.buscarPorId(analise.getId()).isEmpty());
    }

    @Test
    void excluirRegiaoDeixaEquipamentoSemRegiaoEApagaComposicao() {
        Regiao regiao = novaRegiao("agregacao");
        Escola escola = new Escola(null, regiao.getId(), prefixo + " Escola agregada", 100, 20, "Fundamental");
        equipamentoDao.inserir(escola);
        equipamentosCriados.add(escola.getId());
        Loteamento loteamento = new Loteamento(null, regiao.getId(), prefixo + " L", null, 5_000, 10, false, null);
        loteamento.adicionarLote(new Lote(null, "A", "1", 200, 8));
        loteamentoDao.inserir(loteamento);
        CriterioVedacao criterio = criterioDao.inserir(
                new CriterioPreservacao(null, regiao.getId(), prefixo + "-PX", null, false));

        regiaoDao.excluir(regiao.getId());

        EquipamentoComunitario lido = equipamentoDao.buscarPorId(escola.getId()).orElseThrow();
        assertNull(lido.getRegiaoId(), "agregação: o equipamento sobrevive com regiao_id NULL");
        assertTrue(loteamentoDao.buscarPorId(loteamento.getId()).isEmpty(), "composição: loteamento removido em cascata");
        assertTrue(criterioDao.buscarPorId(criterio.getId()).isEmpty(), "critério removido em cascata");
    }
}
