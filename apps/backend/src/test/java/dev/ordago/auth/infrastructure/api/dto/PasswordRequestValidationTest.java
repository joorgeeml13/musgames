package dev.ordago.auth.infrastructure.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Password Request DTO Validation Tests")
class PasswordRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Nested
    @DisplayName("ResetPasswordRequest boundary validation")
    class ResetPasswordRequestValidation {

        private static final String VALID_TOKEN = "valid-reset-token-12345";

        @Test
        @DisplayName("Should accept password at minimum boundary (8 characters)")
        void shouldAcceptPasswordAtMinBoundary8Chars() {
            // Arrange
            String minPassword = "a".repeat(8);
            ResetPasswordRequest request = new ResetPasswordRequest(VALID_TOKEN, minPassword);

            // Act
            Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept password at intermediate length (72 characters)")
        void shouldAcceptPasswordAtIntermediateLength72Chars() {
            // Arrange
            String intermediatePassword = "a".repeat(72);
            ResetPasswordRequest request = new ResetPasswordRequest(VALID_TOKEN, intermediatePassword);

            // Act
            Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept password at maximum boundary (128 characters)")
        void shouldAcceptPasswordAtMaxBoundary128Chars() {
            // Arrange
            String maxPassword = "a".repeat(128);
            ResetPasswordRequest request = new ResetPasswordRequest(VALID_TOKEN, maxPassword);

            // Act
            Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject password below minimum boundary (7 characters)")
        void shouldRejectPasswordBelowMinBoundary7Chars() {
            // Arrange
            String tooShortPassword = "a".repeat(7);
            ResetPasswordRequest request = new ResetPasswordRequest(VALID_TOKEN, tooShortPassword);

            // Act
            Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("Password must be between 8 and 128 characters");
        }

        @Test
        @DisplayName("Should reject password exceeding maximum boundary (129 characters)")
        void shouldRejectPasswordExceedingMaxBoundary129Chars() {
            // Arrange
            String tooLongPassword = "a".repeat(129);
            ResetPasswordRequest request = new ResetPasswordRequest(VALID_TOKEN, tooLongPassword);

            // Act
            Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("Password must be between 8 and 128 characters");
        }

        @Test
        @DisplayName("Should reject null password")
        void shouldRejectNullPassword() {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest(VALID_TOKEN, null);

            // Act
            Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("New password is required");
        }

        @Test
        @DisplayName("Should reject blank password")
        void shouldRejectBlankPassword() {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest(VALID_TOKEN, "   ");

            // Act
            Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("New password is required");
        }
    }

    @Nested
    @DisplayName("ChangePasswordRequest boundary validation")
    class ChangePasswordRequestValidation {

        private static final String CURRENT_PASSWORD = "CurrentValidPassword123!";

        @Test
        @DisplayName("Should accept newPassword at minimum boundary (8 characters)")
        void shouldAcceptNewPasswordAtMinBoundary8Chars() {
            // Arrange
            String minPassword = "a".repeat(8);
            ChangePasswordRequest request = new ChangePasswordRequest(CURRENT_PASSWORD, minPassword);

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept newPassword at intermediate length (72 characters)")
        void shouldAcceptNewPasswordAtIntermediateLength72Chars() {
            // Arrange
            String intermediatePassword = "a".repeat(72);
            ChangePasswordRequest request = new ChangePasswordRequest(CURRENT_PASSWORD, intermediatePassword);

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept newPassword at maximum boundary (128 characters)")
        void shouldAcceptNewPasswordAtMaxBoundary128Chars() {
            // Arrange
            String maxPassword = "a".repeat(128);
            ChangePasswordRequest request = new ChangePasswordRequest(CURRENT_PASSWORD, maxPassword);

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject newPassword below minimum boundary (7 characters)")
        void shouldRejectNewPasswordBelowMinBoundary7Chars() {
            // Arrange
            String tooShortPassword = "a".repeat(7);
            ChangePasswordRequest request = new ChangePasswordRequest(CURRENT_PASSWORD, tooShortPassword);

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("Password must be between 8 and 128 characters");
        }

        @Test
        @DisplayName("Should reject newPassword exceeding maximum boundary (129 characters)")
        void shouldRejectNewPasswordExceedingMaxBoundary129Chars() {
            // Arrange
            String tooLongPassword = "a".repeat(129);
            ChangePasswordRequest request = new ChangePasswordRequest(CURRENT_PASSWORD, tooLongPassword);

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("Password must be between 8 and 128 characters");
        }

        @Test
        @DisplayName("Should reject null newPassword")
        void shouldRejectNullNewPassword() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(CURRENT_PASSWORD, null);

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("New password is required");
        }

        @Test
        @DisplayName("Should reject blank newPassword")
        void shouldRejectBlankNewPassword() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(CURRENT_PASSWORD, "   ");

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("New password is required");
        }

        @Test
        @DisplayName("Should accept currentPassword at maximum boundary (128 characters)")
        void shouldAcceptCurrentPasswordAtMaxBoundary128Chars() {
            // Arrange
            String maxCurrentPassword = "a".repeat(128);
            ChangePasswordRequest request = new ChangePasswordRequest(maxCurrentPassword, "ValidNewP@ss1");

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject currentPassword exceeding maximum boundary (129 characters)")
        void shouldRejectCurrentPasswordExceedingMaxBoundary129Chars() {
            // Arrange
            String tooLongCurrentPassword = "a".repeat(129);
            ChangePasswordRequest request = new ChangePasswordRequest(tooLongCurrentPassword, "ValidNewP@ss1");

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("Current password must not exceed 128 characters");
        }

        @Test
        @DisplayName("Should reject null currentPassword")
        void shouldRejectNullCurrentPassword() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(null, "ValidNewP@ss1");

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("Current password is required");
        }

        @Test
        @DisplayName("Should reject blank currentPassword")
        void shouldRejectBlankCurrentPassword() {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest("   ", "ValidNewP@ss1");

            // Act
            Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("Current password is required");
        }
    }

    @Nested
    @DisplayName("RegisterRequest boundary validation")
    class RegisterRequestValidation {

        private static final String VALID_EMAIL = "player@ordago.dev";
        private static final String VALID_GAME_NAME = "ManoMaestro";

        @Test
        @DisplayName("Should accept password at minimum boundary (8 characters)")
        void shouldAcceptPasswordAtMinBoundary8Chars() {
            // Arrange
            String minPassword = "a".repeat(8);
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, minPassword, VALID_GAME_NAME, null, null, null);

            // Act
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept password at intermediate length (72 characters)")
        void shouldAcceptPasswordAtIntermediateLength72Chars() {
            // Arrange
            String intermediatePassword = "a".repeat(72);
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, intermediatePassword, VALID_GAME_NAME, null, null, null);

            // Act
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept password at maximum boundary (128 characters)")
        void shouldAcceptPasswordAtMaxBoundary128Chars() {
            // Arrange
            String maxPassword = "a".repeat(128);
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, maxPassword, VALID_GAME_NAME, null, null, null);

            // Act
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should reject password below minimum boundary (7 characters)")
        void shouldRejectPasswordBelowMinBoundary7Chars() {
            // Arrange
            String tooShortPassword = "a".repeat(7);
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, tooShortPassword, VALID_GAME_NAME, null, null, null);

            // Act
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("Password must be between 8 and 128 characters");
        }

        @Test
        @DisplayName("Should reject password exceeding maximum boundary (129 characters)")
        void shouldRejectPasswordExceedingMaxBoundary129Chars() {
            // Arrange
            String tooLongPassword = "a".repeat(129);
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, tooLongPassword, VALID_GAME_NAME, null, null, null);

            // Act
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("Password must be between 8 and 128 characters");
        }

        @Test
        @DisplayName("Should reject null password")
        void shouldRejectNullPassword() {
            // Arrange
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, null, VALID_GAME_NAME, null, null, null);

            // Act
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .hasSize(1)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("Password is required");
        }

        @Test
        @DisplayName("Should reject blank password")
        void shouldRejectBlankPassword() {
            // Arrange
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, "   ", VALID_GAME_NAME, null, null, null);

            // Act
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

            // Assert
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("Password is required");
        }
    }
}
