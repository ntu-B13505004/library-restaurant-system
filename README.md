# library-restaurant-system

- Claude給我的架構但我懶得動了

  - DatabaseManager   →   負責開門（連線）
  - BookRepository    →   負責去拿東西（SQL）
  - BookService       →   負責決定要拿什麼（邏輯）

      library-restaurant-system/
      │
      ├── database/
      │   ├── DatabaseManager.java
      │   └── DataLoader.java
      │
      ├── library/
      │   ├── model/
      │   │   ├── Book.java
      │   │   ├── BookStatus.java
      │   │   ├── BorrowRecord.java
      │   │   ├── Fine.java
      │   │   ├── User.java
      │   │   └── UserRole.java
      │   ├── repository/
      │   │   ├── BookRepository.java
      │   │   ├── UserRepository.java
      │   │   ├── BorrowRepository.java
      │   │   └── FineRepository.java
      │   ├── service/
      │   │   ├── BookService.java
      │   │   ├── UserService.java
      │   │   ├── BorrowService.java
      │   │   ├── FineService.java
      │   │   └── ReportService.java
      │   └── ui/
      │
      ├── restaurant/
      │   ├── model/
      │   ├── repository/
      │   ├── service/
      │   └── ui/
      │
      ├── Books.json
      ├── Users.json
      ├── Borrow_records.json
      ├── library.db          ← 程式自動產生＆修改
      └── README.md
