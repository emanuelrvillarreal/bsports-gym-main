package com.blue.open.Code.Service;

import org.springframework.stereotype.Service;

@Service
public class CuitValidatorService {

    /**
     * Valida un CUIT argentino basándose en su algoritmo de dígito verificador.
     * 
     * @param cuit El CUIT a validar (puede contener guiones o solo números)
     * @return true si es válido, false de lo contrario
     */
    public boolean isValid(String cuit) {
        if (cuit == null) {
            return false;
        }

        // Eliminar guiones si existen
        String cleanCuit = cuit.replaceAll("[^0-9]", "");

        if (cleanCuit.length() != 11) {
            return false;
        }

        int[] weights = { 5, 4, 3, 2, 7, 6, 5, 4, 3, 2 };
        int sum = 0;

        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cleanCuit.charAt(i)) * weights[i];
        }

        int remainder = sum % 11;
        int result = 11 - remainder;

        int controlDigit;
        if (result == 11) {
            controlDigit = 0;
        } else if (result == 10) {
            // Nota: En la práctica, si el resultado es 10, el CUIT no existe con ese prefijo
            // (se cambia de 20 a 23 por ejemplo), pero para validación técnica:
            controlDigit = 9; 
        } else {
            controlDigit = result;
        }

        int actualControlDigit = Character.getNumericValue(cleanCuit.charAt(10));

        return controlDigit == actualControlDigit;
    }
}
