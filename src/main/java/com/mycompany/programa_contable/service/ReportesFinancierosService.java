package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.model.BalanceGeneralDTO;
import com.mycompany.programa_contable.model.BalanzaComprobacionDTO;
import com.mycompany.programa_contable.model.EstadoResultadosDTO;
import com.mycompany.programa_contable.model.MayorCuenta;
import com.mycompany.programa_contable.model.NaturalezaCuenta;
import com.mycompany.programa_contable.model.TipoCuenta;
import java.util.List;

public class ReportesFinancierosService {

    private final MayorizacionService mayorizacionService;

    public ReportesFinancierosService() {
        this.mayorizacionService = new MayorizacionService();
    }

    public ReportesFinancierosService(MayorizacionService mayorizacionService) {
        this.mayorizacionService = mayorizacionService;
    }

    public EstadoResultadosDTO generarEstadoResultados() {
        EstadoResultadosDTO estado = new EstadoResultadosDTO();
        
        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacion(true);

        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();
            
            // Considerar cualquier cuenta con movimientos propios. El mayor no hace
            // roll-up, así que una cuenta padre usada en asientos no se duplica con
            // sus hijas y no debe desaparecer del estado de resultados.
            if ((m.getTotalDebe() == 0 && m.getTotalHaber() == 0) && !cod.equals("5.2")) continue;
            double saldoNeto = m.getSaldoNeto();
            // En el sistema periódico, el costo es inventario inicial + compras
            // netas - inventario final. Se comparte la misma fórmula de la vista.

            String subtipo = m.getSubtipo() == null ? "" : m.getSubtipo().trim().toUpperCase(java.util.Locale.ROOT);
            if (cod.startsWith("4")) {
                NaturalezaCuenta naturalezaNormal = cod.equals("4.2") || subtipo.equals("RESTA A INGRESOS")
                        ? NaturalezaCuenta.DEUDORA : NaturalezaCuenta.ACREEDORA;
                double montoFinal = saldoSegunNaturaleza(saldoNeto, m.getNaturaleza(), naturalezaNormal);
                if (montoFinal == 0) continue;
                if (cod.equals("4.2") || subtipo.equals("RESTA A INGRESOS")) {
                    estado.agregarDevolucionVenta(m.getCodigo(), m.getNombre(), montoFinal);
                } else {
                    estado.agregarIngreso(m.getCodigo(), m.getNombre(), montoFinal,
                            !subtipo.equals("OTROS INGRESOS"));
                }
            } else if (cod.startsWith("5")) {
                boolean esCuentaCompras = cod.equals("5.4") || cod.startsWith("5.4.");
                boolean esCuentaNoOperativa = subtipo.equals("RESTA A COSTOS")
                        || subtipo.equals("CUENTA TRANSITORIA");
                if (!esCuentaCompras && !esCuentaNoOperativa && !cod.equals("5.1")) {
                    double montoFinal = saldoSegunNaturaleza(saldoNeto, m.getNaturaleza(), NaturalezaCuenta.DEUDORA);
                    if (montoFinal == 0) continue;
                    estado.agregarCosto(m.getCodigo(), m.getNombre(), montoFinal);
                }
            } else if (cod.startsWith("6")) {
                double montoFinal = saldoSegunNaturaleza(saldoNeto, m.getNaturaleza(), NaturalezaCuenta.DEUDORA);
                if (montoFinal == 0) continue;
                if (subtipo.equals("GASTOS FINANCIEROS") || esSubcuentaDe(cod, "6.1")) {
                    estado.agregarGastoFinanciero(m.getCodigo(), m.getNombre(), montoFinal);
                } else if (subtipo.equals("GASTOS DE ADMINISTRACIÓN") || esSubcuentaDe(cod, "6.2")) {
                    estado.agregarGastoAdministracion(m.getCodigo(), m.getNombre(), montoFinal);
                } else if (subtipo.equals("GASTOS DE VENTA") || esSubcuentaDe(cod, "6.3")) {
                    estado.agregarGastoVenta(m.getCodigo(), m.getNombre(), montoFinal);
                }
            }
        }

        estado.calcularTotales();
        return estado;
    }

    private boolean esSubcuentaDe(String codigo, String codigoGrupo) {
        return codigo.equals(codigoGrupo) || codigo.startsWith(codigoGrupo + ".");
    }

    private double saldoSegunNaturaleza(double saldo, NaturalezaCuenta naturalezaCuenta,
            NaturalezaCuenta naturalezaNormal) {
        return naturalezaCuenta == naturalezaNormal ? saldo : -saldo;
    }

    public BalanceGeneralDTO generarBalanceGeneral() {
        BalanceGeneralDTO balance = new BalanceGeneralDTO();
        
        // Utilidad Neta real basada en la Balanza de Comprobación
        // Reutilizamos el criterio del Estado de Resultados para no sumar las
        // devoluciones como ingresos ni las devoluciones de compra como ventas.
        double utilidadNeta = generarEstadoResultados().getUtilidadNeta();

        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacion(true);

        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();

            // El mayor contiene movimientos propios, sin roll-up. Incluir cuentas
            // padre cuando un asiento se registró directamente en ellas.
            if (m.getTotalDebe() == 0 && m.getTotalHaber() == 0 && !cod.equals("1.2")) continue;

            double saldoNeto = m.getSaldoNeto();

            // Activo (grupo 1)
            if (cod.startsWith("1")) {
                // Clasificar corriente vs no corriente usando el subtipo de la cuenta
                String subtipo = m.getSubtipo() != null ? m.getSubtipo() : "";
                boolean esCorriente = !subtipo.contains("NO CORRIENTE");
                // El inventario perpetuo se controla en Kárdex. La cuenta 1.2
                // contiene el asiento de apertura. Reconstruimos el saldo final
                // con importes del mayor y el costo de las salidas de venta para
                // no arrastrar valores de Kárdex guardados con redondeos antiguos.
                if (saldoNeto == 0) continue;
                double saldoPresentado = m.getNaturaleza() == TipoCuenta.ACTIVO.getNaturalezaPorDefecto()
                        ? saldoNeto : -Math.abs(saldoNeto);
                balance.agregarActivo(m.getCodigo(), m.getNombre(), saldoPresentado, esCorriente);
            }
            // Pasivo (grupo 2)
            else if (cod.startsWith("2")) {
                if (saldoNeto == 0) continue;
                // Si el catálogo indica ambas porciones, presentar el saldo completo como no corriente.
                // El modelo actual no guarda el calendario de pagos para separar la porción del próximo año.
                String subtipo = m.getSubtipo() != null ? m.getSubtipo() : "PASIVO CORRIENTE";
                boolean esCorriente = !subtipo.contains("NO CORRIENTE");
                double saldoPresentado = m.getNaturaleza() == TipoCuenta.PASIVO.getNaturalezaPorDefecto()
                        ? saldoNeto : -Math.abs(saldoNeto);
                balance.agregarPasivo(m.getCodigo(), m.getNombre(), saldoPresentado, esCorriente);
            } 
            // Capital (grupo 3)
            else if (cod.startsWith("3")) {
                if (saldoNeto == 0) continue;
                double saldoPresentado = m.getNaturaleza() == TipoCuenta.PATRIMONIO.getNaturalezaPorDefecto()
                        ? saldoNeto : -Math.abs(saldoNeto);
                balance.agregarCapital(m.getCodigo(), m.getNombre(), saldoPresentado);
            }
        }

        balance.calcularTotales(utilidadNeta);
        return balance;
    }

    private double redondear(double val) {
        return java.math.BigDecimal.valueOf(val).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

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
