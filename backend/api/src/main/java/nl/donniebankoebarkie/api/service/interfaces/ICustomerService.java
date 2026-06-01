package nl.donniebankoebarkie.api.service.interfaces;

import nl.donniebankoebarkie.api.dto.customer.request.UpdateCustomerApprovalRequest;
import nl.donniebankoebarkie.api.dto.customer.response.CustomerApprovalResponse;
import nl.donniebankoebarkie.api.dto.customer.response.PagedCustomerSummaryResponse;

public interface ICustomerService {
    PagedCustomerSummaryResponse listPendingCustomers(int page, int size);

    CustomerApprovalResponse approveCustomer(Long customerId, Long approvedByUserId, UpdateCustomerApprovalRequest request);
}
