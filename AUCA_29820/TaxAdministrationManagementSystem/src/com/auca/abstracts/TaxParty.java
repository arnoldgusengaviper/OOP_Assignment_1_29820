package com.auca.abstracts;

import java.time.LocalDate;

import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;

import com.auca.exceptions.ValidationException;

public abstract class TaxParty implements Identifiable, Validatable {

    // Six required fields
    private String partyId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;

    // Constructor
    public TaxParty(String partyId, String firstName, String lastName,
                    String email, String phoneNumber, LocalDate dateOfBirth) {

        this.partyId = partyId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
    }

    // Getter methods
    public String getPartyId() {
        return partyId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    // Implement Identifiable
    @Override
    public String getId() {
        return partyId;
    }

    @Override
    public String getDisplayName() {
        return firstName + " " + lastName;
    }

    // Implement Validatable
    @Override
    public String getSummary() {
        return getDisplayName() + " | ID: " + partyId;
    }

    @Override
    public void printDetails() {
        System.out.println("Party ID: " + partyId);
        System.out.println("First Name: " + firstName);
        System.out.println("Last Name: " + lastName);
        System.out.println("Email: " + email);
        System.out.println("Phone Number: " + phoneNumber);
        System.out.println("Date of Birth: " + dateOfBirth);
    }

    // Methods that subclasses must implement
    @Override
    public abstract void validate() throws ValidationException;

    @Override
    public abstract boolean isActive();

}