package com.mycompany.librarymanagementsystemblp.model;

public class BorrowRecord {

    private int detailId;
    private int borrowId;
    private int memberId;
    private String memberName;   // filled in by a JOIN with MEMBERS
    private int bookId;
    private String bookTitle;    // filled in by a JOIN with BOOKS
    private String borrowDate;
    private String dueDate;
    private String returnDate;   // null until the book is returned
    private String status;       

    public BorrowRecord() {
    }

    public BorrowRecord(int detailId, int borrowId, int memberId, String memberName,
                        int bookId, String bookTitle, String borrowDate,
                        String dueDate, String returnDate, String status) {
        this.detailId = detailId;
        this.borrowId = borrowId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    public int getDetailId() { return detailId; }
    public void setDetailId(int detailId) { this.detailId = detailId; }

    public int getBorrowId() { return borrowId; }
    public void setBorrowId(int borrowId) { this.borrowId = borrowId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBorrowDate() { return borrowDate; }
    public void setBorrowDate(String borrowDate) { this.borrowDate = borrowDate; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}