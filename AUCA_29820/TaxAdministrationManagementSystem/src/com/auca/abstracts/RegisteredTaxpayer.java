package com.auca.abstracts;

import java.time.LocalDate;
import com.auca.exceptions.ValidationException;

public abstract class RegisteredTaxpayer extends TaxParty {

    // Six required fields
    private String tin;
    private LocalDate registrationDate;
    private String taxpayerStatus;
    private String district;
    private String sector;
    private String taxOffice;

    // Constructor
    public RegisteredTaxpayer(String partyId, String firstName, String lastName,
            String email, String phoneNumber, LocalDate dateOfBirth,
            String tin, LocalDate registrationDate,
            String taxpayerStatus, String district,
            String sector, String taxOffice) {

        super(partyId, firstName, lastName, email, phoneNumber, dateOfBirth);

        this.tin = tin;
        this.registrationDate = registrationDate;
        this.taxpayerStatus = taxpayerStatus;
        this.district = district;
        this.sector = sector;
        this.taxOffice = taxOffice;
    }

    // Getters
    public String getTin() {
        return tin;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public String getTaxpayerStatus() {
        return taxpayerStatus;
    }

    public String getDistrict() {
        return district;
    }

    public String getSector() {
        return sector;
    }

    public String getTaxOffice() {
        return taxOffice;
    }
        // Setter with validation (used by "Update Eligible Record")
    public void setTaxpayerStatus(String taxpayerStatus) throws ValidationException {
        if (taxpayerStatus == null) {
            throw new ValidationException("Taxpayer status is required.");
        }
        String s = taxpayerStatus.trim().toUpperCase();
        if (!s.equals("ACTIVE") && !s.equals("INACTIVE") && !s.equals("SUSPENDED")) {
            throw new ValidationException(
                "Taxpayer status must be ACTIVE, INACTIVE or SUSPENDED.");
        }
        this.taxpayerStatus = s;
    }

}