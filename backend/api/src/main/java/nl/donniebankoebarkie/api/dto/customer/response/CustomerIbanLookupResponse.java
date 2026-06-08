package nl.donniebankoebarkie.api.dto.customer.response;

// Returns only the customer name and IBAN for lookup results.
public record CustomerIbanLookupResponse(
        String firstName,
        String lastName,
        String iban
) {
}
