package com.mycompany.librarymanagementsystemblp.model;

import java.math.BigDecimal;

public class Fine {

    private int fineId;
    private int detailId;
    private String memberName;
    private String bookTitle;
    private BigDecimal amount;
    private String fineDate;
    private String status;

    public Fine() {
    }

    public Fine(int fineId, int detailId, String memberName, String bookTitle,
                BigDecimal amount, String fineDate, String status) {
        this.fineId = fineId;
        this.detailId = detailId;
        this.memberName = memberName;
        this.bookTitle = bookTitle;
        this.amount = amount;
        this.fineDate = fineDate;
        this.status = status;
    }

    public int getFineId() { return fineId; }
    public void setFineId(int fineId) { this.fineId = fineId; }

    public int getDetailId() { return detailId; }
    public void setDetailId(int detailId) { this.detailId = detailId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getFineDate() { return fineDate; }
    public void setFineDate(String fineDate) { this.fineDate = fineDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
