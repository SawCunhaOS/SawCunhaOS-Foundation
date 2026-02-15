
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.foundation.utils.valueobjects;

import br.com.sawcunhaos.foundation.utils.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.utils.exception.ScosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaxIdentifier Tests")
class TaxIdentifierTest {

    @Nested
    @DisplayName("Valid CPF scenarios")
    class ValidCpfScenarios {

        @Test
        @DisplayName("Should create TaxIdentifier with valid CPF")
        void shouldCreateTaxIdentifierWithValidCpf() {
            // Arrange
            String validCpf = "11144477735";

            // Act
            TaxIdentifier taxIdentifier = new TaxIdentifier(validCpf);

            // Assert
            assertNotNull(taxIdentifier);
            assertEquals(validCpf, taxIdentifier.getTaxIdentifier());
            assertEquals("CPF", taxIdentifier.getType());
        }

        @ParameterizedTest
        @ValueSource(strings = {"11144477735", "14702172018", "91399147048"})
        @DisplayName("Should recognize various valid CPF numbers")
        void shouldRecognizeVariousValidCpfs(String validCpf) {
            // Act
            TaxIdentifier taxIdentifier = new TaxIdentifier(validCpf);

            // Assert
            assertEquals("CPF", taxIdentifier.getType());
            assertEquals(validCpf, taxIdentifier.getTaxIdentifier());
        }
    }

    @Nested
    @DisplayName("Valid CNPJ scenarios")
    class ValidCnpjScenarios {

        @Test
        @DisplayName("Should create TaxIdentifier with valid CNPJ")
        void shouldCreateTaxIdentifierWithValidCnpj() {
            // Arrange
            String validCnpj = "11222333000181";

            // Act
            TaxIdentifier taxIdentifier = new TaxIdentifier(validCnpj);

            // Assert
            assertNotNull(taxIdentifier);
            assertEquals(validCnpj, taxIdentifier.getTaxIdentifier());
            assertEquals("CNPJ", taxIdentifier.getType());
        }

        @ParameterizedTest
        @ValueSource(strings = {"11222333000181", "71109175000180", "EFJV7G8MY1QE33"})
        @DisplayName("Should recognize various valid CNPJ numbers")
        void shouldRecognizeVariousValidCnpjs(String validCnpj) {
            // Act
            TaxIdentifier taxIdentifier = new TaxIdentifier(validCnpj);

            // Assert
            assertEquals("CNPJ", taxIdentifier.getType());
            assertEquals(validCnpj, taxIdentifier.getTaxIdentifier());
        }
    }

    @Nested
    @DisplayName("Invalid scenarios")
    class InvalidScenarios {

        @Test
        @DisplayName("Should throw ScosException for invalid CPF")
        void shouldThrowExceptionForInvalidCpf() {
            // Arrange
            String invalidCpf = "00000000000";

            // Act & Assert
            ScosException exception = assertThrows(ScosException.class, () -> new TaxIdentifier(invalidCpf));
            assertEquals(ScosExceptionCode.TAX_IDENTIFIER_INVALID.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("Should throw ScosException for invalid CNPJ")
        void shouldThrowExceptionForInvalidCnpj() {
            // Arrange
            String invalidCnpj = "00000000000000";

            // Act & Assert
            ScosException exception = assertThrows(ScosException.class, () -> new TaxIdentifier(invalidCnpj));
            assertEquals(ScosExceptionCode.TAX_IDENTIFIER_INVALID.getCode(), exception.getCode());
        }

        @ParameterizedTest
        @ValueSource(strings = {"11111111111", "99999999999", "12345678901"})
        @DisplayName("Should throw ScosException for various invalid CPF formats")
        void shouldThrowExceptionForVariousInvalidCpfs(String invalidCpf) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new TaxIdentifier(invalidCpf));
        }

