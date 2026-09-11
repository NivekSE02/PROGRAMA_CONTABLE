package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.model.BalanceGeneralDTO;
import com.mycompany.programa_contable.model.BalanzaComprobacionDTO;
import com.mycompany.programa_contable.model.EstadoResultadosDTO;
import com.mycompany.programa_contable.model.MayorCuenta;
import com.mycompany.programa_contable.model.NaturalezaCuenta;
import com.mycompany.programa_contable.model.TipoCuenta;
import java.util.List;

/**
 * Servicio de Generación Dinámica de Estados Financieros y Reportes Automáticos
 * Clasificación obligatoria por dígito:
 * - Balance General: Código 1 (Activo) = Código 2 (Pasivo) + Código 3 (Capital Contable)
 * - Estado de Resultados: Código 5 (Ingresos) - Código 4 (Costos y Gastos) = Utilidad
 * - Balanza de Comprobación de sumas y saldos.
 */
public class ReportesFinancierosService {

    private final MayorizacionService mayorizacionService;

    public ReportesFinancierosService() {
        this.mayorizacionService = new MayorizacionService();
    }

    public ReportesFinancierosService(MayorizacionService mayorizacionService) {
        this.mayorizacionService = mayorizacionService;
    }

    /**
     * Generación automática del Estado de Resultados:
     * Clasificación por dígito: 5 (Ingresos) - 4 (Costos y Gastos) = Utilidad
     */
    public EstadoResultadosDTO generarEstadoResultados() {
        EstadoResultadosDTO estado = new EstadoResultadosDTO();
        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacionCompleta();

        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();
            // Clasificación obligatoria por dígito:
            if (cod.startsWith("5")) {
                // Ingresos: Saldo Acreedor (o Haber - Debe)
                double saldoIngreso = m.getSaldoAcreedor();
                if (saldoIngreso > 0) {
                    boolean esOperacional = cod.startsWith("51");
                    estado.agregarIngreso(m.getCodigo(), m.getNombre(), saldoIngreso, esOperacional);
                }
            } else if (cod.startsWith("4")) {
                // Costos y Gastos: Saldo Deudor (o Debe - Haber)
                double saldoGasto = m.getSaldoDeudor();
                if (saldoGasto > 0) {
                    estado.agregarCostoGasto(m.getCodigo(), m.getNombre(), saldoGasto, m.getCodigo());
                }
            }
        }

        estado.calcularTotales();
        return estado;
    }

    /**
     * Generación automática del Balance General:
     * Clasificación por dígito:
     * Código 1 (Activo) = Código 2 (Pasivo) + Código 3 (Capital Contable + Utilidad del Periodo)
     */
    public BalanceGeneralDTO generarBalanceGeneral() {
        BalanceGeneralDTO balance = new BalanceGeneralDTO();

        // 1. Obtener la utilidad o pérdida neta del Estado de Resultados
        EstadoResultadosDTO estadoResultados = generarEstadoResultados();
        double utilidadPeriodo = estadoResultados.getUtilidadNeta();

        // 2. Clasificar cuentas de Activo (1), Pasivo (2) y Capital (3)
        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacionCompleta();
        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();
            double saldoNeto = m.getSaldoNeto();

            if (cod.startsWith("1")) {
                // Código 1: ACTIVO
                boolean esCorriente = cod.startsWith("11");
                balance.agregarActivo(m.getCodigo(), m.getNombre(), Math.max(0, saldoNeto), esCorriente);
            } else if (cod.startsWith("2")) {
                // Código 2: PASIVO
                boolean esCorriente = cod.startsWith("21");
                balance.agregarPasivo(m.getCodigo(), m.getNombre(), Math.max(0, saldoNeto), esCorriente);
            } else if (cod.startsWith("3")) {
                // Código 3: CAPITAL CONTABLE
                balance.agregarCapital(m.getCodigo(), m.getNombre(), saldoNeto);
            }
        }

        // 3. Consolidar totales y verificar la Ecuación Contable: 1 = 2 + 3
        balance.calcularTotales(utilidadPeriodo);
        return balance;
    }

    /**
     * Generación de la Balanza de Comprobación (Sumas y Saldos)
     */
    public BalanzaComprobacionDTO generarBalanzaComprobacion() {
        BalanzaComprobacionDTO balanza = new BalanzaComprobacionDTO();
        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacionCompleta();

        for (MayorCuenta m : cuentas) {
            balanza.agregarRenglon(new BalanzaComprobacionDTO.Renglon(
                m.getCodigo(),
                m.getNombre(),
                m.getTipo(),
                m.getTotalDebe(),
                m.getTotalHaber(),
                m.getSaldoDeudor(),
                m.getSaldoAcreedor()
            ));
        }

        return balanza;
    }
}
