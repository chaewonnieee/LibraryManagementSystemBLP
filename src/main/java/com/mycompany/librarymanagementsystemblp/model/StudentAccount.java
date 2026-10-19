package com.mycompany.librarymanagementsystemblp.model;

public class StudentAccount {

    private int accountId;
    private int memberId;
    private String username;
    private String status;     // Active or Inactive

    public StudentAccount() {
    }

    public StudentAccount(int accountId, int memberId, String username, String status) {
        this.accountId = accountId;
        this.memberId = memberId;
        this.username = username;
        this.status = status;
    }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
