package nl.donniebankoebarkie.api.controller;

import jakarta.validation.Valid;
import nl.donniebankoebarkie.api.dto.customer.request.UpdateCustomerApprovalRequest;
import nl.donniebankoebarkie.api.dto.customer.response.CustomerApprovalResponse;
import nl.donniebankoebarkie.api.dto.customer.response.PagedCustomerSummaryResponse;
import nl.donniebankoebarkie.api.security.AuthenticatedUser;
import nl.donniebankoebarkie.api.service.interfaces.ICustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final ICustomerService customerService;

    public CustomerController(ICustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public PagedCustomerSummaryResponse listPendingCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return customerService.listPendingCustomers(page, size);
    }

    @PatchMapping("/{customerId}/approval")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public CustomerApprovalResponse approveCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody UpdateCustomerApprovalRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return customerService.approveCustomer(customerId, authenticatedUser.userId(), request);
    }
}
