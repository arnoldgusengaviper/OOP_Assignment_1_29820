package com.auca.taxtype;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;

public class TaxType implements Identifiable, Validatable {

    // Six required fields
    private String taxTypeId;
    private String taxCode;
    private String taxName;
    private BigDecimal rate;
    private String filingFrequency;
    private boolean active;

    // Constructor
    public TaxType(String taxTypeId, String taxCode, String taxName,
            BigDecimal rate, String filingFrequency, boolean active)
            throws ValidationException {
        setTaxTypeId(taxTypeId);
        setTaxCode(taxCode);
        setTaxName(taxName);
        setRate(rate);
        setFilingFrequency(filingFrequency);
        this.active = active;
    }

    // Getters
    public String getTaxTypeId() {
        return taxTypeId;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public String getTaxName() {
        return taxName;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public String getFilingFrequency() {
        return filingFrequency;
    }

    // Setters with validation
    public void setTaxTypeId(String taxTypeId) throws ValidationException {
        if (taxTypeId == null || !taxTypeId.trim().matches("[A-Za-z0-9-]{3,20}")) {
            throw new ValidationException(
                    "Tax type ID must be 3-20 letters, digits or hyphens.");
        }
        this.taxTypeId = taxTypeId.trim();
    }

    public void setTaxCode(String taxCode) throws ValidationException {
        if (taxCode == null || !taxCode.trim().matches("[A-Z0-9-]{2,10}")) {
            throw new ValidationException(
                    "Tax code must be 2-10 uppercase letters, digits or hyphens.");
        }
        this.taxCode = taxCode.trim();
    }

    public void setTaxName(String taxName) throws ValidationException {
        if (taxName == null || taxName.trim().length() < 3
                || taxName.trim().length() > 60) {
            throw new ValidationException("Tax name must be 3-60 characters.");
        }
        this.taxName = taxName.trim();
    }

    public void setRate(BigDecimal rate) throws ValidationException {
        if (rate == null) {
            throw new ValidationException("Rate is required.");
        }
        if (rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException("Rate must be between 0 and 100 inclusive.");
        }
        if (rate.scale() > 2) {
            throw new ValidationException("Rate allows at most 2 decimal places.");
        }
        this.rate = rate;
    }

    public void setFilingFrequency(String filingFrequency) throws ValidationException {
        if (filingFrequency == null) {
            throw new ValidationException("Filing frequency is required.");
        }
        String f = filingFrequency.trim().toUpperCase();
        if (!f.equals("MONTHLY") && !f.equals("QUARTERLY") && !f.equals("ANNUAL")) {
            throw new ValidationException(
                    "Filing frequency must be MONTHLY, QUARTERLY or ANNUAL.");
        }
        this.filingFrequency = f;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Business method (documented domain extension):
    // expected tax = taxableAmount x rate / 100, rounded HALF_UP to 2 decimals
    public BigDecimal calculateTax(BigDecimal taxableAmount) {
        return taxableAmount.multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    // Identifiable
    @Override
    public String getId() {
        return taxTypeId;
    }

    @Override
    public String getDisplayName() {
        return taxName + " (" + taxCode + ")";
    }

    @Override
    public boolean isActive() {
        return active;
    }

    // Validatable
    @Override
    public void validate() throws ValidationException {
        setTaxTypeId(taxTypeId);
        setTaxCode(taxCode);
        setTaxName(taxName);
        setRate(rate);
        setFilingFrequency(filingFrequency);
    }

    @Override
    public String getSummary() {
        return "TaxType | " + taxCode + " | " + taxName + " | Rate " + rate
                + "% | " + filingFrequency + " | " + (active ? "ACTIVE" : "INACTIVE");
    }

    @Override
    public void printDetails() {
        System.out.println("Tax type ID      : " + taxTypeId);
        System.out.println("Tax code         : " + taxCode);
        System.out.println("Tax name         : " + taxName);
        System.out.println("Rate (%)         : " + rate);
        System.out.println("Filing frequency : " + filingFrequency);
        System.out.println("Active           : " + (active ? "YES" : "NO"));
    }
}