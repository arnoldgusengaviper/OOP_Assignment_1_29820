package com.auca.main;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

import com.auca.abstracts.RegisteredTaxpayer;
import com.auca.corporatetaxpayer.CorporateTaxpayer;
import com.auca.exceptions.DuplicateRecordException;
import com.auca.exceptions.ValidationException;
import com.auca.individualtaxpayer.IndividualTaxpayer;
import com.auca.penalty.Penalty;
import com.auca.revenueofficer.RevenueOfficer;
import com.auca.taxassessment.TaxAssessment;
import com.auca.taxclearance.TaxClearance;
import com.auca.taxdeclaration.TaxDeclaration;
import com.auca.taxobligation.TaxObligation;
import com.auca.taxpayment.TaxPayment;
import com.auca.taxrefund.TaxRefund;
import com.auca.taxtype.TaxType;
import com.auca.exceptions.InvalidStatusTransitionException;
import com.auca.exceptions.InvalidTaxCalculationException;
import com.auca.exceptions.InvalidTaxPeriodException;
import com.auca.exceptions.PrerequisiteMissingException;
import com.auca.exceptions.RecordNotFoundException;
import com.auca.exceptions.OverpaymentException;
import com.auca.exceptions.UnauthorizedApprovalException;
import com.auca.abstracts.TaxParty;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Validatable;

public class Main {

        // ================= ONE shared Scanner =================
        private static final Scanner sc = new Scanner(System.in);

        // ================= Storage: fixed-size typed arrays + counters
        // =================
        private static final IndividualTaxpayer[] individuals = new IndividualTaxpayer[100];
        private static int individualCount = 0;

        private static final CorporateTaxpayer[] corporates = new CorporateTaxpayer[100];
        private static int corporateCount = 0;

        private static final RevenueOfficer[] officers = new RevenueOfficer[50];
        private static int officerCount = 0;

        private static final TaxType[] taxTypes = new TaxType[50];
        private static int taxTypeCount = 0;

        // Declared now, used in the next layers
        private static final TaxObligation[] obligations = new TaxObligation[200];
        private static int obligationCount = 0;

        private static final TaxDeclaration[] declarations = new TaxDeclaration[200];
        private static int declarationCount = 0;

        private static final TaxAssessment[] assessments = new TaxAssessment[200];
        private static int assessmentCount = 0;

        private static final TaxPayment[] payments = new TaxPayment[300];
        private static int paymentCount = 0;

        private static final Penalty[] penalties = new Penalty[200];
        private static int penaltyCount = 0;

        private static final TaxRefund[] refunds = new TaxRefund[100];
        private static int refundCount = 0;

        private static final TaxClearance[] clearances = new TaxClearance[100];
        private static int clearanceCount = 0;

        // Private helper used to leave a form when the user types CANCEL.
        // It is NOT a domain type; it is unchecked so it can fly out of any helper.
        private static class CancelledException extends RuntimeException {
                CancelledException(String message) {
                        super(message);
                }
        }

        // ================= MAIN: menu loop until 0 =================
        public static void main(String[] args) {
                System.out.println("TAMS - fictional tax rules, for teaching only.");
                boolean running = true;
                while (running) {
                        printMenu();
                        System.out.print("Select option: ");
                        if (!sc.hasNextLine()) {
                                break; // input closed: stop safely
                        }
                        String line = sc.nextLine().trim();
                        int choice;
                        try {
                                choice = Integer.parseInt(line);
                        } catch (NumberFormatException e) {
                                System.out.println("Invalid option. Enter a number from 0 to 21.");
                                continue;
                        }

                        // Workflow boundary: every error is caught here, the program never stops
                        try {
                                switch (choice) {
                                        case 0:
                                                running = false;
                                                System.out.println("Goodbye.");
                                                break;
                                        case 1:
                                                registerIndividual();
                                                break;
                                        case 2:
                                                registerCorporate();
                                                break;
                                        case 3:
                                                registerOfficer();
                                                break;
                                        case 4:
                                                registerTaxType();
                                                break;
                                        case 5:
                                                createObligation();
                                                break;
                                        case 6:
                                                fileDeclaration();
                                                break;
                                        case 7:
                                                assessDeclaration();
                                                break;
                                        case 8:
                                                confirmAssessment();
                                                break;
                                        case 9:
                                                viewAllRecords();
                                                break;
                                        case 10:
                                                viewDeclarationHistory();
                                                break;
                                        case 11:
                                                viewAssessmentHistory();
                                                break;

                                        case 12:
                                                recordPayment();
                                                break;
                                        case 13:
                                                managePenalty();
                                                break;
                                        case 14:
                                                manageRefund();
                                                break;
                                        case 15:
                                                manageClearance();
                                                break;
                                        case 16:
                                                searchRecord();
                                                break;
                                        case 17:
                                                updateRecord();
                                                break;
                                        case 18:
                                                viewTaxpayerStatement();
                                                break;
                                        case 19:
                                                viewOutstandingBalances();
                                                break;
                                        case 20:
                                                viewTaxReports();
                                                break;
                                        case 21:
                                                demonstratePolymorphism();
                                                break;
                                        default:
                                                if (choice < 0 || choice > 21) {
                                                        System.out.println(
                                                                        "Unknown option. Enter a number from 0 to 21.");
                                                } else {
                                                        System.out.println("Option " + choice
                                                                        + " is coming in the next layer.");
                                                }
                                }
                        } catch (CancelledException e) {
                                System.out.println("Cancelled. Nothing was saved.");
                        } catch (DuplicateRecordException | ValidationException | RecordNotFoundException
                                        | PrerequisiteMissingException | InvalidStatusTransitionException
                                        | UnauthorizedApprovalException e) {
                                System.out.println("Error: " + e.getMessage() + " Nothing was saved.");
                        } catch (RuntimeException e) {
                                System.out.println("Unexpected problem: " + e.getMessage()
                                                + " Nothing was saved.");
                        }
                }
                sc.close();
        }
        // ================= LAYER 2a: EXTRA INPUT HELPERS =================

        // A date that MAY be in the future (periods and due dates)
        private static LocalDate readDate(String label) {
                while (true) {
                        String v = readLine(label);
                        try {
                                return LocalDate.parse(v);
                        } catch (DateTimeParseException e) {
                                System.out.println("    Invalid: use a real date as YYYY-MM-DD.");
                        }
                }
        }

        // Money that may be negative (assessment adjustment)
        private static BigDecimal readSignedMoney(String label) {
                while (true) {
                        String v = readLine(label);
                        if (v.matches("-?\\d{1,12}(\\.\\d{1,2})?")) {
                                return new BigDecimal(v);
                        }
                        System.out.println("    Invalid: use an amount like 0, 5000.50 or -2000.00 (max 2 decimals).");
                }
        }

        // ================= LAYER 2a: FINDERS =================

        private static TaxType findTaxTypeByCode(String code) {
                for (int i = 0; i < taxTypeCount; i++) {
                        if (taxTypes[i].getTaxCode().equalsIgnoreCase(code))
                                return taxTypes[i];
                }
                return null;
        }

        private static TaxObligation findObligationById(String id) {
                for (int i = 0; i < obligationCount; i++) {
                        if (obligations[i].getId().equalsIgnoreCase(id))
                                return obligations[i];
                }
                return null;
        }

        private static TaxDeclaration findDeclarationById(String id) {
                for (int i = 0; i < declarationCount; i++) {
                        if (declarations[i].getId().equalsIgnoreCase(id))
                                return declarations[i];
                }
                return null;
        }

        private static TaxAssessment findAssessmentById(String id) {
                for (int i = 0; i < assessmentCount; i++) {
                        if (assessments[i].getId().equalsIgnoreCase(id))
                                return assessments[i];
                }
                return null;
        }

        // First saved obligation that overlaps the given one (same taxpayer + tax type)
        private static TaxObligation findOverlap(TaxObligation candidate) {
                for (int i = 0; i < obligationCount; i++) {
                        if (obligations[i].overlaps(candidate))
                                return obligations[i];
                }
                return null;
        }

        // "==" compares object references: is it the very same obligation object?
        private static TaxDeclaration findActiveDeclarationFor(TaxObligation ob) {
                for (int i = 0; i < declarationCount; i++) {
                        if (declarations[i].getObligation() == ob && declarations[i].isActive()) {
                                return declarations[i];
                        }
                }
                return null;
        }

        private static TaxAssessment findLiveAssessmentFor(TaxDeclaration d) {
                for (int i = 0; i < assessmentCount; i++) {
                        if (assessments[i].getDeclaration() == d && assessments[i].isActive()) {
                                return assessments[i];
                        }
                }
                return null;
        }

        // ================= LAYER 2a: PREREQUISITE SELECTION =================

        // Offers "Register now / Cancel". Returns normally after the nested form ends;
        // the caller then asks for the identifier again. Throws if the user cancels.
        private static void askRegisterPrerequisite(String what)
                        throws PrerequisiteMissingException {
                System.out.println("    PREREQUISITE MISSING: a " + what + " is required.");
                while (true) {
                        String v = readLine("[1] Register prerequisite now   [0] Cancel");
                        if (v.equals("0")) {
                                throw new PrerequisiteMissingException(
                                                "A " + what + " is required to continue.");
                        }
                        if (v.equals("1")) {
                                break;
                        }
                        System.out.println("    Invalid: enter 1 or 0.");
                }
                try {
                        if (what.equals("taxpayer")) {
                                String kind = readChoice("Type of taxpayer [INDIVIDUAL / CORPORATE]",
                                                new String[] { "INDIVIDUAL", "CORPORATE" });
                                if (kind.equals("INDIVIDUAL")) {
                                        registerIndividual();
                                } else {
                                        registerCorporate();
                                }
                        } else if (what.equals("tax type")) {
                                registerTaxType();
                        } else if (what.equals("obligation")) {
                                createObligation();
                        } else if (what.equals("declaration")) {
                                fileDeclaration();
                        } else if (what.equals("assessment")) { // NEW
                                assessDeclaration(); // NEW
                        } else if (what.equals("officer")) { // NEW
                                registerOfficer(); // NEW
                        }
                } catch (CancelledException e) {
                        System.out.println("    Prerequisite form cancelled.");
                } catch (DuplicateRecordException | ValidationException
                                | PrerequisiteMissingException e) {
                        System.out.println("    Prerequisite not saved: " + e.getMessage());
                }
                System.out.println("    Back to your original form (your earlier entries are kept).");
        }

        // ================= LAYER 2b: BALANCES (the money rules live here)
        // =================

        // Penalties that belong to an obligation ("==" = same object)
        private static BigDecimal penaltiesFor(TaxObligation ob) {
                BigDecimal sum = BigDecimal.ZERO;
                for (int i = 0; i < penaltyCount; i++) {
                        if (penalties[i].getObligation() == ob) {
                                sum = sum.add(penalties[i].getPenaltyAmount());
                        }
                }
                return sum;
        }

        private static BigDecimal paymentsFor(TaxAssessment a) {
                BigDecimal sum = BigDecimal.ZERO;
                for (int i = 0; i < paymentCount; i++) {
                        if (payments[i].getAssessment() == a) {
                                sum = sum.add(payments[i].getAmount());
                        }
                }
                return sum;
        }

        private static BigDecimal approvedRefundsFor(TaxAssessment a) {
                BigDecimal sum = BigDecimal.ZERO;
                for (int i = 0; i < refundCount; i++) {
                        if (refunds[i].getAssessment() == a && refunds[i].getStatus().equals("APPROVED")) {
                                sum = sum.add(refunds[i].getRequestedAmount());
                        }
                }
                return sum;
        }

        // outstanding = total + penalties - payments (+ money already refunded)
        private static BigDecimal outstandingFor(TaxAssessment a) {
                return a.getTotal()
                                .add(penaltiesFor(a.getDeclaration().getObligation()))
                                .subtract(paymentsFor(a))
                                .add(approvedRefundsFor(a));
        }

        // Overpayment = how far the outstanding balance is BELOW zero
        private static BigDecimal overpaymentFor(TaxAssessment a) {
                BigDecimal out = outstandingFor(a);
                return out.signum() < 0 ? out.negate() : BigDecimal.ZERO;
        }

        // An obligation is settled when it has a CONFIRMED assessment with nothing left
        // to pay
        private static boolean isObligationSettled(TaxObligation ob) {
                boolean found = false;
                for (int i = 0; i < assessmentCount; i++) {
                        if (assessments[i].getDeclaration().getObligation() == ob
                                        && assessments[i].isConfirmed()) {
                                found = true;
                                if (outstandingFor(assessments[i]).signum() > 0) {
                                        return false;
                                }
                        }
                }
                return found;
        }

        // Overdue obligations of a taxpayer that are not settled (prints each one)
        private static int countOverdueUnpaid(RegisteredTaxpayer tp) {
                int count = 0;
                for (int i = 0; i < obligationCount; i++) {
                        TaxObligation ob = obligations[i];
                        if (ob.getTaxpayer().getTin().equals(tp.getTin())
                                        && ob.isOverdue(LocalDate.now()) && !isObligationSettled(ob)) {
                                count++;
                                System.out.println("    - overdue and not settled: " + ob.getId()
                                                + " (due " + ob.getDueDate() + ")");
                        }
                }
                return count;
        }

