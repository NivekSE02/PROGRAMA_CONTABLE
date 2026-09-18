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
        double costoVentasKardex    = kardexService.obtenerCostoDeVentasTotal();

        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacion(true);

        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();
            
            if (cod.length() != 3) {
                continue;
            }
            double saldoNeto = m.getSaldoNeto();
            if (saldoNeto == 0) {
                for (var mov : m.getMovimientos()) {
                    String concepto = mov.getConcepto();
                    if (concepto != null && concepto.trim().startsWith("$")) {
                        try {
                            saldoNeto += Double.parseDouble(concepto.replace("$", "").replace(",", "").trim());
                        } catch (Exception e) {}
                    }
                }
            }

            if (cod.equals("5.2")) {
                saldoNeto = costoVentasKardex; 
            }

            double montoFinal = Math.abs(saldoNeto);

            if (montoFinal == 0) continue; // Ignoramos si no hay dinero

            // INGRESOS (Cualquier cuenta que empiece con 4)
            if (cod.startsWith("4")) {
                estado.agregarIngreso(m.getCodigo(), m.getNombre(), montoFinal, true);
            } 
            // COSTOS (Cualquier cuenta que empiece con 5)
            else if (cod.startsWith("5")) {
                if (cod.equals("5.1")) {
                    estado.agregarDevolucionCompra(m.getCodigo(), m.getNombre(), montoFinal);
                } else {
                    estado.agregarCosto(m.getCodigo(), m.getNombre(), montoFinal);
                }
            } 
            // GASTOS (Cualquier cuenta que empiece con 6)
            else if (cod.startsWith("6")) {
                if (cod.equals("6.1")) {
                    estado.agregarGastoFinanciero(m.getCodigo(), m.getNombre(), montoFinal);
                } else if (cod.equals("6.2")) {
                    estado.agregarGastoAdministracion(m.getCodigo(), m.getNombre(), montoFinal);
                } else if (cod.equals("6.3")) {
                    estado.agregarGastoVenta(m.getCodigo(), m.getNombre(), montoFinal);
                }
            }
        }

        estado.calcularTotales();
        return estado;
    }
    
    
    public BalanceGeneralDTO generarBalanceGeneral() {
        BalanceGeneralDTO balance = new BalanceGeneralDTO();
        
        KardexService kardexService = new KardexService();
        
        double costoVentasReal = kardexService.obtenerCostoDeVentasTotal();
        
        // Utilidad temporal hasta Kárdex
        double utilidadPeriodo = 6521.75; 

        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacion(true);

        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();

            if (cod.length() != 3) {
                continue;
            }

            double saldoNeto = m.getSaldoNeto();

            // Si el saldo matemático es 0, rescatamos el valor de su texto "Parcial"
            if (saldoNeto == 0) {
                for (var mov : m.getMovimientos()) {
                    String concepto = mov.getConcepto();
                    if (concepto != null && concepto.trim().startsWith("$")) {
                        try {
                            saldoNeto += Double.parseDouble(concepto.replace("$", "").replace(",", "").trim());
                        } catch (Exception e) {}
                    }
                }
            }

            // Si después de todo sigue en 0, no la mostramos
            if (saldoNeto == 0) continue;

            // CLASIFICACIÓN FINAL 
            // ACTIVO
            if (cod.startsWith("1")) {
                boolean esCorriente = cod.equals("1.1") || cod.equals("1.2") || cod.equals("1.3") 
                        || cod.equals("1.4") || cod.equals("1.5") || cod.equals("1.7");
                
                // REBAJA AUTOMÁTICA DEL KÁRDEX
                if (cod.equals("1.2")) {
                    saldoNeto -= costoVentasReal; 
                }
                balance.agregarActivo(m.getCodigo(), m.getNombre(), saldoNeto, esCorriente);
            }
            // PASIVO
            else if (cod.startsWith("2")) {
                boolean esCorriente = cod.equals("2.1") || cod.equals("2.2") || cod.equals("2.3") || cod.equals("2.4");
                balance.agregarPasivo(m.getCodigo(), m.getNombre(), saldoNeto, esCorriente);
            } 
            // CAPITAL
            else if (cod.startsWith("3")) {
                balance.agregarCapital(m.getCodigo(), m.getNombre(), saldoNeto);
            }
        }

        balance.calcularTotales(utilidadPeriodo);
        return balance;
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