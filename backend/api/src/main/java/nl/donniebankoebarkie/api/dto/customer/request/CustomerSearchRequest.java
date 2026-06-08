package nl.donniebankoebarkie.api.dto.customer.request;

// Holds optional employee filters for customer list endpoints.
public record CustomerSearchRequest(
        Boolean approved,
        String firstName,
        String lastName,
        String email
) {
}
