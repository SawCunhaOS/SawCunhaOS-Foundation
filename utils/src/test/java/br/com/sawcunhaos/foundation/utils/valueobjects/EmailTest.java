
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

@DisplayName("Email Tests")
class EmailTest {

    @Nested
    @DisplayName("Valid Email scenarios")
    class ValidEmailScenarios {

        @Test
        @DisplayName("Should create Email with valid address")
        void shouldCreateEmailWithValidAddress() {
            // Arrange
            String validEmail = "user@example.com";

            // Act
            Email email = new Email(validEmail);

            // Assert
            assertNotNull(email);
            assertEquals(validEmail, email.getEmail());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "user@example.com",
                "john.doe@company.co.uk",
                "test+tag@domain.org",
                "user_name@example.com",
                "user-name@example-domain.com",
                "a@b.com",
                "test123@test123.com",
                "UPPERCASE@EXAMPLE.COM",
                "MixedCase@Example.Com",
                "user.name+tag@example.co.uk"
        })
        @DisplayName("Should recognize various valid email addresses")
        void shouldRecognizeVariousValidEmails(String validEmail) {
            // Act
            Email email = new Email(validEmail);

            // Assert
            assertNotNull(email);
            assertEquals(validEmail, email.getEmail());
        }

        @Test
        @DisplayName("Should maintain correct email value after creation")
        void shouldMaintainCorrectEmailValueAfterCreation() {
            // Arrange
            String validEmail = "contact@company.com";
            Email email = new Email(validEmail);

            // Assert
            assertEquals(validEmail, email.getEmail());
        }
    }

    @Nested
    @DisplayName("Invalid email scenarios")
    class InvalidEmailScenarios {

        @Test
        @DisplayName("Should throw ScosException for email without @ symbol")
        void shouldThrowExceptionForEmailWithoutAtSymbol() {
            // Arrange
            String invalidEmail = "userexample.com";

            // Act & Assert
            ScosException exception = assertThrows(ScosException.class, () -> new Email(invalidEmail));
            assertEquals(ScosExceptionCode.EMAIL_INVALID.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("Should throw ScosException for email without domain")
        void shouldThrowExceptionForEmailWithoutDomain() {
            // Arrange
            String invalidEmail = "user@";

            // Act & Assert
            ScosException exception = assertThrows(ScosException.class, () -> new Email(invalidEmail));
            assertEquals(ScosExceptionCode.EMAIL_INVALID.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("Should throw ScosException for email without local part")
        void shouldThrowExceptionForEmailWithoutLocalPart() {
            // Arrange
            String invalidEmail = "@example.com";

            // Act & Assert
            ScosException exception = assertThrows(ScosException.class, () -> new Email(invalidEmail));
            assertEquals(ScosExceptionCode.EMAIL_INVALID.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("Should throw ScosException for email without TLD")
        void shouldThrowExceptionForEmailWithoutTld() {
            // Arrange
            String invalidEmail = "user@example";

            // Act & Assert
            ScosException exception = assertThrows(ScosException.class, () -> new Email(invalidEmail));
            assertEquals(ScosExceptionCode.EMAIL_INVALID.getCode(), exception.getCode());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "userexample.com",
                "user@",
                "@example.com",
                "user@@example.com",
                "user@example",
                "user name@example.com",
                "user@exam ple.com",
                "user@.com",
                "user..name@example.com",
                "user@example..com",
                "user@example.c",
                ".user@example.com",
                "user#@example.com",
                "user@exam#ple.com"
        })
        @DisplayName("Should throw ScosException for various invalid email formats")
        void shouldThrowExceptionForVariousInvalidEmails(String invalidEmail) {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Email(invalidEmail));
        }

        @Test
        @DisplayName("Should throw ScosException for empty string")
        void shouldThrowExceptionForEmptyString() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Email(""));
        }

        @Test
        @DisplayName("Should throw ScosException for null value")
        void shouldThrowExceptionForNullValue() {
            // Act & Assert
            assertThrows(Exception.class, () -> new Email(null));
        }

        @Test
        @DisplayName("Should throw ScosException for email with spaces")
        void shouldThrowExceptionForEmailWithSpaces() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Email("user @example.com"));
        }

        @Test
        @DisplayName("Should throw ScosException for email with special invalid characters")
        void shouldThrowExceptionForEmailWithInvalidSpecialCharacters() {
            // Act & Assert
            assertThrows(ScosException.class, () -> new Email("user*@example.com"));
        }
    }

    @Nested
    @DisplayName("SetEmail method tests")
    class SetEmailMethodTests {

        @Test
        @DisplayName("Should update email with valid new value")
        void shouldUpdateEmailWithValidValue() {
            // Arrange
            Email email = new Email("user@example.com");
            String newValidEmail = "newuser@example.com";

            // Act
            email.setEmail(newValidEmail);

            // Assert
            assertEquals(newValidEmail, email.getEmail());
        }

        @Test
        @DisplayName("Should throw ScosException when setting invalid email")
        void shouldThrowExceptionWhenSettingInvalidEmail() {
            // Arrange
            Email email = new Email("user@example.com");

            // Act & Assert
            assertThrows(ScosException.class, () -> email.setEmail("invalid-email"));
        }

        @Test
        @DisplayName("Should maintain email unchanged when exception occurs during update")
        void shouldMaintainEmailWhenExceptionOccurs() {
            // Arrange
            String originalEmail = "user@example.com";
            Email email = new Email(originalEmail);

            // Act
            assertThrows(ScosException.class, () -> email.setEmail("invalid-email"));

            // Assert
            assertEquals(originalEmail, email.getEmail());
        }

        @Test
        @DisplayName("Should update email multiple times with valid values")
        void shouldUpdateEmailMultipleTimesWithValidValues() {
            // Arrange
            Email email = new Email("user1@example.com");

            // Act
            email.setEmail("user2@example.com");
            email.setEmail("user3@example.com");

            // Assert
            assertEquals("user3@example.com", email.getEmail());
        }

        @Test
        @DisplayName("Should throw ScosException when setting null value")
        void shouldThrowExceptionWhenSettingNullValue() {
            // Arrange
            Email email = new Email("user@example.com");

            // Act & Assert
            assertThrows(Exception.class, () -> email.setEmail(null));
        }
    }

    @Nested
    @DisplayName("Constructor behavior tests")
    class ConstructorBehaviorTests {


        @Test
        @DisplayName("Should create Email instance with valid email")
        void shouldCreateEmailInstanceWithValidEmail() {
            // Arrange
            String validEmail = "test@example.com";

            // Act
            Email email = new Email(validEmail);

            // Assert
            assertNotNull(email);
            assertNotNull(email.getEmail());
            assertEquals(validEmail, email.getEmail());
        }
    }
}

