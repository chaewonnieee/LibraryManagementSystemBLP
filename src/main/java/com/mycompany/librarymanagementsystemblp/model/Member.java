package com.mycompany.librarymanagementsystemblp.model;

public class Member {

    private int memberId;
    private String firstName;
    private String lastName;
    private String contactNo;
    private String email;
    private String address;
    private String dateRegistered;
    private String status;

    public Member() {
    }

    public Member(int memberId, String firstName, String lastName, String contactNo,
                  String email, String address, String dateRegistered, String status) {
        this.memberId = memberId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.contactNo = contactNo;
        this.email = email;
        this.address = address;
        this.dateRegistered = dateRegistered;
        this.status = status;
    }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDateRegistered() { return dateRegistered; }
    public void setDateRegistered(String dateRegistered) { this.dateRegistered = dateRegistered; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Shows "Last, First" in dropdowns (used later by the Borrowing screen)
    @Override
    public String toString() {
        return lastName + ", " + firstName;
    }
}