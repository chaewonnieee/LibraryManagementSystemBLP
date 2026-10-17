package com.mycompany.librarymanagementsystemblp.model;

public class Reservation {

    private int reservationId;
    private int memberId;
    private String memberName;      // filled in by a JOIN with MEMBERS
    private int bookId;
    private String bookTitle;       // filled in by a JOIN with BOOKS
    private String reservationDate;
    private String status;          // Pending, Fulfilled, or Cancelled

    public Reservation() {
    }

    public Reservation(int reservationId, int memberId, String memberName, int bookId,
                       String bookTitle, String reservationDate, String status) {
        this.reservationId = reservationId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getReservationDate() { return reservationDate; }
    public void setReservationDate(String reservationDate) { this.reservationDate = reservationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
