package com.auca.taxobligation;

import java.time.LocalDate;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.InvalidTaxPeriodException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.taxtype.TaxType;

public class TaxObligation implements Identifiable, Validatable {

    // Six required fields
    private String obligationId;
    private RegisteredTaxpayer taxpayer;
    private TaxType taxType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate dueDate;

    // Constructor
    public TaxObligation(String obligationId, RegisteredTaxpayer taxpayer,
            TaxType taxType, LocalDate periodStart,
            LocalDate periodEnd, LocalDate dueDate)
            throws ValidationException {
        setObligationId(obligationId);
        setTaxpayer(taxpayer);
        setTaxType(taxType);
        setPeriod(periodStart, periodEnd, dueDate);
    }

    // Getters
    public String getObligationId() {
        return obligationId;
    }

    public RegisteredTaxpayer getTaxpayer() {
        return taxpayer;
    }

    public TaxType getTaxType() {
        return taxType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    // Setters with validation
    public void setObligationId(String obligationId) throws ValidationException {
        if (obligationId == null || !obligationId.trim().matches("[A-Za-z0-9-]{3,30}")) {
            throw new ValidationException(
                    "Obligation ID must be 3-30 letters, digits or hyphens.");
        }
        this.obligationId = obligationId.trim();
    }

    public void setTaxpayer(RegisteredTaxpayer taxpayer) throws ValidationException {
        if (taxpayer == null) {
            throw new ValidationException("A registered taxpayer is required.");
        }
        if (!taxpayer.isActive()) {
            throw new ValidationException("The taxpayer must be ACTIVE.");
        }
        this.taxpayer = taxpayer;
    }

    public void setTaxType(TaxType taxType) throws ValidationException {
        if (taxType == null) {
            throw new ValidationException("A tax type is required.");
        }
        if (!taxType.isActive()) {
            throw new ValidationException("The tax type must be ACTIVE.");
        }
        this.taxType = taxType;
    }

    // The three dates depend on each other, so they are checked together
    public void setPeriod(LocalDate periodStart, LocalDate periodEnd, LocalDate dueDate)
            throws ValidationException {
        if (periodStart == null || periodEnd == null || dueDate == null) {
            throw new ValidationException(
                    "Period start, period end and due date are all required.");
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new InvalidTaxPeriodException(
                    "Period start must be on or before period end.");
        }
        if (dueDate.isBefore(periodEnd)) {
            throw new InvalidTaxPeriodException(
                    "Due date must not be before the period end.");
        }
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dueDate = dueDate;
    }

    // Business methods (documented domain extensions)

    // True if the two obligations are for the same taxpayer and tax type
    // and their periods share at least one day. Used by Main to block overlaps.
    public boolean overlaps(TaxObligation other) {
        if (other == null)
            return false;
        boolean sameTaxpayer = taxpayer.getTin().equals(other.taxpayer.getTin());
        boolean sameTaxType = taxType.getTaxTypeId().equals(other.taxType.getTaxTypeId());
        boolean periodsOverlap = !periodStart.isAfter(other.periodEnd)
                && !other.periodStart.isAfter(periodEnd);
        return sameTaxpayer && sameTaxType && periodsOverlap;
    }

    // True if the due date has passed on the given day
    public boolean isOverdue(LocalDate today) {
        return today.isAfter(dueDate);
    }

    // Identifiable
    @Override
    public String getId() {
        return obligationId;
    }

    @Override
    public String getDisplayName() {
        return taxType.getTaxCode() + " " + periodStart + " to " + periodEnd
                + " - " + taxpayer.getDisplayName();
    }

    @Override
    public boolean isActive() {
        return taxpayer.isActive() && taxType.isActive();
    }

    // Validatable
    @Override
    public void validate() throws ValidationException {
        setObligationId(obligationId);
        setTaxpayer(taxpayer);
        setTaxType(taxType);
        setPeriod(periodStart, periodEnd, dueDate);
    }

    @Override
    public String getSummary() {
        return "Obligation | " + obligationId + " | TIN " + taxpayer.getTin()
                + " | " + taxType.getTaxCode() + " | " + periodStart + " to "
                + periodEnd + " | due " + dueDate;
    }

    @Override
    public void printDetails() {
        System.out.println("Obligation ID : " + obligationId);
        System.out.println("Taxpayer      : " + taxpayer.getDisplayName()
                + " (TIN " + taxpayer.getTin() + ")");
        System.out.println("Tax type      : " + taxType.getDisplayName());
        System.out.println("Period start  : " + periodStart);
        System.out.println("Period end    : " + periodEnd);
        System.out.println("Due date      : " + dueDate);
    }
}