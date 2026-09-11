package com.mycompany.programa_contable;

import com.mycompany.programa_contable.model.Asiento;
import com.mycompany.programa_contable.model.DetalleAsiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la Validación Obligatoria de la Partida Doble
 * Requerimiento: "El sistema debe bloquear el guardado si el asiento no cumple la Partida Doble"
 */
public class PartidaDobleTest {

    @Test
    @DisplayName("Debe aceptar un asiento cuando Suma(Debe) == Suma(Haber)")
    void testAsientoCuadradoValido() {
        Asiento asiento = new Asiento(1, "2026-09-01", "Aporte inicial de socios", 1);
        asiento.agregarDetalle(new DetalleAsiento(1, "110103", "Bancos", "Depósito", 10000.0, 0.0));
        asiento.agregarDetalle(new DetalleAsiento(2, "310101", "Capital Social", "Aportación", 0.0, 10000.0));

        assertTrue(asiento.isPartidaDobleValida(), "El asiento con Debe=10000 y Haber=10000 debe ser válido.");
        assertEquals(0.0, asiento.getDiferencia(), 0.001, "La diferencia debe ser exactamente 0.");
    }

    @Test
    @DisplayName("Debe rechazar un asiento cuando Suma(Debe) != Suma(Haber)")
    void testAsientoDescuadradoInvalido() {
        Asiento asiento = new Asiento(2, "2026-09-02", "Compra descuadrada", 1);
        asiento.agregarDetalle(new DetalleAsiento(1, "110501", "Inventario", "Compra", 500.0, 0.0));
        asiento.agregarDetalle(new DetalleAsiento(2, "110103", "Bancos", "Pago", 0.0, 450.0)); // Descuadrado por $50

        assertFalse(asiento.isPartidaDobleValida(), "El asiento descuadrado NO debe ser válido.");
        assertEquals(50.0, asiento.getDiferencia(), 0.001, "La diferencia debe ser de $50.00.");
    }

    @Test
    @DisplayName("Debe rechazar un asiento con un solo renglón o totales en cero")
    void testAsientoVacioOIncompleto() {
        Asiento vacio = new Asiento(3, "2026-09-03", "Asiento vacío", 1);
        assertFalse(vacio.isPartidaDobleValida(), "Un asiento sin detalles debe ser rechazado.");

        Asiento unRenglon = new Asiento(4, "2026-09-04", "Solo un renglón", 1);
        unRenglon.agregarDetalle(new DetalleAsiento(1, "110101", "Caja", "Ingreso", 100.0, 0.0));
        assertFalse(unRenglon.isPartidaDobleValida(), "Un asiento con un solo renglón debe ser rechazado.");
    }
}
