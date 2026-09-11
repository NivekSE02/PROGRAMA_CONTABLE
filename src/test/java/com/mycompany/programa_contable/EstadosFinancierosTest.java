package com.mycompany.programa_contable;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.BalanceGeneralDTO;
import com.mycompany.programa_contable.model.BalanzaComprobacionDTO;
import com.mycompany.programa_contable.model.EstadoResultadosDTO;
import com.mycompany.programa_contable.model.MayorCuenta;
import com.mycompany.programa_contable.service.MayorizacionService;
import com.mycompany.programa_contable.service.ReportesFinancierosService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para Mayorización Automática y Estados Financieros Automáticos:
 * 1. Mayorización en tiempo real sin cálculos manuales
 * 2. Balance General: Código 1 (Activo) = Código 2 (Pasivo) + Código 3 (Capital Contable)
 * 3. Estado de Resultados: Código 5 (Ingresos) - Código 4 (Costos y Gastos) = Utilidad
 */
public class EstadosFinancierosTest {

    private static MayorizacionService mayorizacionService;
    private static ReportesFinancierosService reportesService;

    @BeforeAll
    static void setUp() {
        DatabaseManager.getInstance().resetDatabase();
        mayorizacionService = new MayorizacionService();
        reportesService = new ReportesFinancierosService(mayorizacionService);
    }

    @Test
    @DisplayName("Mayorización Automática: Consolidación de cuentas con saldo deudor o acreedor")
    void testMayorizacionAutomatica() {
        List<MayorCuenta> mayores = mayorizacionService.obtenerMayorizacionCompleta();
        assertFalse(mayores.isEmpty(), "La lista de cuentas mayorizadas no debe estar vacía");

        // Verificar cuenta de bancos (110103)
        MayorCuenta bancos = mayorizacionService.obtenerMayorDeCuenta("110103");
        assertNotNull(bancos, "La cuenta de bancos debe existir");
        assertTrue(bancos.getTotalDebe() > 0, "Bancos debe tener movimientos al Debe");
        assertTrue(bancos.getSaldoDeudor() > 0, "Bancos es de naturaleza deudora y debe tener saldo deudor");
        assertEquals(0.0, bancos.getSaldoAcreedor(), 0.001, "Bancos no debe tener saldo acreedor");
    }

    @Test
    @DisplayName("Estado de Resultados Dinámico: Código 5 (Ingresos) - Código 4 (Costos/Gastos) = Utilidad")
    void testEstadoResultados() {
        EstadoResultadosDTO er = reportesService.generarEstadoResultados();
        assertNotNull(er);

        assertTrue(er.getTotalIngresos() > 0, "Debe haber ingresos registrados (código 5)");
        assertTrue(er.getTotalCostosYGastos() > 0, "Debe haber costos/gastos registrados (código 4)");

        // Comprobación exacta de la fórmula exigida:
        double utilidadCalculada = er.getTotalIngresos() - er.getTotalCostosYGastos();
        assertEquals(utilidadCalculada, er.getUtilidadNeta(), 0.01,
            "La utilidad neta debe ser estrictamente Total Ingresos (5) - Total Costos y Gastos (4)");
    }

    @Test
    @DisplayName("Balance General Dinámico: Código 1 (Activo) = Código 2 (Pasivo) + Código 3 (Capital)")
    void testBalanceGeneralCuadre() {
        BalanceGeneralDTO bg = reportesService.generarBalanceGeneral();
        assertNotNull(bg);

        assertTrue(bg.getTotalActivo() > 0, "El total de activos debe ser mayor a cero");
        assertTrue(bg.getTotalPasivoMasCapital() > 0, "El total de pasivo + capital debe ser mayor a cero");

        // Comprobación exacta de la Ecuación Contable: Activo = Pasivo + Capital
        assertTrue(bg.isCuadrado(),
            "El Balance General debe cuadrar perfectamente. Activo: " + bg.getTotalActivo() +
            ", Pasivo+Capital: " + bg.getTotalPasivoMasCapital() + ", Diferencia: " + bg.getDiferencia());
        assertEquals(0.0, bg.getDiferencia(), 0.01, "La diferencia en el balance general debe ser cero");
    }

    @Test
    @DisplayName("Balanza de Comprobación: Sumas de movimientos y saldos exactamente iguales")
    void testBalanzaComprobacion() {
        BalanzaComprobacionDTO balanza = reportesService.generarBalanzaComprobacion();
        assertNotNull(balanza);
        assertTrue(balanza.isCuadrada(), "La Balanza de Comprobación de sumas y saldos debe estar cuadrada");
        assertEquals(balanza.getTotalMovimientoDebe(), balanza.getTotalMovimientoHaber(), 0.01, "Movimiento Debe debe ser igual a Movimiento Haber");
        assertEquals(balanza.getTotalSaldoDeudor(), balanza.getTotalSaldoAcreedor(), 0.01, "Saldo Deudor debe ser igual a Saldo Acreedor");
    }
}