        // Declarations of a taxpayer that are still SUBMITTED or LATE (not
        // assessed/rejected)
        private static int countUnresolvedDeclarations(RegisteredTaxpayer tp) {
                int count = 0;
                for (int i = 0; i < declarationCount; i++) {
                        TaxDeclaration d = declarations[i];
                        if (d.getObligation().getTaxpayer().getTin().equals(tp.getTin())
                                        && (d.getStatus().equals("SUBMITTED") || d.getStatus().equals("LATE"))) {
                                count++;
                                System.out.println("    - unresolved declaration: " + d.getId()
                                                + " (" + d.getStatus() + ")");
                        }
                }
                return count;
        }

        private static void printBalanceNote(TaxObligation ob) {
                for (int i = 0; i < assessmentCount; i++) {
                        if (assessments[i].getDeclaration().getObligation() == ob
                                        && assessments[i].isConfirmed()) {
                                System.out.println("    Balance of assessment " + assessments[i].getId()
                                                + " is now " + outstandingFor(assessments[i])
                                                + " (a negative number means overpaid).");
                        }
                }
        }

        // ================= LAYER 2b: FINDERS =================

        private static TaxPayment findPaymentById(String id) {
                for (int i = 0; i < paymentCount; i++) {
                        if (payments[i].getId().equalsIgnoreCase(id))
                                return payments[i];
                }
                return null;
        }

        // payment reference must be unique GLOBALLY
        private static boolean paymentReferenceExists(String ref) {
                for (int i = 0; i < paymentCount; i++) {
                        if (payments[i].getPaymentReference().equalsIgnoreCase(ref))
                                return true;
                }
                return false;
        }

        private static Penalty findPenaltyById(String id) {
                for (int i = 0; i < penaltyCount; i++) {
                        if (penalties[i].getId().equalsIgnoreCase(id))
                                return penalties[i];
                }
                return null;
        }

        // a penalty is unique by obligation AND penalty type
        private static Penalty findPenaltyFor(TaxObligation ob, String type) {
                for (int i = 0; i < penaltyCount; i++) {
                        if (penalties[i].getObligation() == ob
                                        && penalties[i].getPenaltyType().equals(type)) {
                                return penalties[i];
                        }
                }
                return null;
        }

        private static TaxDeclaration findLateDeclarationFor(TaxObligation ob) {
                for (int i = 0; i < declarationCount; i++) {
                        if (declarations[i].getObligation() == ob && declarations[i].isLate()
                                        && !declarations[i].getStatus().equals("REJECTED")) {
                                return declarations[i];
                        }
                }
                return null;
        }

        private static TaxRefund findRefundById(String id) {
                for (int i = 0; i < refundCount; i++) {
                        if (refunds[i].getId().equalsIgnoreCase(id))
                                return refunds[i];
                }
                return null;
        }

        private static TaxRefund findPendingRefundFor(TaxAssessment a) {
                for (int i = 0; i < refundCount; i++) {
                        if (refunds[i].getAssessment() == a && refunds[i].getStatus().equals("PENDING")) {
                                return refunds[i];
                        }
                }
                return null;
        }

        private static TaxClearance findClearanceById(String id) {
                for (int i = 0; i < clearanceCount; i++) {
                        if (clearances[i].getId().equalsIgnoreCase(id))
                                return clearances[i];
                }
                return null;
        }

        private static RevenueOfficer findOfficerByStaff(String no) {
                for (int i = 0; i < officerCount; i++) {
                        if (officers[i].getStaffNumber().equalsIgnoreCase(no))
                                return officers[i];
                }
                return null;
        }

        // A rate where pressing Enter keeps the default
        private static BigDecimal readRateOrDefault(String label, BigDecimal defaultRate) {
                while (true) {
                        String v = readLine(label);
                        if (v.isEmpty()) {
                                return defaultRate;
                        }
                        if (v.matches("\\d{1,3}(\\.\\d{1,2})?")) {
                                BigDecimal r = new BigDecimal(v);
                                if (r.compareTo(new BigDecimal("100")) <= 0) {
                                        return r;
                                }
                        }
                        System.out.println("    Invalid: use a rate from 0 to 100 (max 2 decimals), or press Enter.");
                }
        }

        // ================= LAYER 2b: SELECTORS =================

