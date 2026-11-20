package com.auca.revenueofficer;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.auca.abstracts.TaxParty;
import com.auca.exceptions.ValidationException;

public class RevenueOfficer extends TaxParty {

    // Six required fields
    private String staffNumber;
    private String department;
    private String role;
    private LocalDate hireDate;
    private BigDecimal approvalLimit;
    private boolean active;

    // Constructor
    public RevenueOfficer(String partyId, String firstName, String lastName,
                          String email, String phoneNumber, LocalDate dateOfBirth,
                          String staffNumber, String department, String role,
                          LocalDate hireDate, BigDecimal approvalLimit,
                          boolean active) throws ValidationException {

        super(partyId, firstName, lastName, email, phoneNumber, dateOfBirth);

        setStaffNumber(staffNumber);
        setDepartment(department);
        setRole(role);
        setHireDate(hireDate);
        setApprovalLimit(approvalLimit);
        this.active = active;
    }

    // Getters
    public String getStaffNumber() { return staffNumber; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
    public LocalDate getHireDate() { return hireDate; }
    public BigDecimal getApprovalLimit() { return approvalLimit; }

    // Setters with validation
    public void setStaffNumber(String staffNumber) throws ValidationException {
        if (staffNumber == null || !staffNumber.trim().matches("[A-Za-z0-9-]{3,20}")) {
            throw new ValidationException(
                "Staff number must be 3-20 letters, digits or hyphens.");
        }
        this.staffNumber = staffNumber.trim();
    }

    public void setDepartment(String department) throws ValidationException {
        if (department == null || department.trim().length() < 2
                || department.trim().length() > 60) {
            throw new ValidationException("Department must be 2-60 characters.");
        }
        this.department = department.trim();
    }

    public void setRole(String role) throws ValidationException {
        if (role == null || role.trim().length() < 2 || role.trim().length() > 60) {
            throw new ValidationException("Role must be 2-60 characters.");
        }
        this.role = role.trim();
    }

    public void setHireDate(LocalDate hireDate) throws ValidationException {
        if (hireDate == null) {
            throw new ValidationException("Hire date is required.");
        }
        if (hireDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Hire date cannot be in the future.");
        }
        this.hireDate = hireDate;
    }

    public void setApprovalLimit(BigDecimal approvalLimit) throws ValidationException {
        if (approvalLimit == null || approvalLimit.signum() < 0) {
            throw new ValidationException("Approval limit cannot be negative.");
        }
        if (approvalLimit.scale() > 2) {
            throw new ValidationException("Approval limit allows at most 2 decimal places.");
        }
        if (approvalLimit.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new ValidationException("Approval limit is too large.");
        }
        this.approvalLimit = approvalLimit;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Role-specific business method (used later for refunds)
    public boolean canApprove(BigDecimal amount) {
        return active && amount != null && amount.compareTo(approvalLimit) <= 0;
    }

    // Implementing the abstract methods
    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void validate() throws ValidationException {
        setStaffNumber(staffNumber);
        setDepartment(department);
        setRole(role);
        setHireDate(hireDate);
        setApprovalLimit(approvalLimit);
        if (!hireDate.isAfter(getDateOfBirth())) {
            throw new ValidationException("Hire date must be after the date of birth.");
        }
    }

    // Overriding methods
    @Override
    public String getDisplayName() {
        return getFirstName() + " " + getLastName() + " (Officer)";
    }

    @Override
    public String getSummary() {
        return "Officer | Staff " + staffNumber + " | " + getFirstName() + " "
                + getLastName() + " | " + role + " | Limit " + approvalLimit
                + " | " + (active ? "ACTIVE" : "INACTIVE");
    }

    @Override
    public void printDetails() {
        super.printDetails();
        System.out.println("Staff number    : " + staffNumber);
        System.out.println("Department      : " + department);
        System.out.println("Role            : " + role);
        System.out.println("Hire date       : " + hireDate);
        System.out.println("Approval limit  : " + approvalLimit);
        System.out.println("Active          : " + (active ? "YES" : "NO"));
    }
}