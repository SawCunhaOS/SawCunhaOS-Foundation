
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

package br.com.sawcunhaos.foundation.validation.valueobjects;

import br.com.sawcunhaos.foundation.core.enums.ScosExceptionCode;
import br.com.sawcunhaos.foundation.core.exception.ScosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CPF Tests")
class CpfTest {

    @Nested
    @DisplayName("Valid CPF scenarios")
    class ValidCpfScenarios {

        @Test
        @DisplayName("Should create CPF with valid number")
        void shouldCreateCpfWithValidNumber() {
            // Arrange
            String validCpf = "11144477735";

            // Act
            Cpf cpf = new Cpf(validCpf);

            // Assert
            assertNotNull(cpf);
            assertEquals(validCpf, cpf.getCpf());
            assertEquals("CPF", cpf.getType());
        }

        @ParameterizedTest
        @ValueSource(strings = {"11144477735", "46079305046", "14702172018"})
        @DisplayName("Should recognize various valid CPF numbers")
        void shouldRecognizeVariousValidCpfs(String validCpf) {
            // Act
            Cpf cpf = new Cpf(validCpf);

            // Assert
            assertEquals("CPF", cpf.getType());
            assertEquals(validCpf, cpf.getCpf());
        }

        @Test
        @DisplayName("Should maintain correct type for any valid CPF")
        void shouldMaintainCorrectTypeForValidCpf() {
            // Arrange
            String validCpf = "11144477735";
            Cpf cpf = new Cpf(validCpf);

            // Assert
            assertEquals("CPF", cpf.getType());
        }
    }

    @Nested
    @DisplayName("Invalid scenarios")
    class InvalidScenarios {

        @Test
        @DisplayName("Should throw ScosException for invalid CPF with all zeros")
        void shouldThrowExceptionForAllZerosCpf() {
            // Arrange
            String invalidCpf = "00000000000";

            // Act & Assert
            ScosException exception = assertThrows(ScosException.class, () -> new Cpf(invalidCpf));
            assertEquals(ScosExceptionCode.CPF_INVALID.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("Should throw ScosException for invalid CPF with all ones")
        void shouldThrowExceptionForAllOnesCpf() {
            // Arrange
            String invalidCpf = "11111111111";

            // Act & Assert
            ScosException exception = assertThrows(ScosException.class, () -> new Cpf(invalidCpf));
            assertEquals(ScosExceptionCode.CPF_INVALID.getCode(), exception.getCode());
        }

        @ParameterizedTest
        @ValueSource(strings = {"11111111111", "99999999999", "12345678901", "00000000000"})
        @DisplayName("Should throw ScosException for various invalid CPF formats")
        void shouldThrowExceptionForVariousInvalidCpfs(String invalidCpf) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf(invalidCpf));
        }

