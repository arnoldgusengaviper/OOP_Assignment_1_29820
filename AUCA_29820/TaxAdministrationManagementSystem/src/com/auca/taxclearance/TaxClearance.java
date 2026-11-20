package com.auca.taxclearance;

import java.time.LocalDate;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.InvalidStatusTransitionException;
import com.auca.exceptions.PrerequisiteMissingException;
import com.auca.exceptions.UnauthorizedApprovalException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.revenueofficer.RevenueOfficer;

public class TaxClearance implements Identifiable, Validatable {

    // Six required fields
    private String clearanceId;
    private RegisteredTaxpayer taxpayer;
    private LocalDate requestDate;
    private LocalDate expiryDate;
    private String status;
    private RevenueOfficer approvedBy;

    // A new request is PENDING and has no approving officer yet
    public TaxClearance(String clearanceId, RegisteredTaxpayer taxpayer,
            LocalDate requestDate, LocalDate expiryDate)
            throws ValidationException {
        setClearanceId(clearanceId);
        setTaxpayer(taxpayer);
        setDates(requestDate, expiryDate);
        this.status = "PENDING";
        this.approvedBy = null;
    }

    // Getters
    public String getClearanceId() {
        return clearanceId;
    }

    public RegisteredTaxpayer getTaxpayer() {
        return taxpayer;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public RevenueOfficer getApprovedBy() {
        return approvedBy;
    }

    // Setters with validation
    public void setClearanceId(String clearanceId) throws ValidationException {
        if (clearanceId == null || !clearanceId.trim().matches("[A-Za-z0-9-]{3,30}")) {
            throw new ValidationException(
                    "Clearance ID must be 3-30 letters, digits or hyphens.");
        }
        this.clearanceId = clearanceId.trim();
    }

    public void setTaxpayer(RegisteredTaxpayer taxpayer) throws ValidationException {
        if (taxpayer == null) {
            throw new ValidationException("A registered taxpayer is required.");
        }
        this.taxpayer = taxpayer;
    }

    // The two dates depend on each other, so they are checked together
    public void setDates(LocalDate requestDate, LocalDate expiryDate)
            throws ValidationException {
        if (requestDate == null || expiryDate == null) {
            throw new ValidationException("Request date and expiry date are required.");
        }
        if (requestDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Request date cannot be in the future.");
        }
        if (!expiryDate.isAfter(requestDate)) {
            throw new ValidationException("Expiry date must be after the request date.");
        }
        this.requestDate = requestDate;
        this.expiryDate = expiryDate;
    }

    // Business methods (documented domain extensions)

    public boolean isExpired(LocalDate today) {
        return today.isAfter(expiryDate);
    }

    // Main counts the two numbers; the class decides.
    public void approve(RevenueOfficer officer, int overdueUnpaidObligations,
            int unresolvedDeclarations)
            throws InvalidStatusTransitionException, UnauthorizedApprovalException,
            PrerequisiteMissingException {
        if (!status.equals("PENDING")) {
            throw new InvalidStatusTransitionException(
                    "Only a PENDING clearance can be approved (current: " + status + ").");
        }
        if (officer == null || !officer.isActive()) {
            throw new UnauthorizedApprovalException("The approving officer must be ACTIVE.");
        }
        if (!taxpayer.isActive()) {
            throw new PrerequisiteMissingException("The taxpayer must be ACTIVE.");
        }
        if (overdueUnpaidObligations > 0) {
            throw new PrerequisiteMissingException("The taxpayer has "
                    + overdueUnpaidObligations + " overdue unpaid obligation(s).");
        }
        if (unresolvedDeclarations > 0) {
            throw new PrerequisiteMissingException("The taxpayer has "
                    + unresolvedDeclarations + " unresolved declaration(s).");
        }
        this.approvedBy = officer;
        this.status = "APPROVED";
    }

    public void reject() throws InvalidStatusTransitionException {
        if (!status.equals("PENDING")) {
            throw new InvalidStatusTransitionException(
                    "Only a PENDING clearance can be rejected (current: " + status + ").");
        }
        this.status = "REJECTED";
    }

    // Identifiable
    @Override
    public String getId() {
        return clearanceId;
    }

    @Override
    public String getDisplayName() {
        return "Clearance " + clearanceId + " - " + taxpayer.getDisplayName();
    }

    @Override
    public boolean isActive() {
        return !status.equals("REJECTED");
    }

    // Validatable
    @Override
    public void validate() throws ValidationException {
        setClearanceId(clearanceId);
        setTaxpayer(taxpayer);
        setDates(requestDate, expiryDate);
        if (status.equals("APPROVED") && approvedBy == null) {
            throw new ValidationException("An APPROVED clearance must store its approving officer.");
        }
    }

    @Override
    public String getSummary() {
        return "Clearance | " + clearanceId + " | TIN " + taxpayer.getTin() + " | "
                + requestDate + " to " + expiryDate + " | " + status
                + " | by " + (approvedBy == null ? "-" : approvedBy.getStaffNumber());
    }

    @Override
    public void printDetails() {
        System.out.println("Clearance ID : " + clearanceId);
        System.out.println("Taxpayer     : " + taxpayer.getDisplayName()
                + " (TIN " + taxpayer.getTin() + ")");
        System.out.println("Request date : " + requestDate);
        System.out.println("Expiry date  : " + expiryDate);
        System.out.println("Status       : " + status);
        System.out.println("Approved by  : "
                + (approvedBy == null ? "not yet approved" : approvedBy.getDisplayName()));
    }
}