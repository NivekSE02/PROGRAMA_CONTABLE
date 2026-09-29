package com.mycompany.programa_contable;

import com.mycompany.programa_contable.dao.LibroDiarioDAO;
import com.mycompany.programa_contable.dao.CuentaDAO;
import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.Asiento;
import com.mycompany.programa_contable.model.DetalleAsiento;
import com.mycompany.programa_contable.model.KardexFilaDTO;
import com.mycompany.programa_contable.model.Cuenta;
import com.mycompany.programa_contable.model.NaturalezaCuenta;
import com.mycompany.programa_contable.model.TipoCuenta;
import com.mycompany.programa_contable.service.BackupService;
import com.mycompany.programa_contable.service.KardexService;
import com.mycompany.programa_contable.service.MayorizacionService;
import com.mycompany.programa_contable.service.ReglasIva;
import com.mycompany.programa_contable.service.ReportesFinancierosService;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class KardexServiceTest {

    private static final String DB_PROPERTY = "contanoportable.db";
    private final DatabaseManager database = DatabaseManager.getInstance();
    private final LibroDiarioDAO diario = new LibroDiarioDAO();
    private final KardexService kardex = new KardexService();
    private String propiedadAnterior;

    @TempDir
    Path tempDir;

    @BeforeEach
    void prepararBaseTemporal() throws Exception {
        propiedadAnterior = System.getProperty(DB_PROPERTY);
        System.setProperty(DB_PROPERTY, tempDir.resolve("contabilidad-test.db").toString());
        database.activarSQLite();
        database.initDatabase();
        try (Connection conn = database.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE productos SET costo_compra = 10, precio_venta = 40 WHERE id = 1");
        }
    }

    @AfterEach
    void restaurarConfiguracionDeRuta() {
        if (propiedadAnterior == null) System.clearProperty(DB_PROPERTY);
        else System.setProperty(DB_PROPERTY, propiedadAnterior);
    }

    @Test
    @DisplayName("Recalcula costo de salida y saldo con compras a costos distintos")
    void compraVentaPromedioPonderado() throws Exception {
        crearApertura("2026-01-01", 100);
        cambiarCostoCompra(20);
        Asiento compra = crearCompra("2026-01-02", 200);
        Asiento venta = crearVenta("2026-01-03", 400, "Venta de diez unidades");

        List<KardexFilaDTO> filas = kardex.generarReporteKardex(1);
        assertEquals(3, filas.size());
        assertEquals(150.00, filas.get(2).getCostoTotal(), 0.001, "Diez unidades salen al promedio de $15.");
        assertEquals(10, filas.get(2).getExistencias());
        assertEquals(150.00, filas.get(2).getSaldoMonetario(), 0.001);
        assertEquals(150.00, kardex.obtenerCostoDeVentasTotal(), 0.001);
        assertEquals(150.00, kardex.obtenerInventarioFinal(1), 0.001);
        assertEquals(150.00, saldoMayor("1.2"), 0.001);
        assertEquals(150.00, saldoMayor("5.2"), 0.001);
        assertEquals(150.00, sumaDetalle(venta.getId(), "5.2", "AUTO_KARDEX_COGS:%"), 0.001);
        assertEquals(150.00, sumaDetalle(venta.getId(), "1.2", "AUTO_KARDEX_INV:%"), 0.001);
        assertEquals(200.00, sumaDetalle(compra.getId(), "1.2", "AUTO_KARDEX_COMPRA:%"), 0.001);
        assertAsientoCuadrado(compra.getId());
        assertEquals(kardex.obtenerInventarioFinal(1), saldoMayor("1.2"), 0.001);
        assertEquals(kardex.obtenerCostoDeVentasTotal(), saldoMayor("5.2"), 0.001);
        assertAsientoCuadrado(venta.getId());
        assertTrue(new ReportesFinancierosService().generarBalanzaComprobacion().isCuadrada());
    }

    @Test
    @DisplayName("Rechaza eliminar una compra que dejaría una venta sin existencias y revierte todo")
    void eliminarCompraVendidaRevierteTransaccion() throws Exception {
        crearApertura("2026-01-01", 100);
        cambiarCostoCompra(20);
        Asiento compra = crearCompra("2026-01-02", 200);
        crearVenta("2026-01-03", 600, "Venta de quince unidades");

        assertThrows(SQLException.class, () -> diario.eliminarAsiento(compra.getId()));
        assertTrue(existeAsiento(compra.getId()), "El asiento de compra debe seguir guardado.");
        assertEquals(1, contarKardex(compra.getId()), "El movimiento de compra debe seguir guardado.");
        List<KardexFilaDTO> filas = kardex.generarReporteKardex(1);
        assertEquals(3, filas.size());
        assertEquals(5, filas.get(2).getExistencias());
        assertEquals(75.00, filas.get(2).getSaldoMonetario(), 0.001);
    }

    @Test
    @DisplayName("Una compra ingresada con fecha retroactiva revaloriza las salidas posteriores")
    void compraRetroactivaRecalculaFilasPosteriores() throws Exception {
        crearApertura("2026-01-01", 100);
        Asiento venta = crearVenta("2026-01-05", 200, "Venta retroactiva");
        cambiarCostoCompra(20);

        crearCompra("2026-01-03", 200);

        List<KardexFilaDTO> filas = kardex.generarReporteKardex(1);
        assertEquals(3, filas.size());
        assertEquals(75.00, filas.get(2).getCostoTotal(), 0.001);
        assertEquals(15, filas.get(2).getExistencias());
        assertEquals(225.00, filas.get(2).getSaldoMonetario(), 0.001);
        assertEquals(75.00, kardex.obtenerCostoDeVentasTotal(), 0.001);
        assertEquals(225.00, kardex.obtenerInventarioFinal(1), 0.001);
        assertEquals(75.00, sumaDetalle(venta.getId(), "5.2", "AUTO_KARDEX_COGS:%"), 0.001);
        assertEquals(75.00, sumaDetalle(venta.getId(), "1.2", "AUTO_KARDEX_INV:%"), 0.001);
        assertEquals(75.00, saldoMayor("5.2"), 0.001);
        assertEquals(225.00, saldoMayor("1.2"), 0.001);
        assertAsientoCuadrado(venta.getId());
        assertTrue(new ReportesFinancierosService().generarBalanzaComprobacion().isCuadrada());
    }

    @Test
    @DisplayName("Rechaza una fila intermedia negativa aunque una compra posterior deje saldo positivo")
    void recálculoValidaCadaFilaYNoSoloElSaldoFinal() throws Exception {
        crearApertura("2026-01-01", 100);
        cambiarCostoCompra(20);
        crearCompra("2026-01-03", 200);

        assertThrows(SQLException.class, () -> crearVenta("2026-01-02", 600, "Venta anterior a la compra"));

        List<KardexFilaDTO> filas = kardex.generarReporteKardex(1);
        assertEquals(2, filas.size(), "El asiento y el movimiento rechazados deben revertirse.");
        assertEquals(20, filas.get(1).getExistencias());
        assertEquals(300.00, filas.get(1).getSaldoMonetario(), 0.001);
    }

    @Test
    @DisplayName("Eliminar una venta devuelve el inventario a su saldo previo")
    void eliminarVentaRestauraSaldoAnterior() throws Exception {
        crearApertura("2026-01-01", 100);
        Asiento venta = crearVenta("2026-01-02", 200, "Venta a eliminar");

        assertTrue(diario.eliminarAsiento(venta.getId()));

        List<KardexFilaDTO> filas = kardex.generarReporteKardex(1);
        assertEquals(1, filas.size());
        assertEquals(10, filas.get(0).getExistencias());
        assertEquals(100.00, kardex.obtenerInventarioFinal(1), 0.001);
        assertEquals(0.00, kardex.obtenerCostoDeVentasTotal(), 0.001);
        assertEquals(100.00, saldoMayor("1.2"), 0.001);
        assertEquals(0.00, saldoMayor("5.2"), 0.001);

        ReportesFinancierosService reportes = new ReportesFinancierosService();
        assertEquals(0.00, reportes.generarEstadoResultados().getTotalCostos(), 0.001);
        var balance = reportes.generarBalanceGeneral();
        assertEquals(100.00, balance.getTotalActivo(), 0.001);
        assertTrue(balance.isCuadrado());
    }

    @Test
    @DisplayName("Una devoluciÃ³n sobre ventas revierte costo y devuelve inventario al Diario")
    void devolucionVentaRegistraAsientoInverso() throws Exception {
        crearApertura("2026-01-01", 100);
        Asiento venta = guardarAsientoConLineas("2026-01-02", "Venta original",
                new DetalleAsiento(1, "1.1.1", "Caja", "Venta original", 226, 0),
                new DetalleAsiento(2, "4.1", "Ventas", "Venta original", 0, 200),
                new DetalleAsiento(3, "2.3", "IVA Débito Fiscal", "IVA venta", 0, 26));
        Asiento devolucion = guardarAsientoConLineas("2026-01-03", "Devolución del cliente",
                new DetalleAsiento(1, "4.2", "Devoluciones sobre ventas", "Devolución", 40, 0),
                new DetalleAsiento(2, "2.3", "IVA Débito Fiscal", "Reversión IVA", 5.2, 0),
                new DetalleAsiento(3, "1.1.1", "Caja", "Reembolso", 0, 45.2));

        assertEquals(50.00, sumaDetalle(venta.getId(), "5.2", "AUTO_KARDEX_COGS:%"), 0.001);
        assertEquals(10.00, sumaDetalle(devolucion.getId(), "5.2", "AUTO_KARDEX_COGS:%"), 0.001);
        assertEquals(10.00, sumaDetalle(devolucion.getId(), "1.2", "AUTO_KARDEX_INV:%"), 0.001);
        assertEquals(40.00, saldoMayor("5.2"), 0.001);
        assertEquals(60.00, saldoMayor("1.2"), 0.001);
        assertEquals(40.00, kardex.obtenerCostoDeVentasTotal(), 0.001);
        assertEquals(60.00, kardex.obtenerInventarioFinal(1), 0.001);
        assertEquals(20.80, saldoMayor("2.3"), 0.001);
        assertEquals(40.00, new ReportesFinancierosService().generarEstadoResultados().getTotalCostos(), 0.001);
        assertAsientoCuadrado(devolucion.getId());
        assertTrue(new ReportesFinancierosService().generarBalanzaComprobacion().isCuadrada());
    }

    @Test
    @DisplayName("La devolución al proveedor revierte inventario e IVA crédito fiscal")
    void devolucionCompraRevierteInventarioEImpuesto() throws Exception {
        crearApertura("2026-01-01", 100);
        cambiarCostoCompra(20);
        guardarAsientoConLineas("2026-01-02", "Compra con IVA",
                new DetalleAsiento(1, "5.4", "Compras", "Compra neta", 200, 0),
                new DetalleAsiento(2, "1.4", "IVA Crédito Fiscal", "IVA compra", 26, 0),
                new DetalleAsiento(3, "2.1.1", "Proveedores", "Factura", 0, 226));
        Asiento devolucion = guardarAsientoConLineas("2026-01-03", "Devolución al proveedor",
                new DetalleAsiento(1, "2.1.1", "Proveedores", "Nota de crédito", 113, 0),
                new DetalleAsiento(2, "5.1", "Devoluciones sobre compras", "Devolución neta", 0, 100),
                new DetalleAsiento(3, "1.4", "IVA Crédito Fiscal", "Reversión IVA", 0, 13));

        assertEquals(200.00, kardex.obtenerInventarioFinal(1), 0.001);
        assertEquals(200.00, saldoMayor("1.2"), 0.001);
        assertEquals(13.00, saldoMayor("1.4"), 0.001);
        assertEquals(113.00, saldoMayor("2.1.1"), 0.001);
        assertEquals(0.00, saldoMayor("5.1"), 0.001);
        assertEquals(100.00, sumaDetalle(devolucion.getId(), "1.2", "AUTO_KARDEX_DEV_COMPRA:%"), 0.001);
        assertAsientoCuadrado(devolucion.getId());
        assertTrue(new ReportesFinancierosService().generarBalanzaComprobacion().isCuadrada());
    }

    @Test
    @DisplayName("El catálogo fija la naturaleza acreedora/deudora de las devoluciones")
    void catalogoFuerzaNaturalezaDeCuentasContra() throws Exception {
        try (Connection conn = database.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE cuentas SET permite_movimiento = 0 WHERE codigo IN ('4.2', '5.1')");
        }
        CuentaDAO cuentas = new CuentaDAO();
        assertFalse(cuentas.insertar(new Cuenta("4.2.1", "Devoluciones ventas", TipoCuenta.INGRESO,
                "RESTA A INGRESOS", 3, NaturalezaCuenta.ACREEDORA, "4.2", true)));
        assertTrue(cuentas.insertar(new Cuenta("4.2.1", "Devoluciones ventas", TipoCuenta.INGRESO,
                "RESTA A INGRESOS", 3, NaturalezaCuenta.DEUDORA, "4.2", true)));
        assertFalse(cuentas.insertar(new Cuenta("5.1.1", "Devoluciones compras", TipoCuenta.COSTO,
                "RESTA A COSTOS", 3, NaturalezaCuenta.DEUDORA, "5.1", true)));
        assertTrue(cuentas.insertar(new Cuenta("5.1.1", "Devoluciones compras", TipoCuenta.COSTO,
                "RESTA A COSTOS", 3, NaturalezaCuenta.ACREEDORA, "5.1", true)));
        assertTrue(ReglasIva.esDevolucionDeVenta("4.2.1"));
        assertFalse(ReglasIva.esDevolucionDeVenta("4.20"));
        assertTrue(ReglasIva.esDevolucionDeCompra("5.1.1"));
        assertFalse(ReglasIva.esDevolucionDeCompra("5.10"));
    }

    @Test
    @DisplayName("El prÃ©stamo mixto aparece en pasivos no corrientes sin alterar el pasivo total")
    void prestamoMixtoSePresentaComoNoCorriente() throws Exception {
        guardarAsiento("2026-01-01", "PrÃ©stamo bancario", "1.1.1", 1000, "2.2", 1000);

        var balance = new ReportesFinancierosService().generarBalanceGeneral();

        assertEquals(0.00, balance.getTotalPasivoCorriente(), 0.001);
        assertEquals(1000.00, balance.getTotalPasivoNoCorriente(), 0.001);
        assertEquals(1000.00, balance.getTotalPasivo(), 0.001);
        assertEquals(1000.00, balance.getPasivosNoCorrientes().stream()
                .filter(linea -> linea.getCodigo().equals("2.2"))
                .mapToDouble(com.mycompany.programa_contable.model.BalanceGeneralDTO.LineaBalance::getMonto)
                .sum(), 0.001);
    }

    @Test
    @DisplayName("Completa asientos antiguos desde KÃ¡rdex sin duplicar al iniciar de nuevo")
    void migracionAsientosExistentesEsIdempotente() throws Exception {
        crearApertura("2026-01-01", 100);
        cambiarCostoCompra(20);
        Asiento compra = crearCompra("2026-01-02", 200);
        Asiento venta = crearVenta("2026-01-03", 400, "Venta histÃ³rica");
        try (Connection conn = database.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM detalle_asiento WHERE concepto_linea LIKE 'AUTO_KARDEX_%'");
        }

        diario.completarAsientosKardexExistentes();
        diario.completarAsientosKardexExistentes();

        assertEquals(200.00, sumaDetalle(compra.getId(), "1.2", "AUTO_KARDEX_COMPRA:%"), 0.001);
        assertEquals(150.00, sumaDetalle(venta.getId(), "5.2", "AUTO_KARDEX_COGS:%"), 0.001);
        assertEquals(150.00, sumaDetalle(venta.getId(), "1.2", "AUTO_KARDEX_INV:%"), 0.001);
        assertEquals(150.00, saldoMayor("1.2"), 0.001);
        assertEquals(150.00, saldoMayor("5.2"), 0.001);
        assertAsientoCuadrado(compra.getId());
        assertAsientoCuadrado(venta.getId());
        assertTrue(new ReportesFinancierosService().generarBalanzaComprobacion().isCuadrada());
    }

    @Test
    @DisplayName("Un respaldo inválido no modifica los datos existentes")
    void respaldoCorruptoConErrorSqlRevierteRestauracion() throws Exception {
        Asiento apertura = crearApertura("2026-01-01", 100);
        Path respaldo = tempDir.resolve("respaldo-invalido.zip");
        crearRespaldoConTablaIncompleta(respaldo);

        assertThrows(SQLException.class, () -> new BackupService().importar(respaldo));

        assertTrue(existeAsiento(apertura.getId()));
        assertEquals(1, contarKardex(apertura.getId()));
        assertEquals(10, kardex.generarReporteKardex(1).get(0).getExistencias());
    }

    @Test
    @DisplayName("Las reglas de IVA respetan los límites de código de cuenta")
    void reglasIvaNoConfundenPrefijosYAdmitenSubcuentas() {
        assertFalse(ReglasIva.esCuentaConCreditoFiscal("6.10"));
        assertTrue(ReglasIva.esCuentaDeVenta("4.1.1"));
        assertTrue(ReglasIva.esDevolucionDeCompra("5.1.2"));
        assertTrue(ReglasIva.esDevolucionDeVenta("4.2.1"));
    }

    private Asiento crearApertura(String fecha, double importe) throws Exception {
        return guardarAsiento(fecha, "Apertura de inventario", "1.2", importe, "3.1", importe);
    }

    private Asiento crearCompra(String fecha, double importe) throws Exception {
        return guardarAsiento(fecha, "Compra de inventario", "5.4", importe, "2.1.1", importe);
    }

    private Asiento crearVenta(String fecha, double importe, String concepto) throws Exception {
        return guardarAsiento(fecha, concepto, "1.1.1", importe, "4.1", importe);
    }

    private Asiento guardarAsiento(String fecha, String concepto, String debeCuenta, double debe,
            String haberCuenta, double haber) throws Exception {
        Asiento asiento = new Asiento(diario.obtenerSiguienteNumeroAsiento(), fecha, concepto);
        asiento.agregarDetalle(new DetalleAsiento(1, debeCuenta, debeCuenta, concepto, debe, 0));
        asiento.agregarDetalle(new DetalleAsiento(2, haberCuenta, haberCuenta, concepto, 0, haber));
        assertTrue(diario.registrarAsiento(asiento), "El asiento de prueba debe guardarse.");
        return asiento;
    }

    private Asiento guardarAsientoConLineas(String fecha, String concepto, DetalleAsiento... detalles) throws Exception {
        Asiento asiento = new Asiento(diario.obtenerSiguienteNumeroAsiento(), fecha, concepto);
        for (DetalleAsiento detalle : detalles) asiento.agregarDetalle(detalle);
        assertTrue(diario.registrarAsiento(asiento), "El asiento de prueba debe guardarse.");
        return asiento;
    }

    private void cambiarCostoCompra(double costo) throws Exception {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "UPDATE productos SET costo_compra = ? WHERE id = 1")) {
            ps.setDouble(1, costo);
            ps.executeUpdate();
        }
    }

    private boolean existeAsiento(int id) throws Exception {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM asientos WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private double sumaDetalle(int asientoId, String cuenta, String conceptoLike) throws Exception {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(debe + haber), 0) FROM detalle_asiento "
                        + "WHERE asiento_id = ? AND cuenta_codigo = ? AND concepto_linea LIKE ?")) {
            ps.setInt(1, asientoId);
            ps.setString(2, cuenta);
            ps.setString(3, conceptoLike);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0; }
        }
    }

    private double saldoMayor(String codigo) {
        return new MayorizacionService().obtenerMayorizacion(true).stream()
                .filter(cuenta -> cuenta.getCodigo().equals(codigo))
                .mapToDouble(cuenta -> cuenta.getSaldoNeto()).findFirst().orElse(0);
    }

    private void assertAsientoCuadrado(int asientoId) throws Exception {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT a.total_debe, a.total_haber, SUM(d.debe), SUM(d.haber) "
                        + "FROM asientos a JOIN detalle_asiento d ON d.asiento_id = a.id "
                        + "WHERE a.id = ? GROUP BY a.id")) {
            ps.setInt(1, asientoId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(rs.getDouble(1), rs.getDouble(2), 0.001, "Los totales del encabezado deben cuadrar.");
                assertEquals(rs.getDouble(3), rs.getDouble(4), 0.001, "Las lÃ­neas del asiento deben cuadrar.");
                assertEquals(rs.getDouble(1), rs.getDouble(3), 0.001, "El encabezado debe coincidir con el detalle.");
            }
        }
    }

    private int contarKardex(int asientoId) throws Exception {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM kardex WHERE asiento_id = ?")) {
            ps.setInt(1, asientoId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private void crearRespaldoConTablaIncompleta(Path archivo) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archivo))) {
            zip.putNextEntry(new ZipEntry("contabilidad-backup.bin"));
            DataOutputStream out = new DataOutputStream(zip);
            out.writeInt(1);
            out.writeUTF(DatabaseManager.MotorBD.SQLITE.name());
            out.writeInt(1);
            out.writeUTF("cuentas");
            out.writeInt(1);
            out.writeUTF("codigo");
            out.writeInt(Types.VARCHAR);
            out.writeInt(1);
            out.writeBoolean(true);
            out.writeUTF("cuenta-incompleta");
            out.flush();
            zip.closeEntry();
        }
    }
}
