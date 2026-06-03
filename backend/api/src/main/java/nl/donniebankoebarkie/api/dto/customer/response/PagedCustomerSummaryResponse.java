package nl.donniebankoebarkie.api.dto.customer.response;

import nl.donniebankoebarkie.api.dto.PageMetadata;

import java.util.List;

public record PagedCustomerSummaryResponse(
        List<CustomerSummaryResponse> items,
        PageMetadata page
) {
}
