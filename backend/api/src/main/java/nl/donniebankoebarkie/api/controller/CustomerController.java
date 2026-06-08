package nl.donniebankoebarkie.api.controller;

import jakarta.validation.Valid;
import nl.donniebankoebarkie.api.dto.UserResponse;
import nl.donniebankoebarkie.api.dto.customer.request.CustomerSearchRequest;
import nl.donniebankoebarkie.api.dto.customer.request.UpdateCustomerApprovalRequest;
import nl.donniebankoebarkie.api.dto.customer.response.CustomerApprovalResponse;
import nl.donniebankoebarkie.api.dto.customer.response.PagedCustomerIbanLookupResponse;
import nl.donniebankoebarkie.api.dto.customer.response.PagedCustomerSummaryResponse;
import nl.donniebankoebarkie.api.security.AuthenticatedUser;
import nl.donniebankoebarkie.api.service.interfaces.ICustomerService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    // Lists customers for employees with optional OpenAPI filters.
    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public PagedCustomerSummaryResponse listCustomers(
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return customerService.listCustomers(
                new CustomerSearchRequest(approved, firstName, lastName, email),
                pageable
        );
    }

    // Lets customers look up active IBANs by name without exposing full profiles.
    @GetMapping("/lookup")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PagedCustomerIbanLookupResponse lookupCustomerIbans(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return customerService.lookupCustomerIbans(firstName, lastName, pageable);
    }

    // Lists customers that still need employee approval.
    @GetMapping("/pending")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public PagedCustomerSummaryResponse listPendingCustomers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return customerService.listPendingCustomers(
                new CustomerSearchRequest(null, firstName, lastName, email),
                pageable
        );
    }

    // Returns one customer profile for employee detail views.
    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public UserResponse getCustomer(@PathVariable Long customerId) {
        return customerService.getCustomer(customerId);
    }

    // Approves a pending customer and creates the required customer accounts.
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