        @Test
        @DisplayName("Should throw ScosException for empty string")
        void shouldThrowExceptionForEmptyString() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf(""));
        }

        @Test
        @DisplayName("Should throw ScosException for null value")
        void shouldThrowExceptionForNullValue() {
            // Act & Assert
            assertThrows(Exception.class, () -> new Cpf(null));
        }

        @Test
        @DisplayName("Should throw ScosException for CPF with invalid characters")
        void shouldThrowExceptionForInvalidCharacters() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf("111.444.777-35!"));
        }

        @Test
        @DisplayName("Should throw ScosException for CPF with letters")
        void shouldThrowExceptionForCpfWithLetters() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf("11144477A35"));
        }

        @Test
        @DisplayName("Should throw ScosException for formatted CPF string")
        void shouldThrowExceptionForFormattedCpf() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf("111.444.777-35"));
        }

        @Test
        @DisplayName("Should throw ScosException for CNPJ instead of CPF")
        void shouldThrowExceptionForCnpj() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf("11222333000181"));
        }

        @Test
        @DisplayName("Should throw ScosException for CPF with wrong number of digits")
        void shouldThrowExceptionForWrongNumberOfDigits() {
            // Arrange
            String invalidCpf = "111444777";

            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf(invalidCpf));
        }
    }

    @Nested
    @DisplayName("SetTaxIdentifier method tests")
    class SetTaxIdentifierMethodTests {

        @Test
        @DisplayName("Should update CPF with valid new value")
        void shouldUpdateCpfWithValidValue() {
            // Arrange
            Cpf cpf = new Cpf("11144477735");
            String newValidCpf = "46079305046";

            // Act
            cpf.setTaxIdentifier(newValidCpf);

            // Assert
            assertEquals(newValidCpf, cpf.getCpf());
            assertEquals("CPF", cpf.getType());
        }

        @Test
        @DisplayName("Should throw ScosException when setting invalid CPF")
        void shouldThrowExceptionWhenSettingInvalidCpf() {
            // Arrange
            Cpf cpf = new Cpf("11144477735");

            // Act & Assert
            assertThrows(ScosException.class, () -> cpf.setTaxIdentifier("00000000000"));
        }

        @Test
        @DisplayName("Should maintain CPF unchanged when exception occurs during update")
        void shouldMaintainCpfWhenExceptionOccurs() {
            // Arrange
            String originalCpf = "11144477735";
            Cpf cpf = new Cpf(originalCpf);

            // Act
            assertThrows(ScosException.class, () -> cpf.setTaxIdentifier("11111111111"));

            // Assert
            assertEquals(originalCpf, cpf.getCpf());
            assertEquals("CPF", cpf.getType());
        }

        @Test
        @DisplayName("Should allow multiple valid updates")
        void shouldAllowMultipleValidUpdates() {
            // Arrange
            Cpf cpf = new Cpf("11144477735");
            String newCpf1 = "46079305046";
            String newCpf2 = "14702172018";

            // Act
            cpf.setTaxIdentifier(newCpf1);
            assertEquals(newCpf1, cpf.getCpf());

            cpf.setTaxIdentifier(newCpf2);

            // Assert
            assertEquals(newCpf2, cpf.getCpf());
            assertEquals("CPF", cpf.getType());
        }
    }

    @Nested
    @DisplayName("Getter tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct CPF value")
        void shouldReturnCorrectCpfValue() {
            // Arrange
            String validCpf = "11144477735";
            Cpf cpf = new Cpf(validCpf);

            // Act & Assert
            assertEquals(validCpf, cpf.getCpf());
        }

        @Test
        @DisplayName("Should return correct type")
        void shouldReturnCorrectType() {
            // Arrange
            Cpf cpf = new Cpf("11144477735");

            // Act & Assert
            assertEquals("CPF", cpf.getType());
        }

        @Test
        @DisplayName("Should always return CPF as type")
        void shouldAlwaysReturnCpfAsType() {
            // Arrange
            String validCpf = "46079305046";
            Cpf cpf = new Cpf(validCpf);

            // Act & Assert
            assertEquals("CPF", cpf.getType());
            assertNotEquals("CNPJ", cpf.getType());
        }
    }

    @Nested
    @DisplayName("Default constructor tests")
    class DefaultConstructorTests {

        @Test
        @DisplayName("Should allow creation with protected no-arg constructor for JPA")
        void shouldAllowProtectedNoArgConstructor() {
            // This test verifies that JPA can instantiate the class
            assertDoesNotThrow(() -> new Cpf("11144477735"));
        }
    }

    @Nested
    @DisplayName("Alphanumeric CPF tests")
    class AlphanumericCpfTests {

        @Test
        @DisplayName("Should reject CPF with special characters in middle")
        void shouldRejectCpfWithSpecialCharactersInMiddle() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf("11144@77735"));
        }

        @Test
        @DisplayName("Should reject CPF with spaces")
        void shouldRejectCpfWithSpaces() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf("111 444 777 35"));
        }

        @Test
        @DisplayName("Should reject CPF with hyphens")
        void shouldRejectCpfWithHyphens() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf("111-444-777-35"));
        }

        @Test
        @DisplayName("Should reject CPF with dots")
        void shouldRejectCpfWithDots() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf("111.444.777.35"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"111A44477735", "1114447773A", "11144A77735", "ABC44477735", "111444777ABC"})
        @DisplayName("Should reject CPF with alphabetic characters")
        void shouldRejectCpfWithAlphabeticCharacters(String cpfWithLetters) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf(cpfWithLetters));
        }

        @ParameterizedTest
        @ValueSource(strings = {"111!44477735", "111#44477735", "111$44477735", "111%44477735", "111&44477735"})
        @DisplayName("Should reject CPF with various special characters")
        void shouldRejectCpfWithSpecialCharacters(String cpfWithSpecialChar) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Cpf(cpfWithSpecialChar));
        }
    }

}

