package library.model;

public class Book {
    private int bookId;
    private String title;
    private String author;
    private String isbn;
    private BookStatus status;

    public Book(int bookId, String title, String author, String isbn) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.status = BookStatus.AVAILABLE; // 預設可借
    }

    public boolean isAvailable() {
        return this.status == BookStatus.AVAILABLE;
    }

    public void updateStatus(BookStatus status) {
        this.status = status;
    }

    // Getters
    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public BookStatus getStatus() { return status; }
}