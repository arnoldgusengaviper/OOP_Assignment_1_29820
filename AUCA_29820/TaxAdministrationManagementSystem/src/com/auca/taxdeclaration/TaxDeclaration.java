package com.auca.taxdeclaration;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.auca.exceptions.InvalidStatusTransitionException;
import com.auca.exceptions.InvalidTaxCalculationException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.taxobligation.TaxObligation;

public class TaxDeclaration implements Identifiable, Validatable {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    // Six required fields
    private String declarationId;
    private TaxObligation obligation;
    private LocalDate filingDate;
    private BigDecimal taxableAmount;
    private BigDecimal declaredTax;
    private String status;

    // Constructor (status is NOT passed in: the class decides it)
    public TaxDeclaration(String declarationId, TaxObligation obligation,
                          LocalDate filingDate, BigDecimal taxableAmount,
                          BigDecimal declaredTax) throws ValidationException {
        setDeclarationId(declarationId);
        setObligation(obligation);
        setFilingDate(filingDate);
        setTaxableAmount(taxableAmount);
        setDeclaredTax(declaredTax);
        checkCalculation();
        this.status = isLate() ? "LATE" : "SUBMITTED";
    }

    // Getters
    public String getDeclarationId() { return declarationId; }
    public TaxObligation getObligation() { return obligation; }
    public LocalDate getFilingDate() { return filingDate; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public BigDecimal getDeclaredTax() { return declaredTax; }
    public String getStatus() { return status; }

    // Setters with validation
    public void setDeclarationId(String declarationId) throws ValidationException {
        if (declarationId == null || !declarationId.trim().matches("[A-Za-z0-9-]{3,30}")) {
            throw new ValidationException(
                "Declaration ID must be 3-30 letters, digits or hyphens.");
        }
        this.declarationId = declarationId.trim();
    }

    public void setObligation(TaxObligation obligation) throws ValidationException {
        if (obligation == null) {
            throw new ValidationException("A tax obligation is required.");
        }
        if (!obligation.isActive()) {
            throw new ValidationException("The obligation must be active.");
        }
        this.obligation = obligation;
    }

    public void setFilingDate(LocalDate filingDate) throws ValidationException {
        if (filingDate == null) {
            throw new ValidationException("Filing date is required.");
        }
        if (filingDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Filing date cannot be in the future.");
        }
        this.filingDate = filingDate;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) throws ValidationException {
        checkMoney(taxableAmount, "Taxable amount");
        this.taxableAmount = taxableAmount;
    }

    public void setDeclaredTax(BigDecimal declaredTax) throws ValidationException {
        checkMoney(declaredTax, "Declared tax");
        this.declaredTax = declaredTax;
    }

    // Shared money rules (negative, 2 decimals, maximum)
    private void checkMoney(BigDecimal value, String label) throws ValidationException {
        if (value == null || value.signum() < 0) {
            throw new ValidationException(label + " cannot be negative.");
        }
        if (value.scale() > 2) {
            throw new ValidationException(label + " allows at most 2 decimal places.");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException(label + " is too large.");
        }
    }

    // Business methods (documented domain extensions)

    // Expected tax from the rate stored in the TaxType
    public BigDecimal getExpectedTax() {
        return obligation.getTaxType().calculateTax(taxableAmount);
    }

    // Declared tax must be within 0.01 of the expected tax
    public void checkCalculation() throws InvalidTaxCalculationException {
        BigDecimal difference = getExpectedTax().subtract(declaredTax).abs();
        if (difference.compareTo(TOLERANCE) > 0) {
            throw new InvalidTaxCalculationException("Declared tax " + declaredTax
                    + " does not match expected tax " + getExpectedTax() + ".");
        }
    }

    // Late means filed after the due date. The filing date is never changed.
    public boolean isLate() {
        return filingDate.isAfter(obligation.getDueDate());
    }

    // Allowed moves: SUBMITTED or LATE -> ASSESSED or REJECTED. Nothing else.
    public void changeStatus(String newStatus) throws InvalidStatusTransitionException {
        if (newStatus == null) {
            throw new InvalidStatusTransitionException("New status is required.");
        }
        String target = newStatus.trim().toUpperCase();
        if (!target.equals("ASSESSED") && !target.equals("REJECTED")) {
            throw new InvalidStatusTransitionException(
                "Status can only change to ASSESSED or REJECTED.");
        }
        if (!status.equals("SUBMITTED") && !status.equals("LATE")) {
            throw new InvalidStatusTransitionException(
                "Cannot change status from " + status + " to " + target + ".");
        }
        this.status = target;
    }

    // Identifiable
    @Override
    public String getId() {
        return declarationId;
    }

    @Override
    public String getDisplayName() {
        return "Declaration " + declarationId + " - " + obligation.getDisplayName();
    }

    // A rejected declaration no longer counts as the active one for the obligation
    @Override
    public boolean isActive() {
        return !status.equals("REJECTED");
    }

    // Validatable
    @Override
    public void validate() throws ValidationException {
        setDeclarationId(declarationId);
        setObligation(obligation);
        setFilingDate(filingDate);
        setTaxableAmount(taxableAmount);
        setDeclaredTax(declaredTax);
        checkCalculation();
    }

    @Override
    public String getSummary() {
        return "Declaration | " + declarationId + " | " + obligation.getId()
                + " | TIN " + obligation.getTaxpayer().getTin() + " | filed "
                + filingDate + " | taxable " + taxableAmount + " | tax "
                + declaredTax + " | " + status;
    }

    @Override
    public void printDetails() {
        System.out.println("Declaration ID : " + declarationId);
        System.out.println("Obligation     : " + obligation.getId() + " ("
                + obligation.getTaxType().getTaxCode() + " "
                + obligation.getPeriodStart() + " to " + obligation.getPeriodEnd() + ")");
        System.out.println("Taxpayer       : " + obligation.getTaxpayer().getDisplayName());
        System.out.println("Due date       : " + obligation.getDueDate());
        System.out.println("Filing date    : " + filingDate);
        System.out.println("Taxable amount : " + taxableAmount);
        System.out.println("Declared tax   : " + declaredTax);
        System.out.println("Expected tax   : " + getExpectedTax());
        System.out.println("Status         : " + status);
    }
}