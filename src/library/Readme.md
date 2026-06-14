# library-restaurant-system

- Claude給我的架構但我懶得動了
- 要開Edit才看的到排版
  - DatabaseManager   →   負責開門（連線）
  - BookRepository    →   負責去拿東西（SQL）
  - BookService       →   負責決定要拿什麼（邏輯）

***自動內建管理員帳號：[ ADMIN001 ]，密碼：[ admin123 ]***
# LibraryApp
1. Database
   - [DatabaseManager.java](database/DatabaseManager.java)
     - initializeDatabase
   - [DataLoader.java](database/DataLoader.java)
     - loadInitialData
     - isDataExists
     - importUsers
     - importBooks
     - importBorrowRecords
     - convertRelativeTime
     - joinListOrString
2. model
   - [Book.java](model/Book.java)
   - [BorrowRecord.java](model/BorrowRecord.java)
   - [Fine.java](model/Fine.java)
   - [User.java](model/User.java)
3. repository
   - [BookRepository.java](repository/BookRepository.java)
   - [BorrowRecordRepository.java](repository/BorrowRecordRepository.java)
   - [FineRepository.java](repository/FineRepository.java)
   - [UserRepository.java](repository/UserRepository.java)
4. sevice
   - [BookService.java](service/BookService.java)
     - getAllBooks ***[Y]***
     - addBook ***[Y]*** 
     - removeBook ***[Y]***
   - [BorrowService.java](service/BorrowService.java)
     - borrowBook ***[Y]***
     - returnBook ***[Y]***
     - getUserBorrowHistory
     - getAllBorrowRecords
   - [FineService.java](service/FineService.java)
     - createOrUpdateFine
     - payFine
     - getUnpaidFinesByUser
     - getTotalUnpaidAmount
     - getAllUnpaidFines
   - [ReportService.java](service/ReportService.java)
     - getBookSubjectPopularity
     - getTop5BorrowedBooks
     - getLibraryFineSummary
     - getLibraryGeneralKPIs
   - [UserService.java](service/UserService.java)
     - login ***[Y]***
     - registerUser ***[Y]***
     - updateUserStatus ***[Y]***
     - updateUserRole 
     - getUserById
     - getUserByStudentNo
     - getAllStudents ***[Y]***
5. gui
   - [AdminDashboardView.java](gui/AdminDashboardView.java)
   - [AppStyle.java](gui/AppStyle.java)
   - [LoginView.java](gui/LoginView.java)
   - [RegisterView.java](gui/RegisterView.java)
   - [UserDashboardView.java](gui/UserDashboardView.java)

### problem 
    罰金機制有問題，應更改成過期就開始有罰金
    User介面借還書不會更新
    Admin新增功能