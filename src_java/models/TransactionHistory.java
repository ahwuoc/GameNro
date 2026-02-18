package models;

import lombok.Getter;

@Getter
public class TransactionHistory {
    private String postingDate;

    private String transactionDate;

    private String accountNo;
    private String creditAmount;
    private String debitAmount;
    private String currency;
    private String description;
    private String availableBalance;
    private String beneficiaryAccount;
    private String refNo;
    private String benAccountName;
    private String bankName;
    private String benAccountNo;
    private String dueDate;
    private String docId;
    private String transactionType;

    // Constructors, getters, and setters

    // You can generate these using your IDE or manually write them.
}
