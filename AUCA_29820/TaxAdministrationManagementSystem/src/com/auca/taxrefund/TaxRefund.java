package com.auca.taxrefund;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.exceptions.InvalidStatusTransitionException;
import com.auca.exceptions.OverpaymentException;
import com.auca.exceptions.UnauthorizedApprovalException;
import com.auca.exceptions.ValidationException;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;
import com.auca.revenueofficer.RevenueOfficer;
import com.auca.taxassessment.TaxAssessment;

public class TaxRefund implements Identifiable, Validatable {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");

    // Six required fields
    private String refundId;
    private RegisteredTaxpayer taxpayer;
    private TaxAssessment assessment;
    private BigDecimal requestedAmount;
    private LocalDate requestDate;
    private String status;

    // verifiedOverpayment is calculated by Main and is not stored
    public TaxRefund(String refundId, RegisteredTaxpayer taxpayer,
            TaxAssessment assessment, BigDecimal requestedAmount,
            LocalDate requestDate, BigDecimal verifiedOverpayment)
            throws ValidationException {
        setRefundId(refundId);
        setTaxpayer(taxpayer);
        setAssessment(assessment);
        checkSameTaxpayer();
        setRequestedAmount(requestedAmount);
        setRequestDate(requestDate);
        checkAgainstOverpayment(verifiedOverpayment);
        this.status = "PENDING";
    }

    // Getters
    public String getRefundId() {
        return refundId;
    }

    public RegisteredTaxpayer getTaxpayer() {
        return taxpayer;
    }

    public TaxAssessment getAssessment() {
        return assessment;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public String getStatus() {
        return status;
    }

    // Setters with validation
    public void setRefundId(String refundId) throws ValidationException {
        if (refundId == null || !refundId.trim().matches("[A-Za-z0-9-]{3,30}")) {
            throw new ValidationException("Refund ID must be 3-30 letters, digits or hyphens.");
        }
        this.refundId = refundId.trim();
    }

    public void setTaxpayer(RegisteredTaxpayer taxpayer) throws ValidationException {
        if (taxpayer == null) {
            throw new ValidationException("A registered taxpayer is required.");
        }
        this.taxpayer = taxpayer;
    }

    public void setAssessment(TaxAssessment assessment) throws ValidationException {
        if (assessment == null) {
            throw new ValidationException("A supporting assessment is required.");
        }
        if (!assessment.isConfirmed()) {
            throw new ValidationException("The assessment must be CONFIRMED.");
        }
        this.assessment = assessment;
    }

    // The refund must belong to the taxpayer of the assessment
    private void checkSameTaxpayer() throws ValidationException {
        String owner = assessment.getDeclaration().getObligation().getTaxpayer().getTin();
        if (!owner.equals(taxpayer.getTin())) {
            throw new ValidationException(
                    "The taxpayer does not match the taxpayer of the assessment.");
        }
    }

    public void setRequestedAmount(BigDecimal requestedAmount) throws ValidationException {
        if (requestedAmount == null || requestedAmount.signum() <= 0) {
            throw new ValidationException("Requested amount must be greater than zero.");
        }
        if (requestedAmount.scale() > 2) {
            throw new ValidationException("Requested amount allows at most 2 decimal places.");
        }
        if (requestedAmount.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException("Requested amount is too large.");
        }
        this.requestedAmount = requestedAmount;
    }

    public void setRequestDate(LocalDate requestDate) throws ValidationException {
        if (requestDate == null) {
            throw new ValidationException("Request date is required.");
        }
        if (requestDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Request date cannot be in the future.");
        }
        if (requestDate.isBefore(assessment.getAssessmentDate())) {
            throw new ValidationException("Request date cannot be before the assessment date.");
        }
        this.requestDate = requestDate;
    }

    // Business methods (documented domain extensions)

    public void checkAgainstOverpayment(BigDecimal verifiedOverpayment)
            throws OverpaymentException {
        if (verifiedOverpayment == null || verifiedOverpayment.signum() <= 0) {
            throw new OverpaymentException("No verified overpayment exists for this assessment.");
        }
        if (requestedAmount.compareTo(verifiedOverpayment) > 0) {
            throw new OverpaymentException("Requested " + requestedAmount
                    + " exceeds the verified overpayment " + verifiedOverpayment + ".");
        }
    }

    // PENDING -> APPROVED, only by an active officer whose limit covers the amount
    public void approve(RevenueOfficer officer, BigDecimal verifiedOverpayment)
            throws InvalidStatusTransitionException, UnauthorizedApprovalException,
            OverpaymentException {
        if (!status.equals("PENDING")) {
            throw new InvalidStatusTransitionException(
                    "Only a PENDING refund can be approved (current: " + status + ").");
        }
        if (officer == null || !officer.isActive()) {
            throw new UnauthorizedApprovalException("The approving officer must be ACTIVE.");
        }
        if (!officer.canApprove(requestedAmount)) {
            throw new UnauthorizedApprovalException("Refund " + requestedAmount
                    + " exceeds the officer's approval limit "
                    + officer.getApprovalLimit() + ".");
        }
        checkAgainstOverpayment(verifiedOverpayment); // re-check at approval time
        this.status = "APPROVED";
    }

    // PENDING -> REJECTED
    public void reject() throws InvalidStatusTransitionException {
        if (!status.equals("PENDING")) {
            throw new InvalidStatusTransitionException(
                    "Only a PENDING refund can be rejected (current: " + status + ").");
        }
        this.status = "REJECTED";
    }

    // Identifiable
    @Override
    public String getId() {
        return refundId;
    }

    @Override
    public String getDisplayName() {
        return "Refund " + refundId + " - " + taxpayer.getDisplayName();
    }

    @Override
    public boolean isActive() {
        return !status.equals("REJECTED");
    }

    // Validatable
    @Override
    public void validate() throws ValidationException {
        setRefundId(refundId);
        setTaxpayer(taxpayer);
        setAssessment(assessment);
        checkSameTaxpayer();
        setRequestedAmount(requestedAmount);
        setRequestDate(requestDate);
    }

    @Override
    public String getSummary() {
        return "Refund | " + refundId + " | TIN " + taxpayer.getTin() + " | "
                + assessment.getId() + " | " + requestedAmount + " | "
                + requestDate + " | " + status;
    }

    @Override
    public void printDetails() {
        System.out.println("Refund ID        : " + refundId);
        System.out.println("Taxpayer         : " + taxpayer.getDisplayName()
                + " (TIN " + taxpayer.getTin() + ")");
        System.out.println("Assessment       : " + assessment.getId());
        System.out.println("Requested amount : " + requestedAmount);
        System.out.println("Request date     : " + requestDate);
        System.out.println("Status           : " + status);
    }
}