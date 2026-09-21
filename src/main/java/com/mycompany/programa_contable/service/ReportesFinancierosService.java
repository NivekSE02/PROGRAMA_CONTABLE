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
            
            // Permitimos cuentas de 3 dígitos O subcuentas operativas de gastos (ej. 6.1.1)
            boolean esCuentaValida = (cod.length() == 3) || (cod.startsWith("6.1") && cod.length() > 3);
            if (!esCuentaValida) {
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

            // Inyectamos el Costo de Ventas real calculado por el Kárdex
            if (cod.equals("5.2")) {
                saldoNeto = costoVentasKardex; 
            }

            double montoFinal = Math.abs(saldoNeto);

            if (montoFinal == 0) continue;

            // INGRESOS (Grupo 4)
            if (cod.startsWith("4")) {
                estado.agregarIngreso(m.getCodigo(), m.getNombre(), montoFinal, true);
            } 
            // COSTOS (Grupo 5, EXCLUYENDO estrictamente la cuenta transitoria 5.4 Compras para evitar duplicidad)
            else if (cod.startsWith("5") && !cod.equals("5.4")) {
                if (cod.equals("5.1")) {
                    estado.agregarDevolucionCompra(m.getCodigo(), m.getNombre(), montoFinal);
                } else {
                    estado.agregarCosto(m.getCodigo(), m.getNombre(), montoFinal);
                }
            } 
            // GASTOS (Grupo 6) - Captura tanto 6.1 como la subcuenta detallada 6.1.1
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
            // Permitimos cuentas de 3 dígitos o subcuentas del grupo 6 (como 6.1.1)
            boolean esValida = (cod.startsWith(prefijo) && cod.length() == 3) || 
                               (prefijo.equals("6") && cod.startsWith("6.1") && cod.length() > 3);
            if (esValida) {
                // Si es el grupo 5, omitimos la cuenta 5.4 para que no duplique compras
                if (prefijo.equals("5") && cod.equals("5.4")) {
                    continue;
                }
                double saldo = m.getSaldoNeto();
                if (saldo == 0) {
                    for (var mov : m.getMovimientos()) {
                        String concepto = mov.getConcepto();
                        if (concepto != null && concepto.trim().startsWith("$")) {
                            try {
                                saldo += Double.parseDouble(concepto.replace("$", "").replace(",", "").trim());
                            } catch (Exception ignored) {}
                        }
                    }
                }
                total += Math.abs(saldo);
            }
        }
        return total;
    }
    
    public BalanceGeneralDTO generarBalanceGeneral() {
        BalanceGeneralDTO balance = new BalanceGeneralDTO();
        
        KardexService kardexService = new KardexService();
        
        // Utilidad Neta real basada en la Balanza de Comprobación
        double totalIngresos = obtenerSaldoGrupo("4");
        double totalCostosGastos = obtenerSaldoGrupoCostosSinCompras() + obtenerSaldoGrupo("6");
        double utilidadNeta = redondear(totalIngresos - totalCostosGastos);

        List<MayorCuenta> cuentas = mayorizacionService.obtenerMayorizacion(true);

        for (MayorCuenta m : cuentas) {
            String cod = m.getCodigo();

            // 1. Tomamos cuentas principales de 3 dígitos (ej. 1.1, 1.3, 1.4, 2.1, 2.2, 2.3, 3.1).
            // 2. EXCEPCIÓN: Tomamos los detalles de Activo No Corriente que empiezan con 1.6 y tienen más de 3 dígitos (1.6.1, 1.6.2, etc.).
            // 3. OMITIMOS estrictamente las cuentas de 1 o 2 dígitos (como "1", "2") para evitar sumas dobles.
            boolean esMayorTresDigitos = (cod.length() == 3);
            boolean esActivoNoCorrienteDetalle = cod.startsWith("1.6") && cod.length() > 3;

            if (!esMayorTresDigitos && !esActivoNoCorrienteDetalle) {
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

            if (saldoNeto == 0) continue;

            // CLASIFICACIÓN FINAL 
            // ACTIVO (Grupo 1)
            if (cod.startsWith("1")) {
                boolean esCorriente = cod.equals("1.1") || cod.equals("1.2") || cod.equals("1.3") || cod.equals("1.4");
                
                // VALORIZACIÓN REAL DEL KÁRDEX PARA LA CUENTA 1.2 (Inventario)
                if (cod.equals("1.2")) {
                    // Si tu inventario físico según la balanza es de $6,000.00, lo tomamos directo del kárdex o saldo neto:
                    double valorTotalBodega = 0.0;
                    List<com.mycompany.programa_contable.model.KardexFilaDTO> filasKardex = kardexService.generarReporteKardex(1);
                    if (!filasKardex.isEmpty()) {
                        var ultimaFila = filasKardex.get(filasKardex.size() - 1);
                        valorTotalBodega = ultimaFila.getSaldoMonetario();
                    }
                    saldoNeto = (valorTotalBodega > 0) ? valorTotalBodega : saldoNeto; 
                }
                balance.agregarActivo(m.getCodigo(), m.getNombre(), saldoNeto, esCorriente);
            }
            // PASIVO (Grupo 2) - Se leen limpio las cuentas 2.1, 2.2 y 2.3 sin duplicidades
            else if (cod.startsWith("2")) {
                boolean esCorriente = cod.equals("2.1") || cod.equals("2.2") || cod.equals("2.3") || cod.equals("2.4");
                balance.agregarPasivo(m.getCodigo(), m.getNombre(), saldoNeto, esCorriente);
            } 
            // CAPITAL (Grupo 3)
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
            if (cod.startsWith("5") && cod.length() == 3 && !cod.equals("5.2") && !cod.equals("5.4")) {
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