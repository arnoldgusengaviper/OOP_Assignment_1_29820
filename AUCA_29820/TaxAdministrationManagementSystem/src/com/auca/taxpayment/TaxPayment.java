package com.auca.taxpayment;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.auca.exceptions.OverpaymentException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.taxassessment.TaxAssessment;

public class TaxPayment implements Identifiable, Validatable {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");

    // Six required fields
    private String paymentId;
    private TaxAssessment assessment;
    private String paymentReference;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMethod;

    // Constructor. outstandingBalance is calculated by Main and is only used
    // for the check; it is NOT stored in the object.
    public TaxPayment(String paymentId, TaxAssessment assessment,
            String paymentReference, BigDecimal amount,
            LocalDate paymentDate, String paymentMethod,
            BigDecimal outstandingBalance) throws ValidationException {
        setPaymentId(paymentId);
        setAssessment(assessment);
        setPaymentReference(paymentReference);
        setAmount(amount);
        setPaymentDate(paymentDate);
        setPaymentMethod(paymentMethod);
        checkAgainstBalance(outstandingBalance);
    }

    // Getters
    public String getPaymentId() {
        return paymentId;
    }

    public TaxAssessment getAssessment() {
        return assessment;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    // Setters with validation
    public void setPaymentId(String paymentId) throws ValidationException {
        if (paymentId == null || !paymentId.trim().matches("[A-Za-z0-9-]{3,30}")) {
            throw new ValidationException(
                    "Payment ID must be 3-30 letters, digits or hyphens.");
        }
        this.paymentId = paymentId.trim();
    }

    public void setAssessment(TaxAssessment assessment) throws ValidationException {
        if (assessment == null) {
            throw new ValidationException("A tax assessment is required.");
        }
        if (!assessment.isConfirmed()) {
            throw new ValidationException(
                    "Payments require a CONFIRMED assessment (current: "
                            + assessment.getStatus() + ").");
        }
        this.assessment = assessment;
    }

    public void setPaymentReference(String paymentReference) throws ValidationException {
        if (paymentReference == null
                || !paymentReference.trim().matches("[A-Z0-9-]{6,30}")) {
            throw new ValidationException(
                    "Payment reference must be 6-30 uppercase letters, digits or hyphens.");
        }
        this.paymentReference = paymentReference.trim();
    }

    public void setAmount(BigDecimal amount) throws ValidationException {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Payment amount must be greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new ValidationException("Payment amount allows at most 2 decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException("Payment amount is too large.");
        }
        this.amount = amount;
    }

    public void setPaymentDate(LocalDate paymentDate) throws ValidationException {
        if (paymentDate == null) {
            throw new ValidationException("Payment date is required.");
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Payment date cannot be in the future.");
        }
        if (paymentDate.isBefore(assessment.getAssessmentDate())) {
            throw new ValidationException("Payment date cannot be before the assessment date.");
        }
        this.paymentDate = paymentDate;
    }

    public void setPaymentMethod(String paymentMethod) throws ValidationException {
        if (paymentMethod == null) {
            throw new ValidationException("Payment method is required.");
        }
        String m = paymentMethod.trim().toUpperCase();
        if (!m.equals("CASH") && !m.equals("MOBILE_MONEY")
                && !m.equals("BANK_TRANSFER") && !m.equals("CARD")) {
            throw new ValidationException(
                    "Payment method must be CASH, MOBILE_MONEY, BANK_TRANSFER or CARD.");
        }
        this.paymentMethod = m;
    }

    // Business method (documented domain extension).
    // Main calculates: outstanding = assessment total + penalties - earlier
    // payments.
    public void checkAgainstBalance(BigDecimal outstandingBalance)
            throws OverpaymentException {
        if (outstandingBalance == null || outstandingBalance.signum() <= 0) {
            throw new OverpaymentException("There is no outstanding balance to pay.");
        }
        if (amount.compareTo(outstandingBalance) > 0) {
            throw new OverpaymentException("Payment " + amount
                    + " exceeds the outstanding balance " + outstandingBalance + ".");
        }
    }

    // Identifiable
    @Override
    public String getId() {
        return paymentId;
    }

    @Override
    public String getDisplayName() {
        return "Payment " + paymentReference + " - "
                + assessment.getDeclaration().getObligation().getTaxpayer().getDisplayName();
    }

    @Override
    public boolean isActive() {
        return assessment.isConfirmed();
    }

    // Validatable
    @Override
    public void validate() throws ValidationException {
        setPaymentId(paymentId);
        setAssessment(assessment);
        setPaymentReference(paymentReference);
        setAmount(amount);
        setPaymentDate(paymentDate);
        setPaymentMethod(paymentMethod);
    }

    @Override
    public String getSummary() {
        return "Payment | " + paymentId + " | ref " + paymentReference + " | "
                + assessment.getId() + " | " + amount + " | " + paymentDate
                + " | " + paymentMethod;
    }

    @Override
    public void printDetails() {
        System.out.println("Payment ID      : " + paymentId);
        System.out.println("Assessment      : " + assessment.getId() + " (total "
                + assessment.getTotal() + ")");
        System.out.println("Taxpayer        : " + assessment.getDeclaration()
                .getObligation().getTaxpayer().getDisplayName());
        System.out.println("Reference       : " + paymentReference);
        System.out.println("Amount          : " + amount);
        System.out.println("Payment date    : " + paymentDate);
        System.out.println("Method          : " + paymentMethod);
    }
}