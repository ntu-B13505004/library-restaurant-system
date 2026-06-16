package library.src.service;

import library.src.database.DatabaseManager;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportService {

    public ReportService() {
        // 初始化需要的 Repository 或設定
    }

    /**
     * ✨ 進階加分功能 1：統計各書籍主題（Subjects）的借閱熱度排名
     * 專為 JavaFX PieChart（圓餅圖）或 BarChart（柱狀圖）設計的數據源。
     * * @return Map<主題名稱, 借閱次數>，使用 LinkedHashMap 確保排序由高到低
     */
    public Map<String, Integer> getBookSubjectPopularity() {
        Map<String, Integer> reportData = new LinkedHashMap<>();

        // 透過 JOIN 結合書籍表與借閱紀錄表，並依據主題進行分組統計
        String sql = "SELECT b.subjects, COUNT(r.record_id) AS borrow_count " +
                "FROM borrow_records r " +
                "JOIN books b ON r.book_id = b.book_id " +
                "WHERE b.subjects IS NOT NULL AND b.subjects != '' " +
                "GROUP BY b.subjects " +
                "ORDER BY borrow_count DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String subject = rs.getString("subjects");
                int count = rs.getInt("borrow_count");
                reportData.put(subject, count);
            }
        } catch (SQLException e) {
            System.err.println("❌產生書籍主題借閱統計報表失敗");
            e.printStackTrace();
        }
        return reportData;
    }

    /**
     * ✨ 進階加分功能 2：統計全校借閱排行榜 Top 5 書籍
     * 讓管理員知道哪些書最熱門、最需要購置新副本。
     * * @return Map<書名, 借閱次數>
     */
    public Map<String, Integer> getTop5BorrowedBooks() {
        Map<String, Integer> reportData = new LinkedHashMap<>();

        String sql = "SELECT b.title, COUNT(r.record_id) AS borrow_count " +
                "FROM borrow_records r " +
                "JOIN books b ON r.book_id = b.book_id " +
                "GROUP BY b.book_id " +
                "ORDER BY borrow_count DESC " +
                "LIMIT 5";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                reportData.put(rs.getString("title"), rs.getInt("borrow_count"));
            }
        } catch (SQLException e) {
            System.err.println("❌ 產生 Top 5 熱門書籍報表失敗");
            e.printStackTrace();
        }
        return reportData;
    }

    /**
     * ✨ 財務營運報表 3：統計全系統罰款營運狀況
     * 整合你的 Fine 模型，統計目前的財務數據。
     * * @return Map<指標名稱, 金額/數量>
     */
    public Map<String, Object> getLibraryFineSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        String sqlTotalFines = "SELECT COUNT(*) AS total_count, SUM(amount) AS total_amount FROM fines";
        String sqlUnpaidFines = "SELECT COUNT(*) AS unpaid_count, SUM(amount) AS unpaid_amount FROM fines WHERE is_paid = 0";

        try (Connection conn = DatabaseManager.getConnection()) {

            // 1. 統計歷史總罰單金額與數量
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlTotalFines)) {
                if (rs.next()) {
                    summary.put("歷史罰單總總數", rs.getInt("total_count"));
                    summary.put("歷史罰款總金額", rs.getInt("total_amount"));
                }
            }

            // 2. 統計未繳清的罰單金額（風控指標）
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlUnpaidFines)) {
                if (rs.next()) {
                    summary.put("未繳罰單總件數", rs.getInt("unpaid_count"));
                    summary.put("未繳罰款總金額", rs.getInt("unpaid_amount"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 產生罰款摘要報表失敗");
            e.printStackTrace();
        }
        return summary;
    }

    /**
     * ✨ 營運稽核 4：統計目前圖書館的「關鍵績效指標 (KPI)」
     * 秀在管理員首頁的資訊看板（Dashboard）。
     */
    public Map<String, Integer> getLibraryGeneralKPIs() {
        Map<String, Integer> kpis = new LinkedHashMap<>();

        String sqlBooks = "SELECT COUNT(*) FROM books";
        String sqlBorrowed = "SELECT COUNT(*) FROM books WHERE status = 'BORROWED'";
        String sqlSuspendedUsers = "SELECT COUNT(*) FROM users WHERE status = 'SUSPENDED'";

        try (Connection conn = DatabaseManager.getConnection()) {

            // 總藏書量
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlBooks)) {
                if (rs.next()) kpis.put("總藏書量", rs.getInt(1));
            }
            // 目前在外借出數量
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlBorrowed)) {
                if (rs.next()) kpis.put("目前借出冊數", rs.getInt(1));
            }
            // 目前被停權的學生總數
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlSuspendedUsers)) {
                if (rs.next()) kpis.put("停權學生人數", rs.getInt(1));
            }

        } catch (SQLException e) {
            System.err.println("❌ 讀取圖書館 KPI 失敗");
            e.printStackTrace();
        }
        return kpis;
    }
}