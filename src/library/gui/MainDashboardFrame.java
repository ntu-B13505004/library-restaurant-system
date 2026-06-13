package library.gui;

import library.model.*;
import library.service.BorrowService;
import library.repository.UserRepository;
import library.repository.BookRepository;
import library.repository.BorrowRecordRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class MainDashboardFrame extends JFrame {

    private final User currentUser;
    private final BorrowService borrowService = new BorrowService();
    private final UserRepository userRepository = new UserRepository();
    private final BookRepository bookRepository = new BookRepository();
    private final BorrowRecordRepository borrowRecordRepository = new BorrowRecordRepository();

    // 介面元件
    private JTabbedPane tabbedPane;

    // 學生專區元件
    private JTextField txtBookId, txtBorrowDays, txtReturnBookId, txtSearchKeyword;
    private JTable tableBooks, tableHistory;
    private DefaultTableModel modelBooks, modelHistory;

    // 管理員專區元件
    private JTextField txtTargetUserId, txtNewBookTitle, txtNewBookAuthor, txtNewBookPublisher;
    private JComboBox<UserStatus> cbUserStatus;
    private JTable tableAdminRecords;
    private DefaultTableModel modelAdminRecords;

    public MainDashboardFrame(User user) {
        this.currentUser = user;

        setTitle("圖書館管理系統 - 主主控台 [當前使用者: " + user.getName() + " (" + user.getRoleLevel() + ")]");
        setSize(850, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        // 建立三大分頁面板
        JPanel bookSearchPanel = createBookSearchPanel();
        JPanel studentPanel = createStudentPanel();
        JPanel adminPanel = createAdminPanel();

        tabbedPane.addTab("🔍 圖書查詢與瀏覽", bookSearchPanel);
        tabbedPane.addTab("📖 個人借還書專區", studentPanel);
        tabbedPane.addTab("🛠️ 管理員控制後台", adminPanel);

        // 權限分流：非管理員則停用管理員分頁
        if (currentUser.getRoleLevel() != UserRole.ADMIN) {
            tabbedPane.setEnabledAt(2, false);
            tabbedPane.setToolTipTextAt(2, "僅限管理員帳號存取");
        }

        add(tabbedPane);

        // 初始載入資料
        refreshBookTable("");
        refreshHistoryTable();
        if (currentUser.getRoleLevel() == UserRole.ADMIN) {
            refreshAdminRecordsTable();
        }
    }

    /**
     * 🔍 1. 圖書查詢與瀏覽面板
     */
    private JPanel createBookSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 上方搜尋列
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtSearchKeyword = new JTextField(20);
        JButton btnSearch = new JButton("搜尋書籍");
        JButton btnReset = new JButton("顯示全部");
        searchBar.add(new JLabel("關鍵字 (書名/作者/主題):"));
        searchBar.add(txtSearchKeyword);
        searchBar.add(btnSearch);
        searchBar.add(btnReset);
        panel.add(searchBar, BorderLayout.NORTH);

        // 中央表格
        String[] columns = {"書籍 ID", "書名", "作者", "出版社", "出版年", "狀態"};
        modelBooks = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; } // 表格不可雙擊修改
        };
        tableBooks = new JTable(modelBooks);
        panel.add(new JScrollPane(tableBooks), BorderLayout.CENTER);

        // 事件監聽
        btnSearch.addActionListener(e -> refreshBookTable(txtSearchKeyword.getText().trim()));
        btnReset.addActionListener(e -> { txtSearchKeyword.setText(""); refreshBookTable(""); });

        return panel;
    }

    /**
     * 📖 2. 個人借還書與歷史紀錄面板
     */
    private JPanel createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 上方：操作區塊
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        // 借書區
        JPanel borrowBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
        borrowBox.setBorder(BorderFactory.createTitledBorder("辦理借書交易"));
        txtBookId = new JTextField(4);
        txtBorrowDays = new JTextField(4);
        txtBorrowDays.setText("7");
        JButton btnBorrow = new JButton("確認借閱");
        borrowBox.add(new JLabel("書籍 ID:"));
        borrowBox.add(txtBookId);
        borrowBox.add(new JLabel("天數:"));
        borrowBox.add(txtBorrowDays);
        borrowBox.add(btnBorrow);

        // 還書區
        JPanel returnBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
        returnBox.setBorder(BorderFactory.createTitledBorder("辦理還書結算"));
        txtReturnBookId = new JTextField(4);
        JButton btnReturn = new JButton("確認歸還");
        returnBox.add(new JLabel("書籍 ID:"));
        returnBox.add(txtReturnBookId);
        returnBox.add(btnReturn);

        topPanel.add(borrowBox);
        topPanel.add(returnBox);
        panel.add(topPanel, BorderLayout.NORTH);

        // 下方：個人借閱歷史表格
        JPanel historyBox = new JPanel(new BorderLayout());
        historyBox.setBorder(BorderFactory.createTitledBorder("您的歷史借閱紀錄與罰款審查"));

        String[] columns = {"紀錄 ID", "書名", "借閱日期", "應還日期", "歸還日期", "租借天數"};
        modelHistory = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableHistory = new JTable(modelHistory);
        historyBox.add(new JScrollPane(tableHistory), BorderLayout.CENTER);
        panel.add(historyBox, BorderLayout.CENTER);

        // 事件監聽
        btnBorrow.addActionListener(this::performBorrow);
        btnReturn.addActionListener(this::performReturn);

        return panel;
    }

    /**
     * 🛠️ 3. 管理員控制後台面板
     */
    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 左側：管理操作面板
        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        leftPanel.setPreferredSize(new Dimension(300, 0));

        // 操作 1：停權與帳號維護
        JPanel userBox = new JPanel(new GridLayout(4, 1, 5, 5));
        userBox.setBorder(BorderFactory.createTitledBorder("學生帳號維護"));
        txtTargetUserId = new JTextField();
        cbUserStatus = new JComboBox<>(UserStatus.values());
        JButton btnUpdateStatus = new JButton("執行狀態變更");
        userBox.add(new JLabel("目標學生 User ID:"));
        userBox.add(txtTargetUserId);
        userBox.add(cbUserStatus);
        userBox.add(btnUpdateStatus);

        // 操作 2：上架新書
        JPanel bookBox = new JPanel(new GridLayout(7, 1, 2, 2));
        bookBox.setBorder(BorderFactory.createTitledBorder("採購新書上架"));
        txtNewBookTitle = new JTextField();
        txtNewBookAuthor = new JTextField();
        txtNewBookPublisher = new JTextField();
        JButton btnAddBook = new JButton("確認新增書籍");
        bookBox.add(new JLabel("書名 (*):"));
        bookBox.add(txtNewBookTitle);
        bookBox.add(new JLabel("作者:"));
        bookBox.add(txtNewBookAuthor);
        bookBox.add(new JLabel("出版社:"));
        bookBox.add(txtNewBookPublisher);
        bookBox.add(btnAddBook);

        leftPanel.add(userBox);
        leftPanel.add(bookBox);
        panel.add(leftPanel, BorderLayout.WEST);

        // 右側：全系統借閱流水帳
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("全系統借閱流水帳審查 (多表關聯數據)"));

        String[] columns = {"紀錄 ID", "借閱人", "書名", "借出時間", "歸還時間"};
        modelAdminRecords = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableAdminRecords = new JTable(modelAdminRecords);
        rightPanel.add(new JScrollPane(tableAdminRecords), BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.CENTER);

        // 事件監聽
        btnUpdateStatus.addActionListener(this::performUserUpdate);
        btnAddBook.addActionListener(this::performAddBook);

        return panel;
    }

    // ==========================================
    // 數據刷新與同步邏輯 (對接各個 Repository)
    // ==========================================

    private void refreshBookTable(String keyword) {
        modelBooks.setRowCount(0);
        // 💡 提示：此處假設您的 BookRepository 已有 findAll() 或包含關鍵字搜尋的方法。
        // 如果沒有，可暫時讀取所有書或串接您現有的查詢邏輯。
        try (java.sql.Connection conn = library.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM books WHERE title LIKE ? OR authors LIKE ? OR subjects LIKE ?")) {

            String queryStr = "%" + keyword + "%";
            pstmt.setString(1, queryStr);
            pstmt.setString(2, queryStr);
            pstmt.setString(3, queryStr);

            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    modelBooks.addRow(new Object[]{
                            rs.getInt("book_id"),
                            rs.getString("title"),
                            rs.getString("authors"),
                            rs.getString("publisher"),
                            rs.getString("publish_year"),
                            rs.getString("status")
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshHistoryTable() {
        modelHistory.setRowCount(0);
        List<BorrowRecord> list = borrowRecordRepository.findAllByUserId(currentUser.getUserId());
        for (BorrowRecord r : list) {
            modelHistory.addRow(new Object[]{
                    r.getRecordId(),
                    r.getBook() != null ? r.getBook().getTitle() : "未知書籍",
                    r.getBorrowDate() != null ? r.getBorrowDate().toString().replace("T", " ") : "-",
                    r.getDueDate() != null ? r.getDueDate().toString().replace("T", " ") : "-",
                    r.getReturnDate() != null ? r.getReturnDate().toString().replace("T", " ") : "尚未歸還",
                    r.getBorrowDays()
            });
        }
    }

    private void refreshAdminRecordsTable() {
        modelAdminRecords.setRowCount(0);
        // 管理員直接撈取資料庫內前 50 筆最新借閱流水帳
        try (java.sql.Connection conn = library.database.DatabaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(
                     "SELECT r.record_id, u.name as uname, b.title as btitle, r.borrow_date, r.return_date " +
                             "FROM borrow_records r " +
                             "LEFT JOIN users u ON r.user_id = u.user_id " +
                             "LEFT JOIN books b ON r.book_id = b.book_id " +
                             "ORDER BY r.record_id DESC LIMIT 50")) {

            while (rs.next()) {
                modelAdminRecords.addRow(new Object[]{
                        rs.getInt("record_id"),
                        rs.getString("uname"),
                        rs.getString("btitle"),
                        rs.getString("borrow_date"),
                        rs.getString("return_date") != null ? rs.getString("return_date") : "借閱中"
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // 按鈕動作執行邏輯
    // ==========================================

    private void performBorrow(ActionEvent e) {
        try {
            int bookId = Integer.parseInt(txtBookId.getText().trim());
            int days = Integer.parseInt(txtBorrowDays.getText().trim());

            String result = borrowService.borrowBook(currentUser.getUserId(), bookId, days);

            if ("SUCCESS".equals(result)) {
                JOptionPane.showMessageDialog(this, "🎉 借書交易成功！", "系統提示", JOptionPane.INFORMATION_MESSAGE);
                txtBookId.setText("");
                // 連動重新整理表格
                refreshBookTable("");
                refreshHistoryTable();
            } else {
                JOptionPane.showMessageDialog(this, result, "借閱受阻", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "⚠️ 請輸入正確的數字格式！", "格式錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performReturn(ActionEvent e) {
        try {
            int bookId = Integer.parseInt(txtReturnBookId.getText().trim());
            String result = borrowService.returnBook(bookId);

            if ("SUCCESS".equals(result)) {
                JOptionPane.showMessageDialog(this, "🎉 還書成功！書籍已重新上架。", "系統提示", JOptionPane.INFORMATION_MESSAGE);
                txtReturnBookId.setText("");
                refreshBookTable("");
                refreshHistoryTable();
            } else if (result.startsWith("SUCCESS_WITH_FINE:")) {
                String fine = result.split(":")[1];
                JOptionPane.showMessageDialog(this, "⚠️ 歸還成功（已逾期）！\n系統已自動拋轉罰單，金額: " + fine + " 元。", "逾期警告", JOptionPane.WARNING_MESSAGE);
                txtReturnBookId.setText("");
                refreshBookTable("");
                refreshHistoryTable();
            } else {
                JOptionPane.showMessageDialog(this, result, "還書失敗", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "⚠️ 請輸入正確的書籍 ID！", "格式錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performUserUpdate(ActionEvent e) {
        try {
            int targetId = Integer.parseInt(txtTargetUserId.getText().trim());
            UserStatus selectedStatus = (UserStatus) cbUserStatus.getSelectedItem();

            boolean success = userRepository.updateUserStatus(targetId, selectedStatus);

            if (success) {
                JOptionPane.showMessageDialog(this, "🎉 成功將 User ID " + targetId + " 狀態更新為: " + selectedStatus, "更新成功", JOptionPane.INFORMATION_MESSAGE);
                txtTargetUserId.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "❌ 找不到該 User ID 的學生。", "錯誤", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "⚠️ 請輸入正確的數字！", "格式錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performAddBook(ActionEvent e) {
        String title = txtNewBookTitle.getText().trim();
        String author = txtNewBookAuthor.getText().trim();
        String publisher = txtNewBookPublisher.getText().trim();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ 書名為必填欄位！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 💡 直接插入一筆新書到資料表
        String sql = "INSERT INTO books (title, authors, publisher, status) VALUES (?, ?, ?, 'AVAILABLE')";
        try (java.sql.Connection conn = library.database.DatabaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, author.isEmpty() ? "未知作者" : author);
            pstmt.setString(3, publisher.isEmpty() ? "未知出版社" : publisher);

            if (pstmt.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(this, "🎉 新書【" + title + "】採購入庫成功！並已自動上架。", "成功", JOptionPane.INFORMATION_MESSAGE);
                txtNewBookTitle.setText("");
                txtNewBookAuthor.setText("");
                txtNewBookPublisher.setText("");
                // 即時刷新圖書清單
                refreshBookTable("");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ 書籍寫入資料庫失敗。", "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }
}