package com.auca.individualtaxpayer;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.ValidationException;

public class IndividualTaxpayer extends RegisteredTaxpayer {

    // Six required fields
    private String nationalId;
    private String occupation;
    private String businessName;
    private LocalDate businessStartDate;
    private BigDecimal annualTurnover;
    private boolean resident;

    // Constructor
    public IndividualTaxpayer(String partyId, String firstName, String lastName,
            String email, String phoneNumber, LocalDate dateOfBirth,
            String tin, LocalDate registrationDate,
            String taxpayerStatus, String district,
            String sector, String taxOffice,
            String nationalId, String occupation,
            String businessName, LocalDate businessStartDate,
            BigDecimal annualTurnover, boolean resident)
            throws ValidationException {

        super(partyId, firstName, lastName, email, phoneNumber, dateOfBirth,
                tin, registrationDate, taxpayerStatus, district, sector, taxOffice);

        setNationalId(nationalId);
        setOccupation(occupation);
        setBusinessName(businessName);
        setBusinessStartDate(businessStartDate);
        setAnnualTurnover(annualTurnover);
        this.resident = resident;
    }

    // Getters
    public String getNationalId() {
        return nationalId;
    }

    public String getOccupation() {
        return occupation;
    }

    public String getBusinessName() {
        return businessName;
    }

    public LocalDate getBusinessStartDate() {
        return businessStartDate;
    }

    public BigDecimal getAnnualTurnover() {
        return annualTurnover;
    }

    public boolean isResident() {
        return resident;
    }

    // Setters with validation
    public void setNationalId(String nationalId) throws ValidationException {
        if (nationalId == null || !nationalId.trim().matches("\\d{16}")) {
            throw new ValidationException("National ID must be exactly 16 digits.");
        }
        this.nationalId = nationalId.trim();
    }

    public void setOccupation(String occupation) throws ValidationException {
        if (occupation == null || occupation.trim().length() < 2) {
            throw new ValidationException("Occupation is required (minimum 2 characters).");
        }
        this.occupation = occupation.trim();
    }

    public void setBusinessName(String businessName) throws ValidationException {
        if (businessName == null || businessName.trim().length() < 2) {
            throw new ValidationException("Business name is required (minimum 2 characters).");
        }
        this.businessName = businessName.trim();
    }

    public void setBusinessStartDate(LocalDate businessStartDate) throws ValidationException {
        if (businessStartDate == null) {
            throw new ValidationException("Business start date is required.");
        }
        if (businessStartDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Business start date cannot be in the future.");
        }
        this.businessStartDate = businessStartDate;
    }

    public void setAnnualTurnover(BigDecimal annualTurnover) throws ValidationException {
        if (annualTurnover == null || annualTurnover.signum() < 0) {
            throw new ValidationException("Annual turnover cannot be negative.");
        }
        if (annualTurnover.scale() > 2) {
            throw new ValidationException("Annual turnover allows at most 2 decimal places.");
        }
        if (annualTurnover.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new ValidationException("Annual turnover is too large.");
        }
        this.annualTurnover = annualTurnover;
    }

    public void setResident(boolean resident) {
        this.resident = resident;
    }

    // Implementing the abstract methods
    @Override
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(getTaxpayerStatus());
    }

    @Override
    public void validate() throws ValidationException {
        if (getTin() == null || !getTin().matches("\\d{9}")) {
            throw new ValidationException("TIN must be exactly 9 digits.");
        }
        setNationalId(nationalId);
        setOccupation(occupation);
        setBusinessName(businessName);
        setBusinessStartDate(businessStartDate);
        setAnnualTurnover(annualTurnover);
        if (!businessStartDate.isBefore(getRegistrationDate())) {
            throw new ValidationException("Business start date must be before registration date.");
        }
    }

    // Overriding methods
    @Override
    public String getDisplayName() {
        return getFirstName() + " " + getLastName() + " (Individual)";
    }

    @Override
    public String getSummary() {
        return "Individual | TIN " + getTin() + " | " + getFirstName() + " "
                + getLastName() + " | Turnover " + annualTurnover;
    }

    @Override
    public void printDetails() {
        super.printDetails();
        System.out.println("National ID     : " + nationalId);
        System.out.println("Occupation      : " + occupation);
        System.out.println("Business name   : " + businessName);
        System.out.println("Business start  : " + businessStartDate);
        System.out.println("Annual turnover : " + annualTurnover);
        System.out.println("Resident        : " + (resident ? "YES" : "NO"));
    }
}
