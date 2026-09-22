package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.model.BalanceGeneralDTO;
import com.mycompany.programa_contable.model.BalanzaComprobacionDTO;
import com.mycompany.programa_contable.model.EstadoResultadosDTO;
import com.mycompany.programa_contable.model.MayorCuenta;
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
        
        KardexService kardexService = new KardexService();
        double costoVentasKardex = kardexService.obtenerCostoDeVentasTotal();

        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacion(true);

        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();
            
            // Solo cuentas que aceptan movimientos directos (cuentas hoja) para evitar doble conteo
            if (!m.isPermiteMovimiento()) continue;
            double saldoNeto = m.getSaldoNeto();
            // El costo de ventas proviene del Kárdex
            if (cod.equals("5.2")) {
                saldoNeto = costoVentasKardex; 
            }

            double montoFinal = Math.abs(saldoNeto);

            if (montoFinal == 0) continue;

            // Ingresos (grupo 4)
            if (cod.equals("4.2")) {
                estado.agregarDevolucionVenta(m.getCodigo(), m.getNombre(), montoFinal);
            } else if (cod.startsWith("4")) {
                estado.agregarIngreso(m.getCodigo(), m.getNombre(), montoFinal, true);
            } 
            // Costos (grupo 5, excluyendo 5.4 para evitar duplicidad con compras)
            else if (cod.startsWith("5") && !cod.equals("5.4")) {
                // El costo de ventas procede del Kárdex. Una devolución sobre
                // compras ya redujo el inventario y no debe restarse otra vez.
                if (!cod.equals("5.1")) {
                    estado.agregarCosto(m.getCodigo(), m.getNombre(), montoFinal);
                }
            } 
            // Gastos (grupo 6)
            else if (cod.startsWith("6")) {
                if (cod.startsWith("6.1")) {
                    estado.agregarGastoFinanciero(m.getCodigo(), m.getNombre(), montoFinal);
                } else if (cod.startsWith("6.2")) {
                    estado.agregarGastoAdministracion(m.getCodigo(), m.getNombre(), montoFinal);
                } else if (cod.startsWith("6.3")) {
                    estado.agregarGastoVenta(m.getCodigo(), m.getNombre(), montoFinal);
                }
            }
        }

        estado.calcularTotales();
        return estado;
    }

    private double obtenerSaldoGrupo(String prefijo) {
        double total = 0.0;
        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacion(true);
        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();
            // Solo cuentas hoja (permiten movimiento) para no duplicar con cuentas padre
            if (!m.isPermiteMovimiento()) continue;
            if (!cod.startsWith(prefijo)) continue;
            // Si es el grupo 5, omitimos la cuenta 5.4 para que no duplique compras
            if (prefijo.equals("5") && cod.equals("5.4")) {
                continue;
            }
            double saldo = m.getSaldoNeto();
            total += Math.abs(saldo);
        }
        return total;
    }
    
    public BalanceGeneralDTO generarBalanceGeneral() {
        BalanceGeneralDTO balance = new BalanceGeneralDTO();
        
        KardexService kardexService = new KardexService();
        
        // Utilidad Neta real basada en la Balanza de Comprobación
        // Reutilizamos el criterio del Estado de Resultados para no sumar las
        // devoluciones como ingresos ni las devoluciones de compra como ventas.
        double utilidadNeta = generarEstadoResultados().getUtilidadNeta();

        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacion(true);

        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();

            // Solo cuentas que aceptan movimientos directos (cuentas hoja) para evitar doble conteo
            if (!m.isPermiteMovimiento()) continue;

            double saldoNeto = m.getSaldoNeto();

            if (saldoNeto == 0) continue;

            // Activo (grupo 1)
            if (cod.startsWith("1")) {
                // Clasificar corriente vs no corriente usando el subtipo de la cuenta
                String subtipo = m.getSubtipo() != null ? m.getSubtipo() : "";
                boolean esCorriente = !subtipo.contains("NO CORRIENTE");
                // El inventario perpetuo se controla en Kárdex. La cuenta 1.2
                // contiene el asiento de apertura y no representa el saldo final.
                if (cod.equals("1.2")) {
                    saldoNeto = kardexService.obtenerInventarioFinal(1);
                }
                balance.agregarActivo(m.getCodigo(), m.getNombre(), saldoNeto, esCorriente);
            }
            // Pasivo (grupo 2)
            else if (cod.startsWith("2")) {
                // Clasificar corriente usando subtipo; "CORRIENTE" cubre tanto corriente como corriente/no corriente
                String subtipo = m.getSubtipo() != null ? m.getSubtipo() : "PASIVO CORRIENTE";
                boolean esCorriente = subtipo.contains("CORRIENTE");
                balance.agregarPasivo(m.getCodigo(), m.getNombre(), saldoNeto, esCorriente);
            } 
            // Capital (grupo 3)
            else if (cod.startsWith("3")) {
                balance.agregarCapital(m.getCodigo(), m.getNombre(), saldoNeto);
            }
        }

        balance.calcularTotales(utilidadNeta);
        return balance;
    }

    private double obtenerSaldoGrupoCostosSinCompras() {
        // Obtenemos específicamente el costo de ventas real del kárdex en lugar de sumar la cuenta 5.4
        KardexService kardexService = new KardexService();
        double costoVentasKardex = kardexService.obtenerCostoDeVentasTotal();
        
        double totalOtrasCuentasCostos = 0.0;
        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacion(true);
        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();
            if (cod.startsWith("5") && m.isPermiteMovimiento() && !cod.equals("5.2") && !cod.equals("5.4")) {
                totalOtrasCuentasCostos += Math.abs(m.getSaldoNeto());
            }
        }
        return costoVentasKardex + totalOtrasCuentasCostos;
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
