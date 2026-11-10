package br.com.urbaniza.service;

import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.AnaliseParcelamento;
import br.com.urbaniza.model.Analista;
import br.com.urbaniza.model.DadosExemploC8;
import br.com.urbaniza.model.Loteamento;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.model.criterio.CriterioVedacao;
import br.com.urbaniza.model.enums.AptidaoParcelamento;
import br.com.urbaniza.model.enums.ClassificacaoIAV;
import br.com.urbaniza.model.enums.PotencialIlhaCalor;
import br.com.urbaniza.model.equipamento.EquipamentoComunitario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnaliseServiceTest {

    private DaoFalsoEmMemoria<Regiao> regiaoDao;
    private DaoFalsoEmMemoria<Loteamento> loteamentoDao;
    private DaoFalsoEmMemoria<EquipamentoComunitario> equipamentoDao;
    private DaoFalsoEmMemoria<Analista> analistaDao;
    private DaoFalsoEmMemoria<CriterioVedacao> criterioDao;
    private DaoFalsoEmMemoria<AnaliseParcelamento> analiseDao;
    private AnaliseService service;

    /** Coloca uma região da C8 (e seus loteamentos/equipamentos) nos DAOs falsos. */
    private void carregarRegiao(Regiao regiao, List<CriterioVedacao> criterios) {
        regiao.getLoteamentos().forEach(loteamentoDao::carregar);
        regiao.getEquipamentos().forEach(equipamentoDao::carregar);
        criterios.forEach(criterioDao::carregar);
        regiao.setLoteamentos(null);
        regiao.setEquipamentos(null);
        regiaoDao.carregar(regiao);
    }

    @BeforeEach
    void preparar() {
        regiaoDao = new DaoFalsoEmMemoria<>(Regiao::getId, Regiao::setId, Regiao::getId);
        loteamentoDao = new DaoFalsoEmMemoria<>(Loteamento::getId, Loteamento::setId, Loteamento::getRegiaoId);
        equipamentoDao = new DaoFalsoEmMemoria<>(EquipamentoComunitario::getId, EquipamentoComunitario::setId,
                EquipamentoComunitario::getRegiaoId);
        analistaDao = new DaoFalsoEmMemoria<>(Analista::getId, Analista::setId, a -> null);
        criterioDao = new DaoFalsoEmMemoria<>(CriterioVedacao::getId, CriterioVedacao::setId,
                CriterioVedacao::getRegiaoId);
        analiseDao = new DaoFalsoEmMemoria<>(AnaliseParcelamento::getId, AnaliseParcelamento::setId,
                a -> a.getRegiao().getId());
        analistaDao.carregar(DadosExemploC8.rafaelMoreira());
        carregarRegiao(DadosExemploC8.regiaoSul(), DadosExemploC8.criteriosSul());
        carregarRegiao(DadosExemploC8.regiaoNorte(), DadosExemploC8.criteriosNorte());
        equipamentoDao.carregar(DadosExemploC8.escolaRuralItinerante());
        service = new AnaliseService(analiseDao, regiaoDao, loteamentoDao, equipamentoDao, analistaDao, criterioDao);
    }

    @Test
    void regiaoSulEhInaptaEAnaliseVaiAoDaoComVersao() {
        AnaliseParcelamento analise = service.emitir(3L, 1L);

        assertEquals(AptidaoParcelamento.INAPTA, analise.getResultado());
        assertEquals(1, analiseDao.getInseridos().size());
        AnaliseParcelamento gravada = analiseDao.getInseridos().get(0);
        assertEquals(analise, gravada);
        assertNotNull(gravada.getId());
        assertNotNull(gravada.getVersaoDados());
        assertTrue(gravada.getVersaoDados().matches("v-[0-9a-f]{12}"));
        assertNotNull(gravada.getDataEmissao());
        assertEquals("Rafael Moreira", gravada.getAnalista().getNome());
        assertEquals(3, gravada.getResultados().size());
        gravada.getResultados().forEach(rc -> assertFalse(rc.atendido()));
        assertEquals(250.0, gravada.getIndiceAreaVerdeApurado(), 0.01);
        assertTrue(gravada.getParecerTextual().contains("6.000"));
        assertEquals(List.of(gravada), service.listarPorRegiao(3L));
    }

    @Test
    void regiaoNorteEhAptaComRestricao() {
        AnaliseParcelamento analise = service.emitir(1L, 1L);
        assertEquals(AptidaoParcelamento.APTA_COM_RESTRICAO, analise.getResultado());
        assertEquals(8.82, analise.getIndiceAreaVerdeApurado(), 0.01);
    }

    @Test
    void analistaOuRegiaoInexistentesSaoRecusados() {
        assertThrows(RegraNegocioException.class, () -> service.emitir(3L, 77L));
        assertThrows(RegraNegocioException.class, () -> service.emitir(77L, 1L));
        assertTrue(analiseDao.getInseridos().isEmpty());
    }

    @Test
    void indicadoresEAtendimentoDaRegiaoNorte() {
        RegiaoService regiaoService = new RegiaoService(regiaoDao, loteamentoDao, equipamentoDao);
        IndicadoresRegiao ind = regiaoService.calcularIndicadores(1L);
        assertEquals(18_000, ind.populacaoAtual());
        assertEquals(20_400, ind.populacaoProjetada());
        assertEquals(8.82, ind.iav(), 0.01);
        assertEquals(ClassificacaoIAV.DEFICITARIO, ind.classificacaoIav());
        assertEquals(0.45, ind.taxaImpermeabilizacao(), 1e-9);
        assertEquals(PotencialIlhaCalor.ALTO, ind.potencialIlhaCalor());

        EquipamentoService equipamentoService = new EquipamentoService(equipamentoDao, regiaoDao, loteamentoDao);
        List<SituacaoAtendimento> situacoes = equipamentoService.calcularAtendimentoDaRegiao(1L);
        assertEquals(2, situacoes.size());
        assertEquals(3_264, situacoes.get(0).demanda());
        assertTrue(situacoes.get(0).deficit());
        assertEquals(6, situacoes.get(1).demanda());

        Optional<SituacaoAtendimento> semRegiao = equipamentoService.calcularAtendimento(6L);
        assertTrue(semRegiao.isEmpty(), "equipamento sem região => índice não aplicável");
    }

    @Test
    void laudoGeologicoEhNormalizadoOuRecusado() {
        assertEquals("INADEQUADO: encosta", CriterioVedacaoService.normalizarLaudo("inadequado: encosta"));
        assertEquals("ADEQUADO", CriterioVedacaoService.normalizarLaudo(" adequado "));
        assertThrows(RegraNegocioException.class, () -> CriterioVedacaoService.normalizarLaudo("solo bom"));
    }
}
