package nl.donniebankoebarkie.api.dto.customer.response;

public record CustomerSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean approved
) {
}
