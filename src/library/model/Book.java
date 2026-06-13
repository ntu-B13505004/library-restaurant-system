package library.model;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private int bookId;
    private String title;
    private String authors;       // 配合新 DB：改為複數型欄位
    private String subjects;      // 主題
    private String publisher;     // 出版者
    private String publishYear;   // 出版日期/年
    private String edition;       // 版本
    private String formatDesc;    // 格式
    private String source;        // 資料來源
    private String note;          // 附註
    private BookStatus status;
    private List<String> isbns;   // 配合新 DB：一對多拆表，改用 List 儲存複數 ISBN

    // ✨ 完整欄位建構子：供新版 BookRepository 使用
    public Book(int bookId, String title, String authors, String subjects, String publisher,
                String publishYear, String edition, String formatDesc, String source, String note) {
        this.bookId = bookId;
        this.title = title;
        this.authors = authors;
        this.subjects = subjects;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.edition = edition;
        this.formatDesc = formatDesc;
        this.source = source;
        this.note = note;
        this.status = BookStatus.AVAILABLE; // 預設可借
        this.isbns = new ArrayList<>();     // 初始化 List 避免 NullPointerException
    }

    public boolean isAvailable() {
        return this.status == BookStatus.AVAILABLE;
    }

    // 填滿原本空著的 Setter
    public void setStatus(BookStatus status) {
        this.status = status;
    }

    public void setIsbns(List<String> isbns) {
        this.isbns = isbns;
    }

    // 實用方法：方便一筆一筆加入自 book_isbns 表撈出來的資料
    public void addIsbn(String isbn) {
        if (this.isbns == null) {
            this.isbns = new ArrayList<>();
        }
        this.isbns.add(isbn);
    }

    // Getters
    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthors() { return authors; } // 原 getAuthor() 改為複數
    public List<String> getIsbns() { return isbns; }
    public BookStatus getStatus() { return status; }
    public String getSubjects() { return subjects; }
    public String getPublisher() { return publisher; }
    public String getPublishYear() { return publishYear; }
    public String getEdition() { return edition; }
    public String getFormatDesc() { return formatDesc; }
    public String getSource() { return source; }
    public String getNote() { return note; }
}