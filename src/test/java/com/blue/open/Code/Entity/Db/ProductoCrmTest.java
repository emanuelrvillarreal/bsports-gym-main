package com.blue.open.Code.Entity.Db;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductoCrmTest {

    // A diferencia de PlanCrm, un producto no tiene precioEsFinal: su precio siempre es neto, asi
    // que el IVA se suma siempre que haya alicuota informada.
    @Test
    void precioFinal_conAlicuota_sumaIvaRedondeado() {
        ProductoCrm producto = productoCon(new BigDecimal("1000.00"), new BigDecimal("21.00"));

        // 1000 * 21 / 100 = 210.00 -> 1210.00
        assertEquals(new BigDecimal("1210.00"), producto.precioFinal());
    }

    // El redondeo HALF_UP a 2 decimales tiene que aplicarse igual que en PlanCrm.
    @Test
    void precioFinal_redondeaHalfUpADosDecimales() {
        ProductoCrm producto = productoCon(new BigDecimal("333.33"), new BigDecimal("21.00"));

        // 333.33 * 21 / 100 = 69.9993 -> HALF_UP a 2 decimales = 70.00 -> 403.33
        assertEquals(new BigDecimal("403.33"), producto.precioFinal());
    }

    // Dado un producto sin alicuotaIva informada (exento), cuando precioFinal, entonces devuelve
    // el precio crudo sin sumar nada.
    @Test
    void precioFinal_sinAlicuotaIva_devuelvePrecioCrudo() {
        ProductoCrm producto = productoCon(new BigDecimal("1000.00"), null);

        assertEquals(new BigDecimal("1000.00"), producto.precioFinal());
    }

    // Dado un producto sin precio vigente, cuando precioFinal, entonces devuelve null.
    @Test
    void precioFinal_sinPrecio_devuelveNull() {
        ProductoCrm producto = productoCon(null, new BigDecimal("21.00"));

        assertNull(producto.precioFinal());
    }

    private ProductoCrm productoCon(BigDecimal precio, BigDecimal alicuotaIva) {
        ProductoCrm producto = ProductoCrm.crear("producto-test");
        producto.actualizar("Producto Test", "desc", precio, alicuotaIva, "unidad");
        return producto;
    }
}