        private static TaxAssessment selectConfirmedAssessment() throws PrerequisiteMissingException {
                while (true) {
                        String id = readPattern("02 Assessment ID [String, must exist and be CONFIRMED]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        TaxAssessment a = findAssessmentById(id);
                        if (a == null) {
                                System.out.println("    No assessment has ID " + id + ".");
                                askRegisterPrerequisite("assessment");
                                continue;
                        }
                        if (!a.isConfirmed()) {
                                System.out.println("    Rejected: assessment " + a.getId() + " is " + a.getStatus()
                                                + ". Only a CONFIRMED assessment can be used (confirm it with option 8).");
                                continue;
                        }
                        System.out.println("    Selected: " + a.getSummary());
                        return a;
                }
        }

        private static RevenueOfficer selectOfficer() throws PrerequisiteMissingException {
                while (true) {
                        String no = readPattern("Officer staff number [String, must exist]",
                                        "[A-Za-z0-9-]{3,20}", "3-20 letters, digits or hyphens.");
                        RevenueOfficer o = findOfficerByStaff(no);
                        if (o == null) {
                                System.out.println("    No officer has staff number " + no + ".");
                                askRegisterPrerequisite("officer");
                                continue;
                        }
                        System.out.println("    Selected: " + o.getSummary());
                        return o;
                }
        }

        // ================= LAYER 3: HELPERS =================

        // TIN, or just Enter to mean "all"
        private static String readTinOrAll() {
                while (true) {
                        String v = readLine("TIN filter [String, 9 digits, or press Enter for ALL]");
                        if (v.isEmpty() || v.matches("\\d{9}")) {
                                return v;
                        }
                        System.out.println("    Invalid: type 9 digits or press Enter.");
                }
        }

        private static RegisteredTaxpayer requireTaxpayer(String tin) throws RecordNotFoundException {
                RegisteredTaxpayer t = findTaxpayerByTin(tin);
                if (t == null) {
                        throw new RecordNotFoundException("No taxpayer has TIN " + tin + ".");
                }
                return t;
        }

        private static void showFound(String kind, Validatable record) {
                System.out.println("\n>>> " + kind);
                record.printDetails();
        }

        // ================= OPTION 10: VIEW DECLARATION HISTORY =================
        private static void viewDeclarationHistory() throws RecordNotFoundException {
                System.out.println("\n--- DECLARATION HISTORY (in the order they were saved) ---");
                String tin = readTinOrAll();
                RegisteredTaxpayer tp = null;
                if (!tin.isEmpty()) {
                        tp = requireTaxpayer(tin);
                        System.out.println("Taxpayer: " + tp.getSummary());
                }
                int shown = 0;
                for (int i = 0; i < declarationCount; i++) {
                        TaxDeclaration d = declarations[i];
                        if (tp != null && !d.getObligation().getTaxpayer().getTin().equals(tp.getTin())) {
                                continue;
                        }
                        shown++;
                        System.out.println(shown + ". " + d.getId() + " | obligation " + d.getObligation().getId()
                                        + " (" + d.getObligation().getTaxType().getTaxCode() + " "
                                        + d.getObligation().getPeriodStart() + " to " + d.getObligation().getPeriodEnd()
                                        + ")");
                        System.out.println("     TIN " + d.getObligation().getTaxpayer().getTin()
                                        + " | due " + d.getObligation().getDueDate() + " | filed " + d.getFilingDate()
                                        + (d.isLate() ? " (LATE)" : " (on time)"));
                        System.out.println("     taxable " + d.getTaxableAmount() + " | declared tax "
                                        + d.getDeclaredTax() + " | expected " + d.getExpectedTax()
                                        + " | status " + d.getStatus());
                }
                if (shown == 0) {
                        System.out.println("  (no declarations found)");
                }
        }

        // ================= OPTION 11: VIEW ASSESSMENT HISTORY =================
        private static void viewAssessmentHistory() throws RecordNotFoundException {
                System.out.println("\n--- ASSESSMENT HISTORY (in the order they were saved) ---");
                String tin = readTinOrAll();
                RegisteredTaxpayer tp = null;
                if (!tin.isEmpty()) {
                        tp = requireTaxpayer(tin);
                        System.out.println("Taxpayer: " + tp.getSummary());
                }
                int shown = 0;
                for (int i = 0; i < assessmentCount; i++) {
                        TaxAssessment a = assessments[i];
                        TaxObligation ob = a.getDeclaration().getObligation();
                        if (tp != null && !ob.getTaxpayer().getTin().equals(tp.getTin())) {
                                continue;
                        }
                        shown++;
                        System.out.println(shown + ". " + a.getId() + " | declaration " + a.getDeclaration().getId()
                                        + " | TIN " + ob.getTaxpayer().getTin() + " | date " + a.getAssessmentDate());
                        System.out.println("     assessed " + a.getAssessedTax() + " + adjustment "
                                        + a.getAdjustmentAmount() + " = TOTAL " + a.getTotal() + " | status "
                                        + a.getStatus());
                        if (a.isConfirmed()) {
                                System.out.println("     paid " + paymentsFor(a) + " | outstanding balance "
                                                + outstandingFor(a));
                        }
                }
                if (shown == 0) {
                        System.out.println("  (no assessments found)");
                }
        }
        // ================= OPTION 16: SEARCH RECORD BY ID / TIN =================
        private static void searchRecord() throws RecordNotFoundException {
                System.out.println("\n--- SEARCH RECORD ---");
                String key = readText("Enter an ID, TIN, national ID, company no., staff no., tax code "
                                + "or payment reference [String, 2-30]", 2, 30);
                int found = 0;

                for (int i = 0; i < individualCount; i++) {
                        if (individuals[i].getId().equalsIgnoreCase(key) || individuals[i].getTin().equals(key)
                                        || individuals[i].getNationalId().equals(key)) {
                                showFound("Individual taxpayer", individuals[i]);
                                found++;
                        }
                }
                for (int i = 0; i < corporateCount; i++) {
                        if (corporates[i].getId().equalsIgnoreCase(key) || corporates[i].getTin().equals(key)
                                        || corporates[i].getCompanyRegistrationNo().equalsIgnoreCase(key)) {
                                showFound("Corporate taxpayer", corporates[i]);
                                found++;
                        }
                }
                for (int i = 0; i < officerCount; i++) {
                        if (officers[i].getId().equalsIgnoreCase(key)
                                        || officers[i].getStaffNumber().equalsIgnoreCase(key)) {
                                showFound("Revenue officer", officers[i]);
                                found++;
                        }
                }
                for (int i = 0; i < taxTypeCount; i++) {
                        if (taxTypes[i].getId().equalsIgnoreCase(key)
                                        || taxTypes[i].getTaxCode().equalsIgnoreCase(key)) {
                                showFound("Tax type", taxTypes[i]);
                                found++;
                        }
                }
                for (int i = 0; i < obligationCount; i++) {
                        if (obligations[i].getId().equalsIgnoreCase(key)) {
                                showFound("Tax obligation", obligations[i]);
                                found++;
                        }
                }
                for (int i = 0; i < declarationCount; i++) {
                        if (declarations[i].getId().equalsIgnoreCase(key)) {
                                showFound("Tax declaration", declarations[i]);
                                found++;
                        }
                }
                for (int i = 0; i < assessmentCount; i++) {
                        if (assessments[i].getId().equalsIgnoreCase(key)) {
                                showFound("Tax assessment", assessments[i]);
                                found++;
                        }
                }
                for (int i = 0; i < paymentCount; i++) {
                        if (payments[i].getId().equalsIgnoreCase(key)
                                        || payments[i].getPaymentReference().equalsIgnoreCase(key)) {
                                showFound("Tax payment", payments[i]);
                                found++;
                        }
                }
                for (int i = 0; i < penaltyCount; i++) {
                        if (penalties[i].getId().equalsIgnoreCase(key)) {
                                showFound("Penalty", penalties[i]);
                                found++;
                        }
                }
                for (int i = 0; i < refundCount; i++) {
                        if (refunds[i].getId().equalsIgnoreCase(key)) {
                                showFound("Tax refund", refunds[i]);
                                found++;
                        }
                }
                for (int i = 0; i < clearanceCount; i++) {
                        if (clearances[i].getId().equalsIgnoreCase(key)) {
                                showFound("Tax clearance", clearances[i]);
                                found++;
                        }
                }
                if (found == 0) {
                        throw new RecordNotFoundException("No record matches '" + key + "'.");
                }
                System.out.println("\n" + found + " record(s) found.");
        }

        // ================= OPTION 17: UPDATE ELIGIBLE RECORD =================
        private static void updateRecord()
                        throws ValidationException, RecordNotFoundException, InvalidStatusTransitionException {
                System.out.println("\n+------- UPDATE ELIGIBLE RECORD  (type CANCEL to stop) -------+");
                System.out.println("  [1] Tax type (rate, active, filing frequency, name)");
                System.out.println("  [2] Revenue officer (approval limit, active, role)");
                System.out.println("  [3] Taxpayer status");
                System.out.println("  [4] Reject a declaration");
                System.out.println("  [5] Cancel a DRAFT assessment");
                String c = readChoice("Choose a record kind [1-5]", new String[] { "1", "2", "3", "4", "5" });
                if (c.equals("1")) {
                        updateTaxType();
                } else if (c.equals("2")) {
                        updateOfficer();
                } else if (c.equals("3")) {
                        updateTaxpayerStatus();
                } else if (c.equals("4")) {
                        rejectDeclaration();
                } else {
                        cancelDraftAssessment();
                }
        }

        private static void updateTaxType() throws ValidationException, RecordNotFoundException {
                System.out.println("STEP 1 OF 4 - SELECT THE TAX TYPE");
                String code = readText("01 Tax code [String, existing]", 2, 10).toUpperCase();
                TaxType tt = findTaxTypeByCode(code);
                if (tt == null) {
                        throw new RecordNotFoundException("No tax type has code " + code + ".");
                }
                System.out.println("STEP 2 OF 4 - NEW VALUE");
                tt.printDetails();
                String f = readChoice("02 Field [1] Rate  [2] Active  [3] Filing frequency  [4] Name",
                                new String[] { "1", "2", "3", "4" });
                BigDecimal newRate = null;
                boolean newActive = false;
                String newText = null;
                String change;
                if (f.equals("1")) {
                        newRate = readRate("03 New rate % [BigDecimal, 0-100, 2 decimals]");
                        change = "Rate " + tt.getRate() + " -> " + newRate;
                } else if (f.equals("2")) {
                        newActive = readYesNo("03 Active [boolean: YES / NO]");
                        change = "Active " + tt.isActive() + " -> " + newActive;
                } else if (f.equals("3")) {
                        newText = readChoice("03 Filing frequency [MONTHLY / QUARTERLY / ANNUAL]",
                                        new String[] { "MONTHLY", "QUARTERLY", "ANNUAL" });
                        change = "Filing frequency " + tt.getFilingFrequency() + " -> " + newText;
                } else {
                        newText = readText("03 Tax name [String, 3-60]", 3, 60);
                        change = "Name " + tt.getTaxName() + " -> " + newText;
                }
                System.out.println("STEP 3 OF 4 - CHANGE");
                System.out.println("    " + change);
                System.out.println("    (a new rate affects declarations filed from now on)");
                System.out.println("STEP 4 OF 4 - DECISION");
                if (!confirmSave()) {
                        System.out.println("Cancelled. The tax type is unchanged.");
                        return;
                }
                if (f.equals("1")) {
                        tt.setRate(newRate);
                } else if (f.equals("2")) {
                        tt.setActive(newActive);
                } else if (f.equals("3")) {
                        tt.setFilingFrequency(newText);
                } else {
                        tt.setTaxName(newText);
                }
                System.out.println("Updated: " + tt.getSummary());
        }

        private static void updateOfficer() throws ValidationException, RecordNotFoundException {
                System.out.println("STEP 1 OF 4 - SELECT THE OFFICER");
                String no = readPattern("01 Staff number [String, existing]",
                                "[A-Za-z0-9-]{3,20}", "3-20 letters, digits or hyphens.");
                RevenueOfficer o = findOfficerByStaff(no);
                if (o == null) {
                        throw new RecordNotFoundException("No officer has staff number " + no + ".");
                }
                System.out.println("STEP 2 OF 4 - NEW VALUE");
                o.printDetails();
                String f = readChoice("02 Field [1] Approval limit  [2] Active  [3] Role",
                                new String[] { "1", "2", "3" });
                BigDecimal newLimit = null;
                boolean newActive = false;
                String newRole = null;
                String change;
                if (f.equals("1")) {
                        newLimit = readMoney("03 New approval limit [BigDecimal, >= 0, 2 decimals]");
                        change = "Approval limit " + o.getApprovalLimit() + " -> " + newLimit;
                } else if (f.equals("2")) {
                        newActive = readYesNo("03 Active [boolean: YES / NO]");
                        change = "Active " + o.isActive() + " -> " + newActive;
                } else {
                        newRole = readText("03 Role [String, 2-60]", 2, 60);
                        change = "Role " + o.getRole() + " -> " + newRole;
                }
                System.out.println("STEP 3 OF 4 - CHANGE");
                System.out.println("    " + change);
                System.out.println("STEP 4 OF 4 - DECISION");
                if (!confirmSave()) {
                        System.out.println("Cancelled. The officer is unchanged.");
                        return;
                }
                if (f.equals("1")) {
                        o.setApprovalLimit(newLimit);
                } else if (f.equals("2")) {
                        o.setActive(newActive);
                } else {
                        o.setRole(newRole);
                }
                System.out.println("Updated: " + o.getSummary());
        }

        private static void updateTaxpayerStatus() throws ValidationException, RecordNotFoundException {
                System.out.println("STEP 1 OF 4 - SELECT THE TAXPAYER");
                String tin = readPattern("01 TIN [String, 9 digits, existing]", "\\d{9}",
                                "TIN must be exactly 9 digits.");
                RegisteredTaxpayer t = requireTaxpayer(tin);
                System.out.println("STEP 2 OF 4 - NEW STATUS");
                System.out.println("    Current: " + t.getSummary() + " | status " + t.getTaxpayerStatus());
                String newStatus = readChoice("02 New status [ACTIVE / INACTIVE / SUSPENDED]",
                                new String[] { "ACTIVE", "INACTIVE", "SUSPENDED" });
                System.out.println("STEP 3 OF 4 - CHANGE");
                System.out.println("    Status " + t.getTaxpayerStatus() + " -> " + newStatus);
                System.out.println("STEP 4 OF 4 - DECISION");
                if (!confirmSave()) {
                        System.out.println("Cancelled. The taxpayer is unchanged.");
                        return;
                }
                t.setTaxpayerStatus(newStatus);
                System.out.println("Updated: " + t.getSummary() + " | status " + t.getTaxpayerStatus());
        }

        private static void rejectDeclaration()
                        throws RecordNotFoundException, InvalidStatusTransitionException {
                System.out.println("STEP 1 OF 4 - SELECT THE DECLARATION");
                String id = readPattern("01 Declaration ID [String, existing]",
                                "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                TaxDeclaration d = findDeclarationById(id);
                if (d == null) {
                        throw new RecordNotFoundException("No declaration has ID " + id + ".");
                }
                System.out.println("STEP 2 OF 4 - CHECKS");
                d.printDetails();
                if (!d.getStatus().equals("SUBMITTED") && !d.getStatus().equals("LATE")) {
                        throw new InvalidStatusTransitionException(
                                        "Only a SUBMITTED or LATE declaration can be rejected (current: "
                                                        + d.getStatus() + ").");
                }
                TaxAssessment live = findLiveAssessmentFor(d);
                if (live != null) {
                        throw new InvalidStatusTransitionException("Declaration " + d.getId()
                                        + " has assessment " + live.getId() + ". Cancel that assessment first.");
                }
                System.out.println("STEP 3 OF 4 - CHANGE");
                System.out.println("    Status " + d.getStatus()
                                + " -> REJECTED (a rejected declaration cannot be assessed)");
                System.out.println("STEP 4 OF 4 - DECISION");
                if (!confirmSave()) {
                        System.out.println("Cancelled. The declaration is unchanged.");
                        return;
                }
                d.changeStatus("REJECTED");
                System.out.println("Declaration " + d.getId() + " is now " + d.getStatus() + ".");
        }

        private static void cancelDraftAssessment()
                        throws RecordNotFoundException, InvalidStatusTransitionException {
                System.out.println("STEP 1 OF 4 - SELECT THE ASSESSMENT");
                String id = readPattern("01 Assessment ID [String, existing]",
                                "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                TaxAssessment a = findAssessmentById(id);
                if (a == null) {
                        throw new RecordNotFoundException("No assessment has ID " + id + ".");
                }
                System.out.println("STEP 2 OF 4 - CHECKS");
                a.printDetails();
                if (!a.getStatus().equals("DRAFT")) {
                        throw new InvalidStatusTransitionException(
                                        "Only a DRAFT assessment can be cancelled (current: " + a.getStatus() + ").");
                }
                System.out.println("STEP 3 OF 4 - CHANGE");
                System.out.println("    Status DRAFT -> CANCELLED");
                System.out.println("STEP 4 OF 4 - DECISION");
                if (!confirmSave()) {
                        System.out.println("Cancelled. The assessment is unchanged.");
                        return;
                }
                a.cancel();
                System.out.println("Assessment " + a.getId() + " is now " + a.getStatus() + ".");
        }

        // ================= OPTION 18: VIEW TAXPAYER STATEMENT =================
        private static void viewTaxpayerStatement() throws RecordNotFoundException {
                System.out.println("\n--- TAXPAYER STATEMENT ---");
                String tin = readPattern("TIN [String, 9 digits, existing]", "\\d{9}",
                                "TIN must be exactly 9 digits.");
                RegisteredTaxpayer tp = requireTaxpayer(tin);
                System.out.println("\nTAXPAYER");
                tp.printDetails();

                BigDecimal due = BigDecimal.ZERO;
                BigDecimal pen = BigDecimal.ZERO;
                BigDecimal paid = BigDecimal.ZERO;
                BigDecimal refunded = BigDecimal.ZERO;
                BigDecimal outstanding = BigDecimal.ZERO;

                System.out.println("\nOBLIGATIONS, DECLARATIONS, ASSESSMENTS, PAYMENTS");
                int obCount = 0;
                for (int i = 0; i < obligationCount; i++) {
                        TaxObligation ob = obligations[i];
                        if (!ob.getTaxpayer().getTin().equals(tp.getTin())) {
                                continue;
                        }
                        obCount++;
                        System.out.println("* " + ob.getSummary());
                        for (int j = 0; j < penaltyCount; j++) {
                                if (penalties[j].getObligation() == ob) {
                                        System.out.println("    penalty: " + penalties[j].getSummary());
                                }
                        }
                        for (int j = 0; j < declarationCount; j++) {
                                if (declarations[j].getObligation() != ob) {
                                        continue;
                                }
                                System.out.println("    declaration: " + declarations[j].getSummary());
                                for (int k = 0; k < assessmentCount; k++) {
                                        TaxAssessment a = assessments[k];
                                        if (a.getDeclaration() != declarations[j]) {
                                                continue;
                                        }
                                        System.out.println("      assessment: " + a.getSummary());
                                        if (a.isConfirmed()) {
                                                for (int m = 0; m < paymentCount; m++) {
                                                        if (payments[m].getAssessment() == a) {
                                                                System.out.println("        payment: "
                                                                                + payments[m].getSummary());
                                                        }
                                                }
                                                due = due.add(a.getTotal());
                                                pen = pen.add(penaltiesFor(ob));
                                                paid = paid.add(paymentsFor(a));
                                                refunded = refunded.add(approvedRefundsFor(a));
                                                outstanding = outstanding.add(outstandingFor(a));
                                        }
                                }
                        }
                }
                if (obCount == 0) {
                        System.out.println("  (no obligations)");
                }

                System.out.println("\nREFUNDS");
                int rc = 0;
                for (int i = 0; i < refundCount; i++) {
                        if (refunds[i].getTaxpayer().getTin().equals(tp.getTin())) {
                                System.out.println("  " + refunds[i].getSummary());
                                rc++;
                        }
                }
                if (rc == 0) {
                        System.out.println("  (none)");
                }
                System.out.println("CLEARANCES");
                int cc = 0;
                for (int i = 0; i < clearanceCount; i++) {
                        if (clearances[i].getTaxpayer().getTin().equals(tp.getTin())) {
                                System.out.println("  " + clearances[i].getSummary());
                                cc++;
                        }
                }
                if (cc == 0) {
                        System.out.println("  (none)");
                }

                System.out.println("\nTOTALS (confirmed assessments only)");
                System.out.println("  Assessed ............ " + due);
                System.out.println("  + Penalties ......... " + pen);
                System.out.println("  - Payments .......... " + paid);
                System.out.println("  + Refunds paid out .. " + refunded);
                System.out.println("  = OUTSTANDING ....... " + outstanding + "  (negative = overpaid)");
        }

        // ================= OPTION 19: VIEW OUTSTANDING BALANCES =================
        private static void viewOutstandingBalances() {
                System.out.println("\n--- OUTSTANDING BALANCES (confirmed assessments) ---");
                BigDecimal grandTotal = BigDecimal.ZERO;
                int shown = 0;
                for (int i = 0; i < assessmentCount; i++) {
                        TaxAssessment a = assessments[i];
                        if (!a.isConfirmed()) {
                                continue;
                        }
                        BigDecimal out = outstandingFor(a);
                        if (out.signum() <= 0) {
                                continue;
                        }
                        shown++;
                        grandTotal = grandTotal.add(out);
                        TaxObligation ob = a.getDeclaration().getObligation();
                        System.out.println(shown + ". " + a.getId() + " | " + ob.getTaxpayer().getDisplayName()
                                        + " (TIN " + ob.getTaxpayer().getTin() + ") | " + ob.getTaxType().getTaxCode());
                        System.out.println("     total " + a.getTotal() + " + penalties " + penaltiesFor(ob)
                                        + " - paid " + paymentsFor(a) + " + refunds " + approvedRefundsFor(a)
                                        + " = OUTSTANDING " + out);
                }
                if (shown == 0) {
                        System.out.println("  No outstanding balances.");
                } else {
                        System.out.println("GRAND TOTAL OUTSTANDING: " + grandTotal);
                }
                for (int i = 0; i < assessmentCount; i++) {
                        if (assessments[i].isConfirmed() && outstandingFor(assessments[i]).signum() < 0) {
                                System.out.println("Note: " + assessments[i].getId() + " is overpaid by "
                                                + overpaymentFor(assessments[i]) + ".");
                        }
                }
        }

        // ================= OPTION 20: VIEW TAX REPORTS =================
        private static int countDeclarationsWithStatus(String status) {
                int n = 0;
                for (int i = 0; i < declarationCount; i++) {
                        if (declarations[i].getStatus().equals(status))
                                n++;
                }
                return n;
        }

        private static int countAssessmentsWithStatus(String status) {
                int n = 0;
                for (int i = 0; i < assessmentCount; i++) {
                        if (assessments[i].getStatus().equals(status))
                                n++;
                }
                return n;
        }

        private static int countRefundsWithStatus(String status) {
                int n = 0;
                for (int i = 0; i < refundCount; i++) {
                        if (refunds[i].getStatus().equals(status))
                                n++;
                }
                return n;
        }

        private static int countClearancesWithStatus(String status) {
                int n = 0;
                for (int i = 0; i < clearanceCount; i++) {
                        if (clearances[i].getStatus().equals(status))
                                n++;
                }
                return n;
        }

        private static void viewTaxReports() {
                System.out.println("\n================ TAX REPORTS ================");
                System.out.println("1. RECORD COUNTS");
                System.out.println("   Individuals " + individualCount + " | Corporates " + corporateCount
                                + " | Officers " + officerCount + " | Tax types " + taxTypeCount);
                System.out.println("   Obligations " + obligationCount + " | Declarations " + declarationCount
                                + " | Assessments " + assessmentCount + " | Payments " + paymentCount);
                System.out.println("   Penalties " + penaltyCount + " | Refunds " + refundCount
                                + " | Clearances " + clearanceCount);

                System.out.println("2. DECLARATIONS BY STATUS");
                String[] declStatuses = { "SUBMITTED", "LATE", "ASSESSED", "REJECTED" };
                for (int s = 0; s < declStatuses.length; s++) {
                        System.out.println(
                                        "   " + declStatuses[s] + ": " + countDeclarationsWithStatus(declStatuses[s]));
                }
                System.out.println("3. ASSESSMENTS BY STATUS");
                String[] assessStatuses = { "DRAFT", "CONFIRMED", "CANCELLED" };
                for (int s = 0; s < assessStatuses.length; s++) {
                        System.out.println("   " + assessStatuses[s] + ": "
                                        + countAssessmentsWithStatus(assessStatuses[s]));
                }
                System.out.println("4. REFUNDS BY STATUS");
                String[] decisionStatuses = { "PENDING", "APPROVED", "REJECTED" };
                for (int s = 0; s < decisionStatuses.length; s++) {
                        System.out.println("   " + decisionStatuses[s] + ": "
                                        + countRefundsWithStatus(decisionStatuses[s]));
                }
                System.out.println("5. CLEARANCES BY STATUS");
                for (int s = 0; s < decisionStatuses.length; s++) {
                        System.out.println("   " + decisionStatuses[s] + ": "
                                        + countClearancesWithStatus(decisionStatuses[s]));
                }

                System.out.println("6. BY TAX TYPE");
                for (int t = 0; t < taxTypeCount; t++) {
                        TaxType tt = taxTypes[t];
                        int decls = 0;
                        BigDecimal declared = BigDecimal.ZERO;
                        BigDecimal assessed = BigDecimal.ZERO;
                        BigDecimal received = BigDecimal.ZERO;
                        for (int i = 0; i < declarationCount; i++) {
                                if (declarations[i].getObligation().getTaxType() == tt) {
                                        decls++;
                                        declared = declared.add(declarations[i].getDeclaredTax());
                                }
                        }
                        for (int i = 0; i < assessmentCount; i++) {
                                if (assessments[i].isConfirmed()
                                                && assessments[i].getDeclaration().getObligation().getTaxType() == tt) {
                                        assessed = assessed.add(assessments[i].getTotal());
                                }
                        }
                        for (int i = 0; i < paymentCount; i++) {
                                if (payments[i].getAssessment().getDeclaration().getObligation().getTaxType() == tt) {
                                        received = received.add(payments[i].getAmount());
                                }
                        }
                        System.out.println("   " + tt.getTaxCode() + " (" + tt.getRate() + "%): declarations "
                                        + decls + ", declared tax " + declared + ", assessed " + assessed
                                        + ", payments received " + received);
                }

                System.out.println("7. PAYMENTS BY METHOD");
                String[] methods = { "CASH", "MOBILE_MONEY", "BANK_TRANSFER", "CARD" };
                for (int m = 0; m < methods.length; m++) {
                        BigDecimal sum = BigDecimal.ZERO;
                        int n = 0;
                        for (int i = 0; i < paymentCount; i++) {
                                if (payments[i].getPaymentMethod().equals(methods[m])) {
                                        sum = sum.add(payments[i].getAmount());
                                        n++;
                                }
                        }
                        System.out.println("   " + methods[m] + ": " + n + " payment(s), total " + sum);
                }

                System.out.println("8. MONEY SUMMARY");
                BigDecimal assessedTotal = BigDecimal.ZERO;
                BigDecimal penaltyTotal = BigDecimal.ZERO;
                BigDecimal paymentTotal = BigDecimal.ZERO;
                BigDecimal refundTotal = BigDecimal.ZERO;
                BigDecimal outstandingSum = BigDecimal.ZERO;
                BigDecimal overpaidSum = BigDecimal.ZERO;
                for (int i = 0; i < assessmentCount; i++) {
                        if (assessments[i].isConfirmed()) {
                                assessedTotal = assessedTotal.add(assessments[i].getTotal());
                                BigDecimal out = outstandingFor(assessments[i]);
                                if (out.signum() > 0) {
                                        outstandingSum = outstandingSum.add(out);
                                } else if (out.signum() < 0) {
                                        overpaidSum = overpaidSum.add(out.negate());
                                }
                        }
                }
                for (int i = 0; i < penaltyCount; i++) {
                        penaltyTotal = penaltyTotal.add(penalties[i].getPenaltyAmount());
                }
                for (int i = 0; i < paymentCount; i++) {
                        paymentTotal = paymentTotal.add(payments[i].getAmount());
                }
                for (int i = 0; i < refundCount; i++) {
                        if (refunds[i].getStatus().equals("APPROVED")) {
                                refundTotal = refundTotal.add(refunds[i].getRequestedAmount());
                        }
                }
                System.out.println("   Confirmed assessments total ... " + assessedTotal);
                System.out.println("   Penalties recorded ............ " + penaltyTotal);
                System.out.println("   Payments received ............. " + paymentTotal);
                System.out.println("   Refunds approved .............. " + refundTotal);
                System.out.println("   Outstanding (owed) ............ " + outstandingSum);
                System.out.println("   Overpaid (not yet refunded) ... " + overpaidSum);
        }

        // ================= OPTION 21: DEMONSTRATE POLYMORPHISM =================
        private static void demonstratePolymorphism() throws RecordNotFoundException {
                int partyTotal = individualCount + corporateCount + officerCount;
                if (partyTotal == 0) {
                        throw new RecordNotFoundException(
                                        "Register at least one taxpayer or officer first (options 1 to 3).");
                }

                // ARRAY 1: TaxParty[] holds three DIFFERENT concrete types
                TaxParty[] parties = new TaxParty[partyTotal];
                int k = 0;
                for (int i = 0; i < individualCount; i++) {
                        parties[k++] = individuals[i];
                }
                for (int i = 0; i < corporateCount; i++) {
                        parties[k++] = corporates[i];
                }
                for (int i = 0; i < officerCount; i++) {
                        parties[k++] = officers[i];
                }

                System.out.println("\n=== ARRAY 1: TaxParty[] with " + partyTotal + " object(s) ===");
                System.out.println("Same method calls on every element, different behaviour at run time.");
                for (int i = 0; i < parties.length; i++) {
                        TaxParty p = parties[i];
                        System.out.println("\n" + (i + 1) + ". real type: " + p.getClass().getSimpleName());
                        System.out.println("     getDisplayName() -> " + p.getDisplayName());
                        System.out.println("     getSummary()     -> " + p.getSummary());
                        System.out.println("     isActive()       -> " + p.isActive());
                        // instanceof + cast = reach the role-specific part of the object
                        if (p instanceof IndividualTaxpayer) {
                                IndividualTaxpayer it = (IndividualTaxpayer) p;
                                System.out.println("     role-specific    -> business " + it.getBusinessName()
                                                + ", resident " + it.isResident());
                        } else if (p instanceof CorporateTaxpayer) {
                                CorporateTaxpayer ct = (CorporateTaxpayer) p;
                                System.out.println("     role-specific    -> legal name " + ct.getLegalName()
                                                + ", representative " + ct.getAuthorizedRepresentative());
                        } else if (p instanceof RevenueOfficer) {
                                RevenueOfficer ro = (RevenueOfficer) p;
                                System.out.println("     role-specific    -> approval limit " + ro.getApprovalLimit()
                                                + ", can approve 100000.00? "
                                                + ro.canApprove(new BigDecimal("100000.00")));
                        }
                }

                // ARRAY 2: Identifiable[] holds EVERY kind of record
                int idTotal = partyTotal + taxTypeCount + obligationCount + declarationCount
                                + assessmentCount + paymentCount + penaltyCount + refundCount + clearanceCount;
                Identifiable[] all = new Identifiable[idTotal];
                int n = 0;
                for (int i = 0; i < parties.length; i++) {
                        all[n++] = parties[i];
                }
                for (int i = 0; i < taxTypeCount; i++) {
                        all[n++] = taxTypes[i];
                }
                for (int i = 0; i < obligationCount; i++) {
                        all[n++] = obligations[i];
                }
                for (int i = 0; i < declarationCount; i++) {
                        all[n++] = declarations[i];
                }
                for (int i = 0; i < assessmentCount; i++) {
                        all[n++] = assessments[i];
                }
                for (int i = 0; i < paymentCount; i++) {
                        all[n++] = payments[i];
                }
                for (int i = 0; i < penaltyCount; i++) {
                        all[n++] = penalties[i];
                }
                for (int i = 0; i < refundCount; i++) {
                        all[n++] = refunds[i];
                }
                for (int i = 0; i < clearanceCount; i++) {
                        all[n++] = clearances[i];
                }

                System.out.println("\n=== ARRAY 2: Identifiable[] with " + idTotal + " object(s) ===");
                System.out.println("One interface type, many unrelated classes.");
                for (int i = 0; i < all.length; i++) {
                        System.out.println((i + 1) + ". " + all[i].getClass().getSimpleName() + " | id "
                                        + all[i].getId() + " | " + all[i].getDisplayName()
                                        + " | active " + all[i].isActive());
                }
                System.out.println("\nSame call, different object, different result: that is runtime polymorphism.");
        }

        // ================= OPTION 12: RECORD TAX PAYMENT =================
        private static void recordPayment()
                        throws ValidationException, DuplicateRecordException, PrerequisiteMissingException {
                if (paymentCount >= payments.length) {
                        System.out.println("Capacity reached (" + payments.length
                                        + " payments). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- RECORD TAX PAYMENT  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - IDENTIFICATION AND PREREQUISITES");
                String id;
                while (true) {
                        id = readPattern("01 Payment ID [String, unique, 3-30 letters/digits/-]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        if (findPaymentById(id) == null)
                                break;
                        System.out.println("    Duplicate: that payment ID is already used.");
                }
                TaxAssessment a;
                while (true) {
                        a = selectConfirmedAssessment();
                        if (outstandingFor(a).signum() > 0)
                                break;
                        System.out.println("    Rejected: assessment " + a.getId() + " has nothing left to pay.");
                }
                String ref;
                while (true) {
                        ref = readPattern("03 Payment reference [String, globally unique, 6-30 of A-Z 0-9 -]",
                                        "[A-Z0-9-]{6,30}", "6-30 UPPERCASE letters, digits or hyphens.");
                        if (!paymentReferenceExists(ref))
                                break;
                        System.out.println("    Duplicate: that payment reference was already used.");
                }

                System.out.println("STEP 2 OF 4 - PAYMENT DETAILS");
                LocalDate date;
                while (true) {
                        date = readDateNotFuture("05 Payment date [LocalDate, YYYY-MM-DD, not before "
                                        + a.getAssessmentDate() + "]");
                        if (!date.isBefore(a.getAssessmentDate()))
                                break;
                        System.out.println("    Invalid: payment date cannot be before the assessment date.");
                }
                String method = readChoice("06 Payment method [String: CASH / MOBILE_MONEY / BANK_TRANSFER / CARD]",
                                new String[] { "CASH", "MOBILE_MONEY", "BANK_TRANSFER", "CARD" });

                System.out.println("STEP 3 OF 4 - BALANCE AND VALIDATION");
                BigDecimal outstanding = outstandingFor(a);
                System.out.println("    Assessment total ......... " + a.getTotal());
                System.out.println("    + Penalties .............. "
                                + penaltiesFor(a.getDeclaration().getObligation()));
                System.out.println("    - Payments so far ........ " + paymentsFor(a));
                System.out.println("    + Refunds already paid ... " + approvedRefundsFor(a));
                System.out.println("    = OUTSTANDING BALANCE .... " + outstanding);
                TaxPayment p;
                while (true) {
                        BigDecimal amount = readMoney("04 Amount [BigDecimal, > 0, not above the outstanding balance]");
                        try {
                                p = new TaxPayment(id, a, ref, amount, date, method, outstanding);
                                break;
                        } catch (ValidationException e) {
                                System.out.println("    Rejected (" + e.getClass().getSimpleName() + "): "
                                                + e.getMessage());
                        }
                }
                System.out.println("    Balance after this payment will be "
                                + outstanding.subtract(p.getAmount())
                                + (p.getAmount().compareTo(outstanding) < 0 ? " (partial payment)" : " (fully paid)"));

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 6 FIELDS");
                p.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. No payment and no balance change were saved.");
                        return;
                }

                // Revalidate at SAVE time with a FRESH balance
                p.validate();
                p.checkAgainstBalance(outstandingFor(a));
                if (findPaymentById(p.getId()) != null) {
                        throw new DuplicateRecordException("Payment ID already exists.");
                }
                if (paymentReferenceExists(p.getPaymentReference())) {
                        throw new DuplicateRecordException("Payment reference already exists.");
                }
                payments[paymentCount] = p;
                paymentCount++;
                System.out.println("Payment saved. Outstanding balance of " + a.getId()
                                + " is now " + outstandingFor(a));
        }

        // ================= OPTION 13: CALCULATE / RECORD PENALTY =================
        private static void managePenalty()
                        throws ValidationException, DuplicateRecordException,
                        PrerequisiteMissingException, RecordNotFoundException {
                String c = readChoice("Choose [1] Record a new penalty   [2] Revise the rate of a penalty",
                                new String[] { "1", "2" });
                if (c.equals("1")) {
                        recordPenalty();
                } else {
                        revisePenalty();
                }
        }

        private static void recordPenalty()
                        throws ValidationException, DuplicateRecordException, PrerequisiteMissingException {
                if (penaltyCount >= penalties.length) {
                        System.out.println("Capacity reached (" + penalties.length
                                        + " penalties). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- CALCULATE / RECORD PENALTY  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - IDENTIFICATION AND PREREQUISITES");
                String id;
                while (true) {
                        id = readPattern("01 Penalty ID [String, unique, 3-30 letters/digits/-]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        if (findPenaltyById(id) == null)
                                break;
                        System.out.println("    Duplicate: that penalty ID is already used.");
                }
                TaxObligation ob;
                String type;
                while (true) {
                        ob = selectObligation();
                        type = readChoice("03 Penalty type [String: LATE_FILING / LATE_PAYMENT]",
                                        new String[] { "LATE_FILING", "LATE_PAYMENT" });
                        Penalty existing = findPenaltyFor(ob, type);
                        if (existing == null)
                                break;
                        System.out.println("    Rejected: obligation " + ob.getId() + " already has a "
                                        + type + " penalty (" + existing.getId() + ").");
                }

                System.out.println("STEP 2 OF 4 - BASE AND RATE");
                BigDecimal base;
                BigDecimal rate;
                if (type.equals("LATE_FILING")) {
                        TaxDeclaration late = findLateDeclarationFor(ob);
                        if (late == null) {
                                throw new PrerequisiteMissingException("No late declaration exists for obligation "
                                                + ob.getId() + ". A LATE_FILING penalty needs one.");
                        }
                        base = late.getDeclaredTax();
                        System.out.println("    Late declaration " + late.getId() + " (filed "
                                        + late.getFilingDate() + ", due " + ob.getDueDate()
                                        + "). Base amount = its declared tax = " + base);
                        rate = readRateOrDefault("04 Penalty rate % [BigDecimal, 0-100, Enter = "
                                        + Penalty.LATE_FILING_RATE + " illustrative]", Penalty.LATE_FILING_RATE);
                } else {
                        if (!ob.isOverdue(LocalDate.now())) {
                                throw new PrerequisiteMissingException("Obligation " + ob.getId()
                                                + " is not overdue yet (due " + ob.getDueDate() + ").");
                        }
                        base = readMoney("05 Base amount [BigDecimal, >= 0, 2 decimals]");
                        rate = readRateOrDefault("04 Penalty rate % [BigDecimal, 0-100, Enter = "
                                        + Penalty.LATE_FILING_RATE + " illustrative]", Penalty.LATE_FILING_RATE);
                }

                Penalty pen = new Penalty(id, ob, type, base, rate);
                System.out.println("STEP 3 OF 4 - CALCULATION");
                System.out.println("    " + base + " x " + rate + " / 100 = " + pen.getPenaltyAmount()
                                + "  (rounded HALF_UP to 2 decimals)");

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 6 FIELDS");
                pen.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return;
                }

                pen.validate();
                if (findPenaltyById(pen.getId()) != null) {
                        throw new DuplicateRecordException("Penalty ID already exists.");
                }
                if (findPenaltyFor(ob, pen.getPenaltyType()) != null) {
                        throw new DuplicateRecordException("That obligation already has this penalty type.");
                }
                penalties[penaltyCount] = pen;
                penaltyCount++;
                System.out.println("Penalty saved. Total penalties: " + penaltyCount);
                printBalanceNote(ob);
        }

        private static void revisePenalty() throws ValidationException, RecordNotFoundException {
                System.out.println("\n+------- REVISE PENALTY RATE  (type CANCEL to stop) -------+");
                if (penaltyCount == 0) {
                        throw new RecordNotFoundException("There is no penalty to revise.");
                }
                System.out.println("STEP 1 OF 4 - SELECT A PENALTY");
                for (int i = 0; i < penaltyCount; i++) {
                        System.out.println("    " + penalties[i].getSummary());
                }
                Penalty pen;
                while (true) {
                        String id = readPattern("01 Penalty ID [String, one of the above]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        pen = findPenaltyById(id);
                        if (pen != null)
                                break;
                        System.out.println("    No penalty has ID " + id + ".");
                }
                System.out.println("STEP 2 OF 4 - NEW RATE");
                BigDecimal newRate = readRate("02 New penalty rate % [BigDecimal, 0-100, 2 decimals]");
                BigDecimal newAmount = pen.getBaseAmount().multiply(newRate)
                                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                System.out.println("STEP 3 OF 4 - CALCULATION");
                System.out.println("    Old: " + pen.getPenaltyRate() + "% = " + pen.getPenaltyAmount()
                                + "   New: " + newRate + "% = " + newAmount);
                System.out.println("STEP 4 OF 4 - DECISION");
                if (!confirmSave()) {
                        System.out.println("Cancelled. The penalty is unchanged.");
                        return;
                }
                pen.setPenaltyRate(newRate);
                System.out.println("Penalty " + pen.getId() + " revised to " + pen.getPenaltyAmount() + ".");
                printBalanceNote(pen.getObligation());
        }

        // ================= OPTION 14: REQUEST / APPROVE REFUND =================
        private static void manageRefund()
                        throws ValidationException, DuplicateRecordException, PrerequisiteMissingException,
                        RecordNotFoundException, InvalidStatusTransitionException,
                        UnauthorizedApprovalException {
                String c = readChoice("Choose [1] Request a refund   [2] Approve or reject a pending refund",
                                new String[] { "1", "2" });
                if (c.equals("1")) {
                        requestRefund();
                } else {
                        decideRefund();
                }
        }

        private static void requestRefund()
                        throws ValidationException, DuplicateRecordException, PrerequisiteMissingException {
                if (refundCount >= refunds.length) {
                        System.out.println("Capacity reached (" + refunds.length
                                        + " refunds). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- REQUEST REFUND  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - IDENTIFICATION AND PREREQUISITES");
                String id;
                while (true) {
                        id = readPattern("01 Refund ID [String, unique, 3-30 letters/digits/-]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        if (findRefundById(id) == null)
                                break;
                        System.out.println("    Duplicate: that refund ID is already used.");
                }
                RegisteredTaxpayer tp = selectTaxpayer();
                TaxAssessment a;
                while (true) {
                        a = selectConfirmedAssessment();
                        String owner = a.getDeclaration().getObligation().getTaxpayer().getTin();
                        if (!owner.equals(tp.getTin())) {
                                System.out.println("    Rejected: that assessment belongs to TIN " + owner + ".");
                                continue;
                        }
                        TaxRefund pending = findPendingRefundFor(a);
                        if (pending != null) {
                                System.out.println("    Rejected: it already has a pending refund (" + pending.getId()
                                                + ").");
                                continue;
                        }
                        break;
                }

                System.out.println("STEP 2 OF 4 - REFUND DETAILS");
                BigDecimal overpayment = overpaymentFor(a);
                System.out.println("    Total " + a.getTotal() + ", penalties "
                                + penaltiesFor(a.getDeclaration().getObligation()) + ", payments "
                                + paymentsFor(a) + ", refunded " + approvedRefundsFor(a));
                System.out.println("    VERIFIED OVERPAYMENT = " + overpayment);
                if (overpayment.signum() <= 0) {
                        throw new OverpaymentException("No verified overpayment exists for assessment "
                                        + a.getId() + ", so a refund cannot be requested.");
                }
                LocalDate date;
                while (true) {
                        date = readDateNotFuture("05 Request date [LocalDate, YYYY-MM-DD, not before "
                                        + a.getAssessmentDate() + "]");
                        if (!date.isBefore(a.getAssessmentDate()))
                                break;
                        System.out.println("    Invalid: the request date cannot be before the assessment date.");
                }
                TaxRefund r;
                while (true) {
                        BigDecimal amount = readMoney(
                                        "04 Requested amount [BigDecimal, > 0, not above the overpayment]");
                        try {
                                r = new TaxRefund(id, tp, a, amount, date, overpayment);
                                break;
                        } catch (ValidationException e) {
                                System.out.println("    Rejected (" + e.getClass().getSimpleName() + "): "
                                                + e.getMessage());
                        }
                }

                System.out.println("STEP 3 OF 4 - VALIDATION");
                System.out.println("    Assessment is CONFIRMED and belongs to the taxpayer .... OK");
                System.out.println("    No other pending refund ................................ OK");
                System.out.println("    Requested " + r.getRequestedAmount() + " <= overpayment " + overpayment
                                + " ... OK");

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 6 FIELDS (new refunds start as PENDING)");
                r.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return;
                }

                r.validate();
                r.checkAgainstOverpayment(overpaymentFor(a)); // fresh figure at save time
                if (findRefundById(r.getId()) != null) {
                        throw new DuplicateRecordException("Refund ID already exists.");
                }
                if (findPendingRefundFor(a) != null) {
                        throw new DuplicateRecordException("That assessment already has a pending refund.");
                }
                refunds[refundCount] = r;
                refundCount++;
                System.out.println("Refund request saved as PENDING. Use option 14, choice 2, to decide it.");
        }

        private static void decideRefund()
                        throws ValidationException, RecordNotFoundException, PrerequisiteMissingException,
                        InvalidStatusTransitionException, UnauthorizedApprovalException {
                System.out.println("\n+------- APPROVE OR REJECT REFUND  (type CANCEL to stop) -------+");
                System.out.println("STEP 1 OF 4 - SELECT A PENDING REFUND");
                boolean any = false;
                for (int i = 0; i < refundCount; i++) {
                        if (refunds[i].getStatus().equals("PENDING")) {
                                System.out.println("    " + refunds[i].getSummary());
                                any = true;
                        }
                }
                if (!any) {
                        throw new RecordNotFoundException("There is no PENDING refund to decide.");
                }
                TaxRefund r;
                while (true) {
                        String id = readPattern("01 Refund ID [String, one of the PENDING ones above]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        r = findRefundById(id);
                        if (r == null) {
                                System.out.println("    No refund has ID " + id + ".");
                        } else if (!r.getStatus().equals("PENDING")) {
                                System.out.println("    Rejected: that refund is " + r.getStatus() + ".");
                        } else {
                                break;
                        }
                }
                System.out.println("STEP 2 OF 4 - DETAILS");
                r.printDetails();
                System.out.println("    Verified overpayment right now: " + overpaymentFor(r.getAssessment()));

                System.out.println("STEP 3 OF 4 - DECISION");
                String action = readChoice("Decision [APPROVE / REJECT]  (type CANCEL to leave it pending)",
                                new String[] { "APPROVE", "REJECT" });
                System.out.println("STEP 4 OF 4 - RESULT");
                if (action.equals("REJECT")) {
                        r.reject();
                        System.out.println("Refund " + r.getId() + " is now " + r.getStatus() + ".");
                        return;
                }
                RevenueOfficer officer = selectOfficer();
                r.approve(officer, overpaymentFor(r.getAssessment()));
                System.out.println("Refund " + r.getId() + " is now " + r.getStatus()
                                + ", approved by " + officer.getDisplayName() + ".");
                printBalanceNote(r.getAssessment().getDeclaration().getObligation());
        }

        // ================= OPTION 15: REQUEST / ISSUE TAX CLEARANCE =================
        private static void manageClearance()
                        throws ValidationException, DuplicateRecordException, PrerequisiteMissingException,
                        RecordNotFoundException, InvalidStatusTransitionException,
                        UnauthorizedApprovalException {
                String c = readChoice("Choose [1] Request a clearance   [2] Issue or reject a pending clearance",
                                new String[] { "1", "2" });
                if (c.equals("1")) {
                        requestClearance();
                } else {
                        issueClearance();
                }
        }

        private static void requestClearance()
                        throws ValidationException, DuplicateRecordException, PrerequisiteMissingException {
                if (clearanceCount >= clearances.length) {
                        System.out.println("Capacity reached (" + clearances.length
                                        + " clearances). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- REQUEST TAX CLEARANCE  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - IDENTIFICATION AND PREREQUISITES");
                String id;
                while (true) {
                        id = readPattern("01 Clearance ID [String, unique, 3-30 letters/digits/-]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        if (findClearanceById(id) == null)
                                break;
                        System.out.println("    Duplicate: that clearance ID is already used.");
                }
                RegisteredTaxpayer tp = selectTaxpayer();

                System.out.println("STEP 2 OF 4 - DATES");
                LocalDate requestDate = readDateNotFuture("03 Request date [LocalDate, YYYY-MM-DD, not in the future]");
                TaxClearance c;
                while (true) {
                        LocalDate expiry = readDate("04 Expiry date [LocalDate, YYYY-MM-DD, after the request date]");
                        try {
                                c = new TaxClearance(id, tp, requestDate, expiry);
                                break;
                        } catch (ValidationException e) {
                                System.out.println("    Rejected: " + e.getMessage());
                        }
                }

                System.out.println("STEP 3 OF 4 - ELIGIBILITY PREVIEW");
                int overdue = countOverdueUnpaid(tp);
                int unresolved = countUnresolvedDeclarations(tp);
                System.out.println("    Overdue unpaid obligations: " + overdue
                                + "   Unresolved declarations: " + unresolved);
                System.out.println("    A clearance can only be APPROVED when both numbers are 0.");

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 6 FIELDS (new requests start as PENDING)");
                c.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return;
                }

                c.validate();
                if (findClearanceById(c.getId()) != null) {
                        throw new DuplicateRecordException("Clearance ID already exists.");
                }
                clearances[clearanceCount] = c;
                clearanceCount++;
                System.out.println("Clearance request saved as PENDING. Use option 15, choice 2, to issue it.");
        }

        private static void issueClearance()
                        throws RecordNotFoundException, PrerequisiteMissingException,
                        InvalidStatusTransitionException, UnauthorizedApprovalException {
                System.out.println("\n+------- ISSUE OR REJECT CLEARANCE  (type CANCEL to stop) -------+");
                System.out.println("STEP 1 OF 4 - SELECT A PENDING CLEARANCE");
                boolean any = false;
                for (int i = 0; i < clearanceCount; i++) {
                        if (clearances[i].getStatus().equals("PENDING")) {
                                System.out.println("    " + clearances[i].getSummary());
                                any = true;
                        }
                }
                if (!any) {
                        throw new RecordNotFoundException("There is no PENDING clearance to decide.");
                }
                TaxClearance c;
                while (true) {
                        String id = readPattern("01 Clearance ID [String, one of the PENDING ones above]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        c = findClearanceById(id);
                        if (c == null) {
                                System.out.println("    No clearance has ID " + id + ".");
                        } else if (!c.getStatus().equals("PENDING")) {
                                System.out.println("    Rejected: that clearance is " + c.getStatus() + ".");
                        } else {
                                break;
                        }
                }
                System.out.println("STEP 2 OF 4 - TAXPAYER STANDING");
                c.printDetails();
                int overdue = countOverdueUnpaid(c.getTaxpayer());
                int unresolved = countUnresolvedDeclarations(c.getTaxpayer());
                System.out.println("    Overdue unpaid obligations: " + overdue
                                + "   Unresolved declarations: " + unresolved);

                System.out.println("STEP 3 OF 4 - DECISION");
                String action = readChoice("Decision [APPROVE / REJECT]  (type CANCEL to leave it pending)",
                                new String[] { "APPROVE", "REJECT" });
                System.out.println("STEP 4 OF 4 - RESULT");
                if (action.equals("REJECT")) {
                        c.reject();
                        System.out.println("Clearance " + c.getId() + " is now " + c.getStatus() + ".");
                        return;
                }
                RevenueOfficer officer = selectOfficer();
                c.approve(officer, overdue, unresolved);
                System.out.println("Clearance " + c.getId() + " is now " + c.getStatus()
                                + ", issued by " + officer.getDisplayName() + ", valid until "
                                + c.getExpiryDate() + ".");
        }

        private static RegisteredTaxpayer selectTaxpayer() throws PrerequisiteMissingException {
                while (true) {
                        String tin = readPattern("02 Taxpayer TIN [String, 9 digits, must be registered]",
                                        "\\d{9}", "TIN must be exactly 9 digits.");
                        RegisteredTaxpayer t = findTaxpayerByTin(tin);
                        if (t == null) {
                                System.out.println("    No registered taxpayer has TIN " + tin + ".");
                                askRegisterPrerequisite("taxpayer");
                                continue;
                        }
                        if (!t.isActive()) {
                                System.out.println("    Rejected: that taxpayer is not ACTIVE ("
                                                + t.getTaxpayerStatus() + ").");
                                continue;
                        }
                        System.out.println("    Selected: " + t.getSummary());
                        return t;
                }
        }

        private static TaxType selectTaxType() throws PrerequisiteMissingException {
                while (true) {
                        String code = readText("03 Tax code [String, e.g. VAT, must be registered]", 2, 10)
                                        .toUpperCase();
                        TaxType tt = findTaxTypeByCode(code);
                        if (tt == null) {
                                System.out.println("    No tax type has code " + code + ".");
                                askRegisterPrerequisite("tax type");
                                continue;
                        }
                        if (!tt.isActive()) {
                                System.out.println("    Rejected: that tax type is not ACTIVE.");
                                continue;
                        }
                        System.out.println("    Selected: " + tt.getSummary());
                        return tt;
                }
        }

        private static TaxObligation selectObligation() throws PrerequisiteMissingException {
                while (true) {
                        String id = readPattern("02 Obligation ID [String, must exist]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        TaxObligation ob = findObligationById(id);
                        if (ob == null) {
                                System.out.println("    No obligation has ID " + id + ".");
                                askRegisterPrerequisite("obligation");
                                continue;
                        }
                        if (!ob.isActive()) {
                                System.out.println("    Rejected: that obligation is not active.");
                                continue;
                        }
                        System.out.println("    Selected: " + ob.getSummary());
                        return ob;
                }
        }

        private static TaxDeclaration selectDeclaration() throws PrerequisiteMissingException {
                while (true) {
                        String id = readPattern("02 Declaration ID [String, must exist]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        TaxDeclaration d = findDeclarationById(id);
                        if (d == null) {
                                System.out.println("    No declaration has ID " + id + ".");
                                askRegisterPrerequisite("declaration");
                                continue;
                        }
                        System.out.println("    Selected: " + d.getSummary());
                        return d;
                }
        }

        // ================= OPTION 5: CREATE TAX OBLIGATION =================
        private static void createObligation()
                        throws ValidationException, DuplicateRecordException, PrerequisiteMissingException {
                if (obligationCount >= obligations.length) {
                        System.out.println("Capacity reached (" + obligations.length
                                        + " obligations). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- CREATE TAX OBLIGATION  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - IDENTIFICATION AND PREREQUISITES");
                String id;
                while (true) {
                        id = readPattern("01 Obligation ID [String, unique, 3-30 letters/digits/-]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        if (findObligationById(id) == null)
                                break;
                        System.out.println("    Duplicate: that obligation ID is already used.");
                }
                RegisteredTaxpayer taxpayer = selectTaxpayer();
                TaxType taxType = selectTaxType();

                System.out.println("STEP 2 OF 4 - PERIOD AND DUE DATE");
                TaxObligation ob;
                while (true) {
                        LocalDate start = readDate("04 Period start [LocalDate, YYYY-MM-DD]");
                        LocalDate end = readDate("05 Period end [LocalDate, YYYY-MM-DD, on/after start]");
                        LocalDate due = readDate("06 Due date [LocalDate, YYYY-MM-DD, on/after period end]");
                        try {
                                ob = new TaxObligation(id, taxpayer, taxType, start, end, due);
                        } catch (InvalidTaxPeriodException e) {
                                System.out.println("    Rejected (InvalidTaxPeriodException): "
                                                + e.getMessage() + " Enter the three dates again.");
                                continue;
                        }
                        TaxObligation clash = findOverlap(ob);
                        if (clash != null) {
                                System.out.println("    Rejected: overlaps obligation " + clash.getId()
                                                + " (" + clash.getPeriodStart() + " to " + clash.getPeriodEnd()
                                                + "). Enter the three dates again.");
                                continue;
                        }
                        break;
                }

                System.out.println("STEP 3 OF 4 - VALIDATION");
                ob.validate();
                System.out.println("    Taxpayer is ACTIVE ............ OK (" + taxpayer.getDisplayName() + ")");
                System.out.println("    Tax type is ACTIVE ............ OK (" + taxType.getTaxCode()
                                + ", rate " + taxType.getRate() + "%, " + taxType.getFilingFrequency() + ")");
                System.out.println("    Period and due date ........... OK");
                System.out.println("    No overlapping obligation ..... OK");

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 6 FIELDS");
                ob.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return;
                }

                // Revalidate everything at SAVE time
                ob.validate();
                if (findObligationById(ob.getId()) != null) {
                        throw new DuplicateRecordException("Obligation ID already exists.");
                }
                TaxObligation clash = findOverlap(ob);
                if (clash != null) {
                        throw new DuplicateRecordException("Overlaps obligation " + clash.getId() + ".");
                }
                obligations[obligationCount] = ob;
                obligationCount++;
                System.out.println("Tax obligation saved. Total obligations: " + obligationCount);
        }

        // ================= OPTION 6: FILE TAX DECLARATION =================
        private static void fileDeclaration()
                        throws ValidationException, DuplicateRecordException, PrerequisiteMissingException {
                if (declarationCount >= declarations.length) {
                        System.out.println("Capacity reached (" + declarations.length
                                        + " declarations). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- FILE TAX DECLARATION  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - IDENTIFICATION AND PREREQUISITES");
                String id;
                while (true) {
                        id = readPattern("01 Declaration ID [String, unique, 3-30 letters/digits/-]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        if (findDeclarationById(id) == null)
                                break;
                        System.out.println("    Duplicate: that declaration ID is already used.");
                }
                TaxObligation ob;
                while (true) {
                        ob = selectObligation();
                        TaxDeclaration existing = findActiveDeclarationFor(ob);
                        if (existing == null)
                                break;
                        System.out.println("    Rejected: obligation " + ob.getId()
                                        + " already has an active declaration (" + existing.getId()
                                        + "). Choose another obligation.");
                }

                System.out.println("STEP 2 OF 4 - FILING DETAILS");
                LocalDate filingDate = readDateNotFuture("03 Filing date [LocalDate, YYYY-MM-DD, not in the future]");
                BigDecimal taxable = readMoney("04 Taxable amount [BigDecimal, >= 0, 2 decimals]");

                System.out.println("STEP 3 OF 4 - CALCULATIONS");
                BigDecimal expected = ob.getTaxType().calculateTax(taxable);
                System.out.println("    Rate " + ob.getTaxType().getRate() + "%  ->  expected tax = "
                                + taxable + " x " + ob.getTaxType().getRate() + " / 100 = " + expected);
                boolean late = filingDate.isAfter(ob.getDueDate());
                System.out.println("    Due date " + ob.getDueDate() + "  ->  "
                                + (late ? "LATE (filed after the due date, filing date is kept as typed)"
                                                : "on time"));
                TaxDeclaration d;
                while (true) {
                        BigDecimal declared = readMoney(
                                        "05 Declared tax [BigDecimal, must match expected within 0.01]");
                        try {
                                d = new TaxDeclaration(id, ob, filingDate, taxable, declared);
                                break;
                        } catch (InvalidTaxCalculationException e) {
                                System.out.println("    Rejected (InvalidTaxCalculationException): "
                                                + e.getMessage());
                        }
                }

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 6 FIELDS (status is set by the system)");
                d.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return;
                }

                d.validate();
                if (findDeclarationById(d.getId()) != null) {
                        throw new DuplicateRecordException("Declaration ID already exists.");
                }
                if (findActiveDeclarationFor(ob) != null) {
                        throw new DuplicateRecordException(
                                        "Obligation " + ob.getId() + " already has an active declaration.");
                }
                declarations[declarationCount] = d;
                declarationCount++;
                System.out.println("Tax declaration saved with status " + d.getStatus()
                                + ". Total declarations: " + declarationCount);
        }

        // ================= OPTION 7: ASSESS DECLARATION =================
        private static void assessDeclaration()
                        throws ValidationException, DuplicateRecordException, PrerequisiteMissingException {
                if (assessmentCount >= assessments.length) {
                        System.out.println("Capacity reached (" + assessments.length
                                        + " assessments). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- ASSESS DECLARATION  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - IDENTIFICATION AND PREREQUISITES");
                String id;
                while (true) {
                        id = readPattern("01 Assessment ID [String, unique, 3-30 letters/digits/-]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        if (findAssessmentById(id) == null)
                                break;
                        System.out.println("    Duplicate: that assessment ID is already used.");
                }
                TaxDeclaration d;
                while (true) {
                        d = selectDeclaration();
                        if (d.getStatus().equals("REJECTED")) {
                                System.out.println("    Rejected: a rejected declaration cannot be assessed.");
                                continue;
                        }
                        if (!d.getStatus().equals("SUBMITTED") && !d.getStatus().equals("LATE")) {
                                System.out.println("    Rejected: that declaration is already " + d.getStatus() + ".");
                                continue;
                        }
                        TaxAssessment live = findLiveAssessmentFor(d);
                        if (live != null) {
                                System.out.println("    Rejected: it already has assessment " + live.getId()
                                                + " (" + live.getStatus() + ").");
                                continue;
                        }
                        break;
                }

                System.out.println("STEP 2 OF 4 - ASSESSMENT DETAILS");
                System.out.println("    Declared tax is " + d.getDeclaredTax() + ", filed "
                                + d.getFilingDate() + " (" + d.getStatus() + ").");
                TaxAssessment a;
                while (true) {
                        BigDecimal assessed = readMoney("03 Assessed tax [BigDecimal, >= 0, 2 decimals]");
                        BigDecimal adjustment = readSignedMoney(
                                        "04 Adjustment amount [BigDecimal, may be negative, total must be >= 0]");
                        LocalDate date = readDateNotFuture(
                                        "05 Assessment date [LocalDate, YYYY-MM-DD, not before filing date]");
                        try {
                                a = new TaxAssessment(id, d, assessed, adjustment, date);
                                break;
                        } catch (ValidationException e) {
                                System.out.println("    Rejected: " + e.getMessage()
                                                + " Enter the three values again.");
                        }
                }

                System.out.println("STEP 3 OF 4 - CALCULATION");
                System.out.println("    Declared tax ...... " + d.getDeclaredTax());
                System.out.println("    Assessed tax ...... " + a.getAssessedTax());
                System.out.println("    Adjustment ........ " + a.getAdjustmentAmount());
                System.out.println("    TOTAL (assessed + adjustment) = " + a.getTotal());

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 6 FIELDS (new assessments start as DRAFT)");
                a.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return;
                }

                a.validate();
                if (findAssessmentById(a.getId()) != null) {
                        throw new DuplicateRecordException("Assessment ID already exists.");
                }
                if (findLiveAssessmentFor(d) != null) {
                        throw new DuplicateRecordException(
                                        "Declaration " + d.getId() + " already has a live assessment.");
                }
                assessments[assessmentCount] = a;
                assessmentCount++;
                System.out.println("Assessment saved as DRAFT. Use option 8 to confirm it.");
        }

        // ================= OPTION 8: CONFIRM ASSESSMENT =================
        private static void confirmAssessment()
                        throws RecordNotFoundException, InvalidStatusTransitionException, ValidationException {
                System.out.println("\n+------- CONFIRM ASSESSMENT  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - SELECT A DRAFT ASSESSMENT");
                boolean anyDraft = false;
                for (int i = 0; i < assessmentCount; i++) {
                        if (assessments[i].getStatus().equals("DRAFT")) {
                                System.out.println("    " + assessments[i].getSummary());
                                anyDraft = true;
                        }
                }
                if (!anyDraft) {
                        throw new RecordNotFoundException("There is no DRAFT assessment to confirm.");
                }
                TaxAssessment a;
                while (true) {
                        String id = readPattern("01 Assessment ID [String, one of the DRAFT ones above]",
                                        "[A-Za-z0-9-]{3,30}", "3-30 letters, digits or hyphens.");
                        a = findAssessmentById(id);
                        if (a == null) {
                                System.out.println("    No assessment has ID " + id + ".");
                        } else if (!a.getStatus().equals("DRAFT")) {
                                System.out.println(
                                                "    Rejected: only a DRAFT assessment can be confirmed (this one is "
                                                                + a.getStatus() + ").");
                        } else {
                                break;
                        }
                }

                System.out.println("STEP 2 OF 4 - DETAILS");
                a.printDetails();

                System.out.println("STEP 3 OF 4 - VALIDATION");
                a.validate();
                System.out.println("    Fields valid .................. OK");
                System.out.println("    Declaration " + a.getDeclaration().getId() + " is "
                                + a.getDeclaration().getStatus() + " ... OK (still assessable)");
                System.out.println("    Total payable (before penalties) = " + a.getTotal());

                System.out.println("STEP 4 OF 4 - DECISION");
                while (true) {
                        String v = readLine("[1] CONFIRM   [0] CANCEL");
                        if (v.equals("0")) {
                                System.out.println("Cancelled. Nothing was changed.");
                                return;
                        }
                        if (v.equals("1")) {
                                break;
                        }
                        System.out.println("    Invalid: enter 1 or 0.");
                }
                a.validate(); // revalidate at confirm time
                a.confirm(); // also marks the declaration ASSESSED
                System.out.println("Assessment " + a.getId() + " is now " + a.getStatus()
                                + ". Declaration " + a.getDeclaration().getId() + " is now "
                                + a.getDeclaration().getStatus() + ". Payments can now be recorded.");
        }

        private static void printMenu() {
                System.out.println();
                System.out.println("================ TAX ADMINISTRATION MANAGEMENT ================");
                row(1, "Register Individual Taxpayer", 12, "Record Tax Payment");
                row(2, "Register Corporate Taxpayer", 13, "Calculate / Record Penalty");
                row(3, "Register Revenue Officer", 14, "Request / Approve Refund");
                row(4, "Register Tax Type", 15, "Request / Issue Tax Clearance");
                row(5, "Create Tax Obligation", 16, "Search Record by ID / TIN");
                row(6, "File Tax Declaration", 17, "Update Eligible Record");
                row(7, "Assess Declaration", 18, "View Taxpayer Statement");
                row(8, "Confirm Assessment", 19, "View Outstanding Balances");
                row(9, "View All Records", 20, "View Tax Reports");
                row(10, "View Declaration History", 21, "Demonstrate Polymorphism");
                row(11, "View Assessment History", 0, "Exit");
        }

        private static void row(int a, String left, int b, String right) {
                System.out.printf(" %2d %-30s %2d %s%n", a, left, b, right);
        }

        // ================= INPUT HELPERS (every one re-prompts only its own field)
        // =================

        // The ONLY place that touches the Scanner. Always nextLine(), never nextInt().
        private static String readLine(String prompt) {
                System.out.print("  " + prompt + " : ");
                if (!sc.hasNextLine()) {
                        throw new CancelledException("Input closed.");
                }
                String text = sc.nextLine().trim();
                if (text.equalsIgnoreCase("CANCEL")) {
                        throw new CancelledException("Cancelled by user.");
                }
                return text;
        }

        private static String readText(String label, int min, int max) {
                while (true) {
                        String v = readLine(label);
                        if (v.length() >= min && v.length() <= max) {
                                return v;
                        }
                        System.out.println("    Invalid: must be " + min + "-" + max
                                        + " characters (blank is not allowed).");
                }
        }

        private static String readPattern(String label, String regex, String message) {
                while (true) {
                        String v = readLine(label);
                        if (v.matches(regex)) {
                                return v;
                        }
                        System.out.println("    Invalid: " + message);
                }
        }

        private static LocalDate readDateNotFuture(String label) {
                while (true) {
                        String v = readLine(label);
                        try {
                                LocalDate d = LocalDate.parse(v); // strict YYYY-MM-DD
                                if (d.isAfter(LocalDate.now())) {
                                        System.out.println("    Invalid: the date cannot be in the future.");
                                } else {
                                        return d;
                                }
                        } catch (DateTimeParseException e) {
                                System.out.println("    Invalid: use a real date as YYYY-MM-DD.");
                        }
                }
        }

        // Money: digits with at most 2 decimals, at most 12 digits before the point
        private static BigDecimal readMoney(String label) {
                while (true) {
                        String v = readLine(label);
                        if (v.matches("\\d{1,12}(\\.\\d{1,2})?")) {
                                return new BigDecimal(v);
                        }
                        System.out.println("    Invalid: use a non-negative amount, max 2 decimals, "
                                        + "up to 999999999999.99.");
                }
        }

        private static BigDecimal readRate(String label) {
                while (true) {
                        String v = readLine(label);
                        if (v.matches("\\d{1,3}(\\.\\d{1,2})?")) {
                                BigDecimal r = new BigDecimal(v);
                                if (r.compareTo(new BigDecimal("100")) <= 0) {
                                        return r;
                                }
                        }
                        System.out.println("    Invalid: use a rate from 0 to 100 with at most 2 decimals.");
                }
        }

        private static boolean readYesNo(String label) {
                while (true) {
                        String v = readLine(label);
                        if (v.equalsIgnoreCase("YES") || v.equalsIgnoreCase("Y"))
                                return true;
                        if (v.equalsIgnoreCase("NO") || v.equalsIgnoreCase("N"))
                                return false;
                        System.out.println("    Invalid: type YES or NO.");
                }
        }

        private static String readChoice(String label, String[] options) {
                while (true) {
                        String v = readLine(label);
                        for (int i = 0; i < options.length; i++) {
                                if (options[i].equalsIgnoreCase(v)) {
                                        return options[i];
                                }
                        }
                        System.out.println("    Invalid: choose one of the listed values.");
                }
        }

        private static boolean confirmSave() {
                while (true) {
                        String v = readLine("[1] SAVE   [0] CANCEL");
                        if (v.equals("1"))
                                return true;
                        if (v.equals("0"))
                                return false;
                        System.out.println("    Invalid: enter 1 or 0.");
                }
        }

        // ---- shared field readers (labels carry the Java type, range and format) ----

        private static String readPartyId() {
                while (true) {
                        String id = readPattern("01 Party ID [String, unique, 4-30]",
                                        "[A-Za-z0-9_-]{4,30}", "4-30 letters, digits, - or _.");
                        if (!partyIdExists(id)) {
                                return id;
                        }
                        System.out.println("    Duplicate: that Party ID is already used.");
                }
        }

        private static String readEmail() {
                while (true) {
                        String v = readPattern("04 Email [String, valid email, max 120]",
                                        "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "not a valid email.");
                        if (v.length() <= 120) {
                                return v;
                        }
                        System.out.println("    Invalid: email is longer than 120 characters.");
                }
        }

        private static String readPhone() {
                return readPattern("05 Phone [String, 10-15 digits, optional +]",
                                "\\+?\\d{10,15}", "10-15 digits with an optional leading +.");
        }

        private static String readTin() {
                while (true) {
                        String tin = readPattern("07 TIN [String, unique, exactly 9 digits]",
                                        "\\d{9}", "TIN must be exactly 9 digits.");
                        if (!tinExists(tin)) {
                                return tin;
                        }
                        System.out.println("    Duplicate: that TIN already belongs to another taxpayer.");
                }
        }

        // ================= SEARCH / UNIQUENESS (loops over occupied entries only)
        // =================

        private static boolean partyIdExists(String id) {
                for (int i = 0; i < individualCount; i++) {
                        if (individuals[i].getId().equalsIgnoreCase(id))
                                return true;
                }
                for (int i = 0; i < corporateCount; i++) {
                        if (corporates[i].getId().equalsIgnoreCase(id))
                                return true;
                }
                for (int i = 0; i < officerCount; i++) {
                        if (officers[i].getId().equalsIgnoreCase(id))
                                return true;
                }
                return false;
        }

        // TIN must be unique across BOTH taxpayer types
        private static boolean tinExists(String tin) {
                return findTaxpayerByTin(tin) != null;
        }

        private static RegisteredTaxpayer findTaxpayerByTin(String tin) {
                for (int i = 0; i < individualCount; i++) {
                        if (individuals[i].getTin().equals(tin))
                                return individuals[i];
                }
                for (int i = 0; i < corporateCount; i++) {
                        if (corporates[i].getTin().equals(tin))
                                return corporates[i];
                }
                return null;
        }

        private static boolean nationalIdExists(String nid) {
                for (int i = 0; i < individualCount; i++) {
                        if (individuals[i].getNationalId().equals(nid))
                                return true;
                }
                return false;
        }

        private static boolean companyNoExists(String no) {
                for (int i = 0; i < corporateCount; i++) {
                        if (corporates[i].getCompanyRegistrationNo().equals(no))
                                return true;
                }
                return false;
        }

        private static boolean staffNumberExists(String no) {
                for (int i = 0; i < officerCount; i++) {
                        if (officers[i].getStaffNumber().equalsIgnoreCase(no))
                                return true;
                }
                return false;
        }

        private static boolean taxTypeIdExists(String id) {
                for (int i = 0; i < taxTypeCount; i++) {
                        if (taxTypes[i].getTaxTypeId().equalsIgnoreCase(id))
                                return true;
                }
                return false;
        }

        private static boolean taxCodeExists(String code) {
                for (int i = 0; i < taxTypeCount; i++) {
                        if (taxTypes[i].getTaxCode().equals(code))
                                return true;
                }
                return false;
        }

        // ================= OPTION 1: REGISTER INDIVIDUAL TAXPAYER =================
        private static void registerIndividual() throws ValidationException, DuplicateRecordException {
                if (individualCount >= individuals.length) {
                        System.out.println("Capacity reached (" + individuals.length
                                        + " individuals). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- REGISTER INDIVIDUAL TAXPAYER  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - PERSONAL DETAILS");
                String partyId = readPartyId();
                String firstName = readText("02 First name [String, 2-60]", 2, 60);
                String lastName = readText("03 Last name [String, 2-60]", 2, 60);
                String email = readEmail();
                String phone = readPhone();
                LocalDate dob = readDateNotFuture("06 Date of birth [LocalDate, YYYY-MM-DD]");

                System.out.println("STEP 2 OF 4 - TAX REGISTRATION");
                String tin = readTin();
                LocalDate regDate;
                while (true) {
                        regDate = readDateNotFuture("08 Registration date [LocalDate, YYYY-MM-DD, after birth date]");
                        if (dob.isBefore(regDate))
                                break;
                        System.out.println("    Invalid: registration date must be after the date of birth.");
                }
                String status = readChoice("09 Taxpayer status [String: ACTIVE / INACTIVE / SUSPENDED]",
                                new String[] { "ACTIVE", "INACTIVE", "SUSPENDED" });
                String district = readText("10 District [String, 2-60]", 2, 60);
                String sector = readText("11 Sector [String, 2-60]", 2, 60);
                String taxOffice = readText("12 Tax office [String, 2-60]", 2, 60);

                System.out.println("STEP 3 OF 4 - IDENTITY AND BUSINESS");
                String nationalId;
                while (true) {
                        nationalId = readPattern("13 National ID [String, unique, exactly 16 digits]",
                                        "\\d{16}", "National ID must be exactly 16 digits.");
                        if (!nationalIdExists(nationalId))
                                break;
                        System.out.println("    Duplicate: that National ID is already registered.");
                }
                String occupation = readText("14 Occupation [String, 2-60]", 2, 60);
                String businessName = readText("15 Business name [String, 2-60]", 2, 60);
                LocalDate startDate;
                while (true) {
                        startDate = readDateNotFuture(
                                        "16 Business start date [LocalDate, YYYY-MM-DD, before registration date]");
                        if (startDate.isBefore(regDate))
                                break;
                        System.out.println("    Invalid: start date must be before the registration date.");
                }
                BigDecimal turnover = readMoney("17 Annual turnover [BigDecimal, >= 0, 2 decimals]");
                boolean resident = readYesNo("18 Resident [boolean: YES / NO]");

                // The object is created only after every field is valid
                IndividualTaxpayer t = new IndividualTaxpayer(partyId, firstName, lastName, email,
                                phone, dob, tin, regDate, status, district, sector, taxOffice,
                                nationalId, occupation, businessName, startDate, turnover, resident);
                t.validate();

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 18 FIELDS");
                t.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return;
                }

                // Revalidate and re-check uniqueness at SAVE time
                t.validate();
                if (partyIdExists(t.getId())) {
                        throw new DuplicateRecordException("Party ID already exists.");
                }
                if (tinExists(t.getTin())) {
                        throw new DuplicateRecordException("TIN already exists.");
                }
                if (nationalIdExists(t.getNationalId())) {
                        throw new DuplicateRecordException("National ID already exists.");
                }
                individuals[individualCount] = t;
                individualCount++;
                System.out.println("Individual taxpayer saved. Total individuals: " + individualCount);
        }

        // ================= OPTION 2: REGISTER CORPORATE TAXPAYER =================
        private static void registerCorporate() throws ValidationException, DuplicateRecordException {
                if (corporateCount >= corporates.length) {
                        System.out.println("Capacity reached (" + corporates.length
                                        + " corporates). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- REGISTER CORPORATE TAXPAYER  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - CONTACT PERSON DETAILS");
                String partyId = readPartyId();
                String firstName = readText("02 First name [String, 2-60]", 2, 60);
                String lastName = readText("03 Last name [String, 2-60]", 2, 60);
                String email = readEmail();
                String phone = readPhone();
                LocalDate dob = readDateNotFuture("06 Date of birth [LocalDate, YYYY-MM-DD]");

                System.out.println("STEP 2 OF 4 - TAX REGISTRATION");
                String tin = readTin();
                LocalDate regDate;
                while (true) {
                        regDate = readDateNotFuture("08 Registration date [LocalDate, YYYY-MM-DD, after birth date]");
                        if (dob.isBefore(regDate))
                                break;
                        System.out.println("    Invalid: registration date must be after the date of birth.");
                }
                String status = readChoice("09 Taxpayer status [String: ACTIVE / INACTIVE / SUSPENDED]",
                                new String[] { "ACTIVE", "INACTIVE", "SUSPENDED" });
                String district = readText("10 District [String, 2-60]", 2, 60);
                String sector = readText("11 Sector [String, 2-60]", 2, 60);
                String taxOffice = readText("12 Tax office [String, 2-60]", 2, 60);

                System.out.println("STEP 3 OF 4 - COMPANY DETAILS");
                String companyNo;
                while (true) {
                        companyNo = readPattern("13 Company reg. no [String, unique, 5-30 of A-Z 0-9 -]",
                                        "[A-Z0-9-]{5,30}", "5-30 UPPERCASE letters, digits or hyphens.");
                        if (!companyNoExists(companyNo))
                                break;
                        System.out.println("    Duplicate: that company number is already registered.");
                }
                String legalName = readText("14 Legal name [String, 3-120]", 3, 120);
                LocalDate incorporation;
                while (true) {
                        incorporation = readDateNotFuture(
                                        "15 Incorporation date [LocalDate, YYYY-MM-DD, before registration date]");
                        if (incorporation.isBefore(regDate))
                                break;
                        System.out.println("    Invalid: incorporation date must be before the registration date.");
                }
                String sectorOfBusiness = readText("16 Business sector [String, 2-60]", 2, 60);
                String representative = readText("17 Authorized representative [String, 2-60]", 2, 60);
                BigDecimal turnover = readMoney("18 Annual turnover [BigDecimal, >= 0, 2 decimals]");

                CorporateTaxpayer c = new CorporateTaxpayer(partyId, firstName, lastName, email,
                                phone, dob, tin, regDate, status, district, sector, taxOffice,
                                companyNo, legalName, incorporation, sectorOfBusiness, representative,
                                turnover);
                c.validate();

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 18 FIELDS");
                c.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return;
                }

                c.validate();
                if (partyIdExists(c.getId())) {
                        throw new DuplicateRecordException("Party ID already exists.");
                }
                if (tinExists(c.getTin())) {
                        throw new DuplicateRecordException("TIN already exists.");
                }
                if (companyNoExists(c.getCompanyRegistrationNo())) {
                        throw new DuplicateRecordException("Company registration number already exists.");
                }
                corporates[corporateCount] = c;
                corporateCount++;
                System.out.println("Corporate taxpayer saved. Total corporates: " + corporateCount);
        }

        // ================= OPTION 3: REGISTER REVENUE OFFICER =================
        private static void registerOfficer() throws ValidationException, DuplicateRecordException {
                if (officerCount >= officers.length) {
                        System.out.println("Capacity reached (" + officers.length
                                        + " officers). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- REGISTER REVENUE OFFICER  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - PERSONAL DETAILS");
                String partyId = readPartyId();
                String firstName = readText("02 First name [String, 2-60]", 2, 60);
                String lastName = readText("03 Last name [String, 2-60]", 2, 60);
                String email = readEmail();
                String phone = readPhone();
                LocalDate dob = readDateNotFuture("06 Date of birth [LocalDate, YYYY-MM-DD]");

                System.out.println("STEP 2 OF 4 - STAFF DETAILS");
                String staffNo;
                while (true) {
                        staffNo = readPattern("07 Staff number [String, unique, 3-20 letters/digits/-]",
                                        "[A-Za-z0-9-]{3,20}", "3-20 letters, digits or hyphens.");
                        if (!staffNumberExists(staffNo))
                                break;
                        System.out.println("    Duplicate: that staff number is already used.");
                }
                String department = readText("08 Department [String, 2-60]", 2, 60);
                String role = readText("09 Role [String, 2-60]", 2, 60);
                LocalDate hireDate;
                while (true) {
                        hireDate = readDateNotFuture("10 Hire date [LocalDate, YYYY-MM-DD, after birth date]");
                        if (hireDate.isAfter(dob))
                                break;
                        System.out.println("    Invalid: hire date must be after the date of birth.");
                }

                System.out.println("STEP 3 OF 4 - AUTHORITY");
                BigDecimal limit = readMoney("11 Approval limit [BigDecimal, >= 0, 2 decimals]");
                boolean active = readYesNo("12 Active [boolean: YES / NO]");

                RevenueOfficer o = new RevenueOfficer(partyId, firstName, lastName, email, phone,
                                dob, staffNo, department, role, hireDate, limit, active);
                o.validate();

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 12 FIELDS");
                o.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return;
                }

                o.validate();
                if (partyIdExists(o.getId())) {
                        throw new DuplicateRecordException("Party ID already exists.");
                }
                if (staffNumberExists(o.getStaffNumber())) {
                        throw new DuplicateRecordException("Staff number already exists.");
                }
                officers[officerCount] = o;
                officerCount++;
                System.out.println("Revenue officer saved. Total officers: " + officerCount);
        }

        // ================= OPTION 4: REGISTER TAX TYPE =================
        private static void registerTaxType() throws ValidationException, DuplicateRecordException {
                if (taxTypeCount >= taxTypes.length) {
                        System.out.println("Capacity reached (" + taxTypes.length
                                        + " tax types). Existing data is safe.");
                        return;
                }
                System.out.println("\n+------- REGISTER TAX TYPE  (type CANCEL to stop) -------+");

                System.out.println("STEP 1 OF 4 - IDENTIFICATION");
                String id;
                while (true) {
                        id = readPattern("01 Tax type ID [String, unique, 3-20 letters/digits/-]",
                                        "[A-Za-z0-9-]{3,20}", "3-20 letters, digits or hyphens.");
                        if (!taxTypeIdExists(id))
                                break;
                        System.out.println("    Duplicate: that tax type ID is already used.");
                }
                String code;
                while (true) {
                        code = readPattern("02 Tax code [String, unique, 2-10 of A-Z 0-9 -]",
                                        "[A-Z0-9-]{2,10}", "2-10 UPPERCASE letters, digits or hyphens.");
                        if (!taxCodeExists(code))
                                break;
                        System.out.println("    Duplicate: that tax code is already used.");
                }

                System.out.println("STEP 2 OF 4 - TAX DETAILS");
                String name = readText("03 Tax name [String, 3-60]", 3, 60);
                BigDecimal rate = readRate("04 Rate % [BigDecimal, 0-100, 2 decimals]");
                String frequency = readChoice("05 Filing frequency [String: MONTHLY / QUARTERLY / ANNUAL]",
                                new String[] { "MONTHLY", "QUARTERLY", "ANNUAL" });

                System.out.println("STEP 3 OF 4 - STATUS");
                boolean active = readYesNo("06 Active [boolean: YES / NO]");

                TaxType tt = new TaxType(id, code, name, rate, frequency, active);
                tt.validate();

                System.out.println("\nSTEP 4 OF 4 - REVIEW ALL 6 FIELDS");
                tt.printDetails();
                if (!confirmSave()) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return;
                }

                tt.validate();
                if (taxTypeIdExists(tt.getTaxTypeId())) {
                        throw new DuplicateRecordException("Tax type ID already exists.");
                }
                if (taxCodeExists(tt.getTaxCode())) {
                        throw new DuplicateRecordException("Tax code already exists.");
                }
                taxTypes[taxTypeCount] = tt;
                taxTypeCount++;
                System.out.println("Tax type saved. Total tax types: " + taxTypeCount);
        }

        // ================= OPTION 9: VIEW ALL RECORDS (grows in the next layers)
        // =================
        private static void viewAllRecords() {
                System.out.println("\n--- INDIVIDUAL TAXPAYERS (" + individualCount + ") ---");
                for (int i = 0; i < individualCount; i++) {
                        System.out.println((i + 1) + ". " + individuals[i].getSummary());
                }
                System.out.println("--- CORPORATE TAXPAYERS (" + corporateCount + ") ---");
                for (int i = 0; i < corporateCount; i++) {
                        System.out.println((i + 1) + ". " + corporates[i].getSummary());
                }
                System.out.println("--- REVENUE OFFICERS (" + officerCount + ") ---");
                for (int i = 0; i < officerCount; i++) {
                        System.out.println((i + 1) + ". " + officers[i].getSummary());
                }
                System.out.println("--- TAX TYPES (" + taxTypeCount + ") ---");
                for (int i = 0; i < taxTypeCount; i++) {
                        System.out.println((i + 1) + ". " + taxTypes[i].getSummary());
                }
                System.out.println("--- TAX OBLIGATIONS (" + obligationCount + ") ---");
                for (int i = 0; i < obligationCount; i++) {
                        System.out.println((i + 1) + ". " + obligations[i].getSummary());
                }
                System.out.println("--- TAX DECLARATIONS (" + declarationCount + ") ---");
                for (int i = 0; i < declarationCount; i++) {
                        System.out.println((i + 1) + ". " + declarations[i].getSummary());
                }
                System.out.println("--- TAX ASSESSMENTS (" + assessmentCount + ") ---");
                for (int i = 0; i < assessmentCount; i++) {
                        System.out.println((i + 1) + ". " + assessments[i].getSummary());
                }
                System.out.println("--- TAX PAYMENTS (" + paymentCount + ") ---");
                for (int i = 0; i < paymentCount; i++) {
                        System.out.println((i + 1) + ". " + payments[i].getSummary());
                }
                System.out.println("--- PENALTIES (" + penaltyCount + ") ---");
                for (int i = 0; i < penaltyCount; i++) {
                        System.out.println((i + 1) + ". " + penalties[i].getSummary());
                }
                System.out.println("--- TAX REFUNDS (" + refundCount + ") ---");
                for (int i = 0; i < refundCount; i++) {
                        System.out.println((i + 1) + ". " + refunds[i].getSummary());
                }
                System.out.println("--- TAX CLEARANCES (" + clearanceCount + ") ---");
                for (int i = 0; i < clearanceCount; i++) {
                        System.out.println((i + 1) + ". " + clearances[i].getSummary());
                }
        }
}