        @ParameterizedTest
        @ValueSource(strings = {"00000000000000", "11111111111111", "12345678901234"})
        @DisplayName("Should throw ScosException for various invalid CNPJ formats")
        void shouldThrowExceptionForVariousInvalidCnpjs(String invalidCnpj) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new TaxIdentifier(invalidCnpj));
        }

        @Test
        @DisplayName("Should throw ScosException for empty string")
        void shouldThrowExceptionForEmptyString() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new TaxIdentifier(""));
        }

        @Test
        @DisplayName("Should throw ScosException for invalid characters")
        void shouldThrowExceptionForInvalidCharacters() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new TaxIdentifier("111.444.777-35!"));
        }
    }

    @Nested
    @DisplayName("SetTaxIdentifier method tests")
    class SetTaxIdentifierMethodTests {

        @Test
        @DisplayName("Should update TaxIdentifier with valid new value")
        void shouldUpdateTaxIdentifierWithValidValue() {
            // Arrange
            TaxIdentifier taxIdentifier = new TaxIdentifier("11144477735");
            String newValidCnpj = "11222333000181";

            // Act
            taxIdentifier.setTaxIdentifier(newValidCnpj);

            // Assert
            assertEquals(newValidCnpj, taxIdentifier.getTaxIdentifier());
            assertEquals("CNPJ", taxIdentifier.getType());
        }

        @Test
        @DisplayName("Should change type from CPF to CNPJ when updating")
        void shouldChangeTypeFromCpfToCnpj() {
            // Arrange
            TaxIdentifier taxIdentifier = new TaxIdentifier("11144477735");
            assertEquals("CPF", taxIdentifier.getType());

            // Act
            taxIdentifier.setTaxIdentifier("11222333000181");

            // Assert
            assertEquals("CNPJ", taxIdentifier.getType());
        }

        @Test
        @DisplayName("Should throw ScosException when setting invalid value")
        void shouldThrowExceptionWhenSettingInvalidValue() {
            // Arrange
            TaxIdentifier taxIdentifier = new TaxIdentifier("11144477735");

            // Act & Assert
            assertThrows(ScosException.class, () -> taxIdentifier.setTaxIdentifier("00000000000"));
        }

        @Test
        @DisplayName("Should maintain TaxIdentifier unchanged when exception occurs during update")
        void shouldMaintainTaxIdentifierWhenExceptionOccurs() {
            // Arrange
            String originalCpf = "11144477735";
            TaxIdentifier taxIdentifier = new TaxIdentifier(originalCpf);

            // Act
            assertThrows(ScosException.class, () -> taxIdentifier.setTaxIdentifier("00000000000"));

            // Assert
            assertEquals(originalCpf, taxIdentifier.getTaxIdentifier());
            assertEquals("CPF", taxIdentifier.getType());
        }
    }

    @Nested
    @DisplayName("Getter tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct tax identifier value")
        void shouldReturnCorrectTaxIdentifierValue() {
            // Arrange
            String validCpf = "11144477735";
            TaxIdentifier taxIdentifier = new TaxIdentifier(validCpf);

            // Act & Assert
            assertEquals(validCpf, taxIdentifier.getTaxIdentifier());
        }

        @Test
        @DisplayName("Should return correct type for CPF")
        void shouldReturnCorrectTypeForCpf() {
            // Arrange
            TaxIdentifier taxIdentifier = new TaxIdentifier("11144477735");

            // Act & Assert
            assertEquals("CPF", taxIdentifier.getType());
        }

        @Test
        @DisplayName("Should return correct type for CNPJ")
        void shouldReturnCorrectTypeForCnpj() {
            // Arrange
            TaxIdentifier taxIdentifier = new TaxIdentifier("11222333000181");

            // Act & Assert
            assertEquals("CNPJ", taxIdentifier.getType());
        }
    }

    @Nested
    @DisplayName("Default constructor tests")
    class DefaultConstructorTests {

        @Test
        @DisplayName("Should create instance using protected no-arg constructor")
        void shouldCreateInstanceWithProtectedConstructor() {
            // This test verifies that JPA can instantiate the class with the protected no-arg constructor
            assertDoesNotThrow(() -> new TaxIdentifier("11144477735"));
        }
    }

}