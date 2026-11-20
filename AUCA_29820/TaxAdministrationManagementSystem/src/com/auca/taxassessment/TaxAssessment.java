package com.auca.taxassessment;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.auca.exceptions.InvalidStatusTransitionException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.taxdeclaration.TaxDeclaration;

public class TaxAssessment implements Identifiable, Validatable {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");

    // Six required fields
    private String assessmentId;
    private TaxDeclaration declaration;
    private BigDecimal assessedTax;
    private BigDecimal adjustmentAmount;
    private LocalDate assessmentDate;
    private String status;

    // Constructor (a new assessment always starts as DRAFT)
    public TaxAssessment(String assessmentId, TaxDeclaration declaration,
            BigDecimal assessedTax, BigDecimal adjustmentAmount,
            LocalDate assessmentDate) throws ValidationException {
        setAssessmentId(assessmentId);
        setDeclaration(declaration);
        checkAssessable(declaration);
        setAmounts(assessedTax, adjustmentAmount);
        setAssessmentDate(assessmentDate);
        this.status = "DRAFT";
    }

    // Getters
    public String getAssessmentId() {
        return assessmentId;
    }

    public TaxDeclaration getDeclaration() {
        return declaration;
    }

    public BigDecimal getAssessedTax() {
        return assessedTax;
    }

    public BigDecimal getAdjustmentAmount() {
        return adjustmentAmount;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public String getStatus() {
        return status;
    }

    // Setters with validation
    public void setAssessmentId(String assessmentId) throws ValidationException {
        if (assessmentId == null || !assessmentId.trim().matches("[A-Za-z0-9-]{3,30}")) {
            throw new ValidationException(
                    "Assessment ID must be 3-30 letters, digits or hyphens.");
        }
        this.assessmentId = assessmentId.trim();
    }

    public void setDeclaration(TaxDeclaration declaration) throws ValidationException {
        if (declaration == null) {
            throw new ValidationException("A tax declaration is required.");
        }
        this.declaration = declaration;
    }

    // Only a SUBMITTED or LATE declaration can be assessed
    private void checkAssessable(TaxDeclaration d) throws ValidationException {
        if (d.getStatus().equals("REJECTED")) {
            throw new ValidationException("A rejected declaration cannot be assessed.");
        }
        if (!d.getStatus().equals("SUBMITTED") && !d.getStatus().equals("LATE")) {
            throw new ValidationException("Declaration is already " + d.getStatus()
                    + " and cannot be assessed again.");
        }
    }

    // The two amounts depend on each other (total >= 0), so they are checked
    // together
    public void setAmounts(BigDecimal assessedTax, BigDecimal adjustmentAmount)
            throws ValidationException {
        if (assessedTax == null || assessedTax.signum() < 0) {
            throw new ValidationException("Assessed tax cannot be negative.");
        }
        if (adjustmentAmount == null) {
            throw new ValidationException("Adjustment amount is required (use 0 for none).");
        }
        if (assessedTax.scale() > 2 || adjustmentAmount.scale() > 2) {
            throw new ValidationException("Amounts allow at most 2 decimal places.");
        }
        if (assessedTax.compareTo(MAX_AMOUNT) > 0
                || adjustmentAmount.abs().compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException("An amount is too large.");
        }
        if (assessedTax.add(adjustmentAmount).signum() < 0) {
            throw new ValidationException("Assessment total cannot be negative.");
        }
        this.assessedTax = assessedTax;
        this.adjustmentAmount = adjustmentAmount;
    }

    public void setAssessmentDate(LocalDate assessmentDate) throws ValidationException {
        if (assessmentDate == null) {
            throw new ValidationException("Assessment date is required.");
        }
        if (assessmentDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Assessment date cannot be in the future.");
        }
        if (assessmentDate.isBefore(declaration.getFilingDate())) {
            throw new ValidationException(
                    "Assessment date cannot be before the declaration filing date.");
        }
        this.assessmentDate = assessmentDate;
    }

    // Business methods (documented domain extensions)

    // Total = assessedTax + adjustmentAmount
    public BigDecimal getTotal() {
        return assessedTax.add(adjustmentAmount);
    }

    public boolean isConfirmed() {
        return status.equals("CONFIRMED");
    }

    // DRAFT -> CONFIRMED. Also marks the declaration ASSESSED.
    public void confirm() throws InvalidStatusTransitionException {
        if (!status.equals("DRAFT")) {
            throw new InvalidStatusTransitionException(
                    "Only a DRAFT assessment can be confirmed (current: " + status + ").");
        }
        declaration.changeStatus("ASSESSED"); // may throw: nothing changed yet
        this.status = "CONFIRMED";
    }

    // DRAFT -> CANCELLED. A confirmed assessment is locked.
    public void cancel() throws InvalidStatusTransitionException {
        if (!status.equals("DRAFT")) {
            throw new InvalidStatusTransitionException(
                    "Only a DRAFT assessment can be cancelled (current: " + status + ").");
        }
        this.status = "CANCELLED";
    }

    // Identifiable
    @Override
    public String getId() {
        return assessmentId;
    }

    @Override
    public String getDisplayName() {
        return "Assessment " + assessmentId + " - " + declaration.getObligation().getTaxpayer().getDisplayName();
    }

    @Override
    public boolean isActive() {
        return !status.equals("CANCELLED");
    }

    // Validatable
    @Override
    public void validate() throws ValidationException {
        setAssessmentId(assessmentId);
        setDeclaration(declaration);
        if (status.equals("DRAFT")) {
            checkAssessable(declaration); // a draft needs a still-assessable declaration
        }
        setAmounts(assessedTax, adjustmentAmount);
        setAssessmentDate(assessmentDate);
    }

    @Override
    public String getSummary() {
        return "Assessment | " + assessmentId + " | " + declaration.getId()
                + " | TIN " + declaration.getObligation().getTaxpayer().getTin()
                + " | assessed " + assessedTax + " | adj " + adjustmentAmount
                + " | total " + getTotal() + " | " + status;
    }

    @Override
    public void printDetails() {
        System.out.println("Assessment ID   : " + assessmentId);
        System.out.println("Declaration     : " + declaration.getId() + " ("
                + declaration.getStatus() + ")");
        System.out.println("Taxpayer        : "
                + declaration.getObligation().getTaxpayer().getDisplayName());
        System.out.println("Declared tax    : " + declaration.getDeclaredTax());
        System.out.println("Assessed tax    : " + assessedTax);
        System.out.println("Adjustment      : " + adjustmentAmount);
        System.out.println("TOTAL           : " + getTotal());
        System.out.println("Assessment date : " + assessmentDate);
        System.out.println("Status          : " + status);
    }
}