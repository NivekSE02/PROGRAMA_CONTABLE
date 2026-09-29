package com.mycompany.programa_contable;

import com.mycompany.programa_contable.service.CalculoIva;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculoIvaTest {

    @Test
    void desglosaImporteConIvaIncluido() {
        CalculoIva.Desglose desglose = CalculoIva.desglosar(1130.00, 0.13, true);
        assertEquals(1000.00, desglose.base(), 0.001);
        assertEquals(130.00, desglose.impuesto(), 0.001);
    }

    @Test
    void calculaIvaCuandoSeIngresaBaseNeta() {
        CalculoIva.Desglose desglose = CalculoIva.desglosar(1000.00, 0.13, false);
        assertEquals(1000.00, desglose.base(), 0.001);
        assertEquals(130.00, desglose.impuesto(), 0.001);
    }

    @Test
    void rechazaImportesNegativosONoFinitos() {
        assertThrows(IllegalArgumentException.class, () -> CalculoIva.desglosar(-1, 0.13, false));
        assertThrows(IllegalArgumentException.class, () -> CalculoIva.desglosar(Double.NaN, 0.13, false));
    }
}
