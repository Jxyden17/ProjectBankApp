Feature: Employee customer approval
  Employees can view registered customers without accounts and approve them.

  Scenario: Employee views customers without accounts
    Given an employee exists
    And a pending customer "pending.customer@example.com" exists
    And an approved customer "approved.customer@example.com" exists
    When the employee requests pending customers
    Then the pending customer list contains "pending.customer@example.com"
    And the pending customer list does not contain "approved.customer@example.com"

  Scenario: Employee approves a pending customer
    Given an employee exists
    And a pending customer "approval.customer@example.com" exists
    When the employee approves "approval.customer@example.com" with default account limits
    Then the approval response contains 2 accounts
    And the customer "approval.customer@example.com" is approved
    And the customer "approval.customer@example.com" has checking and savings accounts

  Scenario: Customer cannot view pending approvals
    Given a pending customer "forbidden.customer@example.com" exists
    When the customer requests pending customers
    Then the response status is 403
