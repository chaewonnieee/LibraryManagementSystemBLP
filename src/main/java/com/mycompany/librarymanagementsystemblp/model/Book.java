package com.mycompany.librarymanagementsystemblp.model;

public class Book {

    private int bookId;
    private int categoryId;
    private String categoryName;  
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private int totalCopies;
    private int availableCopies;
    private String status;

    public Book() {
    }

    public Book(int bookId, int categoryId, String categoryName, String isbn,
                String title, String author, String publisher,
                int totalCopies, int availableCopies, String status) {
        this.bookId = bookId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.status = status;
    }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    @Override
    public String toString() {
        return title + " (" + isbn + ")";
    }
}