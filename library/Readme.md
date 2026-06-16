# library-restaurant-system

- Claude給我的架構但我懶得動了
- 要開Edit才看的到排版
  - DatabaseManager   →   負責開門（連線）
  - BookRepository    →   負責去拿東西（SQL）
  - BookService       →   負責決定要拿什麼（邏輯）

***自動內建管理員帳號：[ ADMIN001 ]，密碼：[ admin123 ]***
# LibraryApp
1. Database
   - [DatabaseManager.java](src/database/DatabaseManager.java)
     - initializeDatabase
   - [DataLoader.java](src/database/DataLoader.java)
     - loadInitialData
     - isDataExists
     - importUsers
     - importBooks
     - importBorrowRecords
     - convertRelativeTime
     - joinListOrString
2. model
   - [Book.java](src/model/Book.java)
   - [BorrowRecord.java](src/model/BorrowRecord.java)
   - [Fine.java](src/model/Fine.java)
   - [User.java](src/model/User.java)
3. repository
   - [BookRepository.java](src/repository/BookRepository.java)
   - [BorrowRecordRepository.java](src/repository/BorrowRecordRepository.java)
   - [FineRepository.java](src/repository/FineRepository.java)
   - [UserRepository.java](src/repository/UserRepository.java)
4. sevice
   - [BookService.java](src/service/BookService.java)
     - getAllBooks ***[Y]***
     - addBook ***[Y]*** 
     - removeBook ***[Y]***
   - [BorrowService.java](src/service/BorrowService.java)
     - borrowBook ***[Y]***
     - returnBook ***[Y]***
     - getUserBorrowHistory ***[Y]***
     - getAllBorrowRecords ***[Y]***
   - [FineService.java](src/service/FineService.java)
     - createOrUpdateFine
     - payFine ***[Y]***
     - getUnpaidFinesByUser ***[Y]***
     - getTotalUnpaidAmount ***[Y]***
     - getAllUnpaidFines ***[Y]***
   - [ReportService.java](src/service/ReportService.java)
     - getBookSubjectPopularity ***[Y]***
     - getTop5BorrowedBooks ***[Y]***
     - getLibraryFineSummary 
     - getLibraryGeneralKPIs ***[Y]***
   - [UserService.java](src/service/UserService.java)
     - login ***[Y]***
     - registerUser ***[Y]***
     - updateUserStatus ***[Y]***
     - updateUserRole ***[give up]***
     - getUserById ***[Y]***
     - getUserByStudentNo ***[Y]***
     - getAllStudents ***[Y]***
5. gui
   - [AdminDashboardView.java](src/gui/AdminDashboardView.java)
   - [AppStyle.java](src/gui/AppStyle.java)
   - [LoginView.java](src/gui/LoginView.java)
   - [RegisterView.java](src/gui/RegisterView.java)
   - [UserDashboardView.java](src/gui/UserDashboardView.java)

### problem 
    罰金機制有問題，應更改成過期就開始有罰金(fix)
    User介面借還書不會更新(fix)
    Admin新增功能->升級管理員(give up;介面問題)