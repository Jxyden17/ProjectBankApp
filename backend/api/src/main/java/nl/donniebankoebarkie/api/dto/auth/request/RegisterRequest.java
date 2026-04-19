package nl.donniebankoebarkie.api.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "firstName is required.")
        @Size(max = 100, message = "firstName must be at most 100 characters.")
        String firstName,
        @NotBlank(message = "lastName is required.")
        @Size(max = 100, message = "lastName must be at most 100 characters.")
        String lastName,
        @NotBlank(message = "email is required.")
        @Email(message = "email must be a valid email address.")
        @Size(max = 255, message = "email must be at most 255 characters.")
        String email,
        @NotBlank(message = "password is required.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "password must be at least 8 characters and include uppercase, lowercase, and a digit."
        )
        String password,
        @NotBlank(message = "phoneNumber is required.")
        @Size(max = 32, message = "phoneNumber must be at most 32 characters.")
        String phoneNumber,
        @NotBlank(message = "bsnNumber is required.")
        @Pattern(regexp = "^\\d{9}$", message = "bsnNumber must be exactly 9 digits.")
        String bsnNumber
) {
}
