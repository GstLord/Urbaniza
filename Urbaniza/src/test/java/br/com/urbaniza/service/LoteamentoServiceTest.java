package br.com.urbaniza.service;

import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.Lote;
import br.com.urbaniza.model.Loteamento;
import br.com.urbaniza.model.Regiao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoteamentoServiceTest {

    private DaoFalsoEmMemoria<Loteamento> loteamentoDao;
    private LoteamentoService service;

    @BeforeEach
    void preparar() {
        DaoFalsoEmMemoria<Regiao> regiaoDao = new DaoFalsoEmMemoria<>(Regiao::getId, Regiao::setId, Regiao::getId);
        regiaoDao.carregar(new Regiao(1L, "Região Norte - Jardim das Águas", 2_000_000, 180_000, 900_000, 18_000, 8));
        loteamentoDao = new DaoFalsoEmMemoria<>(Loteamento::getId, Loteamento::setId, Loteamento::getRegiaoId);
        service = new LoteamentoService(loteamentoDao, regiaoDao);
    }

    private static Loteamento loteamento(Long regiaoId) {
        return new Loteamento(null, regiaoId, "Residencial Teste", null, 10_000, 200, false, null);
    }

    @Test
    void regiaoInexistenteEhRecusada() {
        Loteamento l = loteamento(99L);
        l.adicionarLote(new Lote(null, "A", "1", 200, 8));
        RegraNegocioException e = assertThrows(RegraNegocioException.class, () -> service.inserir(l));
        assertEquals("Região 99 não cadastrada. Cadastre a região antes do loteamento (RN17)", e.getMessage());
        assertTrue(loteamentoDao.getInseridos().isEmpty());
    }

    @Test
    void loteDe120m2EhRecusadoCitandoOLote() {
        Loteamento l = loteamento(1L);
        l.adicionarLote(new Lote(null, "A", "1", 200, 8));
        l.adicionarLote(new Lote(null, "B", "7", 120, 8));
        RegraNegocioException e = assertThrows(RegraNegocioException.class, () -> service.inserir(l));
        assertTrue(e.getMessage().contains("quadra B, número 7"), e.getMessage());
        assertTrue(e.getMessage().contains("125,00 m²"), e.getMessage());
        assertTrue(e.getMessage().contains("5,00 m"), e.getMessage());
        assertTrue(e.getMessage().contains("RN05"), e.getMessage());
        assertTrue(loteamentoDao.getInseridos().isEmpty());
    }

    @Test
    void loteamentoSemLotesEhRecusado() {
        RegraNegocioException e = assertThrows(RegraNegocioException.class, () -> service.inserir(loteamento(1L)));
        assertEquals("O loteamento deve ter ao menos um lote.", e.getMessage());
    }

    @Test
    void somaDosLotesMaiorQueAGlebaEhRecusada() {
        Loteamento l = new Loteamento(null, 1L, "Pequeno", null, 300, 10, false, null);
        l.adicionarLote(new Lote(null, "A", "1", 200, 8));
        l.adicionarLote(new Lote(null, "A", "2", 200, 8));
        RegraNegocioException e = assertThrows(RegraNegocioException.class, () -> service.inserir(l));
        assertTrue(e.getMessage().contains("área da gleba"), e.getMessage());
    }

    @Test
    void casoValidoChamaInserir() {
        Loteamento l = loteamento(1L);
        l.adicionarLote(new Lote(null, "A", "1", 125, 5));
        l.adicionarLote(new Lote(null, "A", "2", 300, 12));
        Loteamento salvo = service.inserir(l);
        assertEquals(1, loteamentoDao.getInseridos().size());
        assertEquals(l, loteamentoDao.getInseridos().get(0));
        assertEquals(1000L, salvo.getId());
    }

    @Test
    void aprovarEMarcarComoOcupado() {
        Loteamento l = loteamento(1L);
        l.adicionarLote(new Lote(null, "A", "1", 200, 8));
        long id = service.inserir(l).getId();
        assertThrows(RegraNegocioException.class, () -> service.marcarComoOcupado(id));
        service.aprovar(id, LocalDate.of(2026, 5, 1));
        service.marcarComoOcupado(id);
        Loteamento atualizado = service.buscar(id);
        assertEquals(LocalDate.of(2026, 5, 1), atualizado.getDataAprovacao());
        assertTrue(atualizado.isOcupado());
        assertThrows(RegraNegocioException.class, () -> service.aprovar(id, LocalDate.now().plusDays(1)));
    }
}
