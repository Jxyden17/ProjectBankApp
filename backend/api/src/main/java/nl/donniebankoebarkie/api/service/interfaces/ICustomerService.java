package nl.donniebankoebarkie.api.service.interfaces;

import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.dto.customer.request.CustomerSearchRequest;
import nl.donniebankoebarkie.api.dto.customer.request.UpdateCustomerApprovalRequest;
import nl.donniebankoebarkie.api.dto.customer.response.CustomerApprovalResponse;
import nl.donniebankoebarkie.api.dto.customer.response.PagedCustomerIbanLookupResponse;
import nl.donniebankoebarkie.api.dto.customer.response.PagedCustomerSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface ICustomerService {
    PagedCustomerSummaryResponse listCustomers(CustomerSearchRequest request, Pageable pageable);

    PagedCustomerSummaryResponse listPendingCustomers(CustomerSearchRequest request, Pageable pageable);

    UserResponse getCustomer(Long customerId);

    PagedCustomerIbanLookupResponse lookupCustomerIbans(String firstName, String lastName, Pageable pageable);

    CustomerApprovalResponse approveCustomer(Long customerId, Long approvedByUserId, UpdateCustomerApprovalRequest request);
}
