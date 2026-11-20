package com.auca.corporatetaxpayer;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.ValidationException;

public class CorporateTaxpayer extends RegisteredTaxpayer {

    // Six required fields
    private String companyRegistrationNo;
    private String legalName;
    private LocalDate incorporationDate;
    private String businessSector;
    private String authorizedRepresentative;
    private BigDecimal annualTurnover;

    // Constructor
    public CorporateTaxpayer(String partyId, String firstName, String lastName,
                             String email, String phoneNumber, LocalDate dateOfBirth,
                             String tin, LocalDate registrationDate,
                             String taxpayerStatus, String district,
                             String sector, String taxOffice,
                             String companyRegistrationNo, String legalName,
                             LocalDate incorporationDate, String businessSector,
                             String authorizedRepresentative, BigDecimal annualTurnover)
                             throws ValidationException {

        super(partyId, firstName, lastName, email, phoneNumber, dateOfBirth,
              tin, registrationDate, taxpayerStatus, district, sector, taxOffice);

        setCompanyRegistrationNo(companyRegistrationNo);
        setLegalName(legalName);
        setIncorporationDate(incorporationDate);
        setBusinessSector(businessSector);
        setAuthorizedRepresentative(authorizedRepresentative);
        setAnnualTurnover(annualTurnover);
    }

    // Getters
    public String getCompanyRegistrationNo() { return companyRegistrationNo; }
    public String getLegalName() { return legalName; }
    public LocalDate getIncorporationDate() { return incorporationDate; }
    public String getBusinessSector() { return businessSector; }
    public String getAuthorizedRepresentative() { return authorizedRepresentative; }
    public BigDecimal getAnnualTurnover() { return annualTurnover; }

    // Setters with validation
    public void setCompanyRegistrationNo(String companyRegistrationNo) throws ValidationException {
        if (companyRegistrationNo == null
                || !companyRegistrationNo.trim().matches("[A-Z0-9-]{5,30}")) {
            throw new ValidationException(
                "Company registration number must be 5-30 uppercase letters, digits or hyphens.");
        }
        this.companyRegistrationNo = companyRegistrationNo.trim();
    }

    public void setLegalName(String legalName) throws ValidationException {
        if (legalName == null || legalName.trim().length() < 3
                || legalName.trim().length() > 120) {
            throw new ValidationException("Legal name must be 3-120 characters.");
        }
        this.legalName = legalName.trim();
    }

    public void setIncorporationDate(LocalDate incorporationDate) throws ValidationException {
        if (incorporationDate == null) {
            throw new ValidationException("Incorporation date is required.");
        }
        if (incorporationDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Incorporation date cannot be in the future.");
        }
        this.incorporationDate = incorporationDate;
    }

    public void setBusinessSector(String businessSector) throws ValidationException {
        if (businessSector == null || businessSector.trim().length() < 2) {
            throw new ValidationException("Business sector is required (minimum 2 characters).");
        }
        this.businessSector = businessSector.trim();
    }

    public void setAuthorizedRepresentative(String rep) throws ValidationException {
        if (rep == null || rep.trim().length() < 2 || rep.trim().length() > 60) {
            throw new ValidationException("Authorized representative must be 2-60 characters.");
        }
        this.authorizedRepresentative = rep.trim();
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
        setCompanyRegistrationNo(companyRegistrationNo);
        setLegalName(legalName);
        setIncorporationDate(incorporationDate);
        setBusinessSector(businessSector);
        setAuthorizedRepresentative(authorizedRepresentative);
        setAnnualTurnover(annualTurnover);
        if (!incorporationDate.isBefore(getRegistrationDate())) {
            throw new ValidationException("Incorporation date must be before registration date.");
        }
    }

    // Overriding methods
    @Override
    public String getDisplayName() {
        return legalName + " (Corporate)";
    }

    @Override
    public String getSummary() {
        return "Corporate | TIN " + getTin() + " | " + legalName
                + " | Turnover " + annualTurnover;
    }

    @Override
    public void printDetails() {
        super.printDetails();
        System.out.println("Company reg. no  : " + companyRegistrationNo);
        System.out.println("Legal name       : " + legalName);
        System.out.println("Incorporated on  : " + incorporationDate);
        System.out.println("Business sector  : " + businessSector);
        System.out.println("Representative   : " + authorizedRepresentative);
        System.out.println("Annual turnover  : " + annualTurnover);
    }
}