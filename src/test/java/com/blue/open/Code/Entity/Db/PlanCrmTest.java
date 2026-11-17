package com.blue.open.Code.Entity.Db;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlanCrmTest {

    // Dado un plan con precioEsFinal=true, cuando precioFinal, entonces devuelve el precio
    // crudo sin sumarle nada: el CRM ya lo informa como importe final.
    @Test
    void precioFinal_precioEsFinal_devuelvePrecioCrudo() {
        PlanCrm plan = planCon(new BigDecimal("1000.00"), Boolean.TRUE, new BigDecimal("21.00"));

        assertEquals(new BigDecimal("1000.00"), plan.precioFinal());
    }

    // Dado un plan con precioEsFinal=false y alicuotaIva, cuando precioFinal, entonces suma el
    // IVA (precio * alicuota / 100), redondeado a 2 decimales HALF_UP.
    @Test
    void precioFinal_noEsFinalConAlicuota_sumaIvaRedondeado() {
        PlanCrm plan = planCon(new BigDecimal("1000.00"), Boolean.FALSE, new BigDecimal("21.00"));

        // 1000 * 21 / 100 = 210.00 -> 1210.00
        assertEquals(new BigDecimal("1210.00"), plan.precioFinal());
    }

    // Mismo caso que arriba pero con precioEsFinal null (no informado): se trata igual que false.
    @Test
    void precioFinal_precioEsFinalNull_sumaIva() {
        PlanCrm plan = planCon(new BigDecimal("500.00"), null, new BigDecimal("10.50"));

        // 500 * 10.5 / 100 = 52.5 -> 552.50
        assertEquals(new BigDecimal("552.50"), plan.precioFinal());
    }

    // El redondeo HALF_UP a 2 decimales tiene que aplicarse aunque el IVA de un valor con mas
    // decimales antes de redondear.
    @Test
    void precioFinal_redondeaHalfUpADosDecimales() {
        PlanCrm plan = planCon(new BigDecimal("333.33"), Boolean.FALSE, new BigDecimal("21.00"));

        // 333.33 * 21 / 100 = 69.9993 -> HALF_UP a 2 decimales = 70.00 -> 403.33
        assertEquals(new BigDecimal("403.33"), plan.precioFinal());
    }

    // Dado un plan sin alicuotaIva informada, cuando precioFinal, entonces no suma nada aunque
    // precioEsFinal sea false: no hay con que calcular el IVA.
    @Test
    void precioFinal_sinAlicuotaIva_devuelvePrecioCrudo() {
        PlanCrm plan = planCon(new BigDecimal("1000.00"), Boolean.FALSE, null);

        assertEquals(new BigDecimal("1000.00"), plan.precioFinal());
    }

    // Dado un plan sin precio vigente, cuando precioFinal, entonces devuelve null.
    @Test
    void precioFinal_sinPrecio_devuelveNull() {
        PlanCrm plan = planCon(null, Boolean.FALSE, new BigDecimal("21.00"));

        assertNull(plan.precioFinal());
    }

    private PlanCrm planCon(BigDecimal precio, Boolean precioEsFinal, BigDecimal alicuotaIva) {
        PlanCrm plan = PlanCrm.crear("plan-test");
        plan.actualizar("Plan Test", "desc", precio, precioEsFinal, alicuotaIva, "mensual", null);
        return plan;
    }
}
