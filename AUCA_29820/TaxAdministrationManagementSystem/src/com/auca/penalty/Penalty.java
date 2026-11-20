package com.auca.penalty;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.taxobligation.TaxObligation;

public class Penalty implements Identifiable, Validatable {

    // Illustrative rate from the assignment, NOT a legal rate
    public static final BigDecimal LATE_FILING_RATE = new BigDecimal("5.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");

    // Six required fields
    private String penaltyId;
    private TaxObligation obligation;
    private String penaltyType;
    private BigDecimal baseAmount;
    private BigDecimal penaltyRate;
    private BigDecimal penaltyAmount;

    // penaltyAmount is NOT a parameter: the class calculates it
    public Penalty(String penaltyId, TaxObligation obligation, String penaltyType,
            BigDecimal baseAmount, BigDecimal penaltyRate)
            throws ValidationException {
        setPenaltyId(penaltyId);
        setObligation(obligation);
        setPenaltyType(penaltyType);
        setBaseAmount(baseAmount);
        setPenaltyRate(penaltyRate);
    }

    // Getters
    public String getPenaltyId() {
        return penaltyId;
    }

    public TaxObligation getObligation() {
        return obligation;
    }

    public String getPenaltyType() {
        return penaltyType;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public BigDecimal getPenaltyRate() {
        return penaltyRate;
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount;
    }

    // Setters with validation
    public void setPenaltyId(String penaltyId) throws ValidationException {
        if (penaltyId == null || !penaltyId.trim().matches("[A-Za-z0-9-]{3,30}")) {
            throw new ValidationException(
                    "Penalty ID must be 3-30 letters, digits or hyphens.");
        }
        this.penaltyId = penaltyId.trim();
    }

    public void setObligation(TaxObligation obligation) throws ValidationException {
        if (obligation == null) {
            throw new ValidationException("A tax obligation is required.");
        }
        this.obligation = obligation;
    }

    public void setPenaltyType(String penaltyType) throws ValidationException {
        if (penaltyType == null) {
            throw new ValidationException("Penalty type is required.");
        }
        String t = penaltyType.trim().toUpperCase();
        if (!t.equals("LATE_FILING") && !t.equals("LATE_PAYMENT")) {
            throw new ValidationException("Penalty type must be LATE_FILING or LATE_PAYMENT.");
        }
        this.penaltyType = t;
    }

    public void setBaseAmount(BigDecimal baseAmount) throws ValidationException {
        if (baseAmount == null || baseAmount.signum() < 0) {
            throw new ValidationException("Base amount cannot be negative.");
        }
        if (baseAmount.scale() > 2) {
            throw new ValidationException("Base amount allows at most 2 decimal places.");
        }
        if (baseAmount.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException("Base amount is too large.");
        }
        this.baseAmount = baseAmount;
        recalculate();
    }

    public void setPenaltyRate(BigDecimal penaltyRate) throws ValidationException {
        if (penaltyRate == null || penaltyRate.signum() < 0
                || penaltyRate.compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException("Penalty rate must be between 0 and 100 inclusive.");
        }
        if (penaltyRate.scale() > 2) {
            throw new ValidationException("Penalty rate allows at most 2 decimal places.");
        }
        this.penaltyRate = penaltyRate;
        recalculate();
    }

    // penaltyAmount = baseAmount x penaltyRate / 100, HALF_UP, 2 decimals
    private void recalculate() {
        if (baseAmount != null && penaltyRate != null) {
            this.penaltyAmount = baseAmount.multiply(penaltyRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
    }

    // Identifiable
    @Override
    public String getId() {
        return penaltyId;
    }

    @Override
    public String getDisplayName() {
        return penaltyType + " penalty on " + obligation.getId();
    }

    // A zero penalty has no effect, so it does not count as active
    @Override
    public boolean isActive() {
        return penaltyAmount.signum() > 0;
    }

    // Validatable
    @Override
    public void validate() throws ValidationException {
        setPenaltyId(penaltyId);
        setObligation(obligation);
        setPenaltyType(penaltyType);
        setBaseAmount(baseAmount);
        setPenaltyRate(penaltyRate);
    }

    @Override
    public String getSummary() {
        return "Penalty | " + penaltyId + " | " + obligation.getId() + " | "
                + penaltyType + " | base " + baseAmount + " | " + penaltyRate
                + "% | amount " + penaltyAmount;
    }

    @Override
    public void printDetails() {
        System.out.println("Penalty ID     : " + penaltyId);
        System.out.println("Obligation     : " + obligation.getId() + " - "
                + obligation.getTaxpayer().getDisplayName());
        System.out.println("Type           : " + penaltyType);
        System.out.println("Base amount    : " + baseAmount);
        System.out.println("Rate (%)       : " + penaltyRate);
        System.out.println("Penalty amount : " + penaltyAmount);
    }
}