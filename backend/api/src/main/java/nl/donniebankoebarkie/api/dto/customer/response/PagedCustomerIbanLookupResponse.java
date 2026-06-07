package nl.donniebankoebarkie.api.dto.customer.response;

import nl.donniebankoebarkie.api.dto.PageMetadata;

import java.util.List;

// Wraps IBAN lookup results with pagination metadata.
public record PagedCustomerIbanLookupResponse(
        List<CustomerIbanLookupResponse> items,
        PageMetadata page
) {
}
