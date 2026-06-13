package library.repository;

import library.database.DatabaseManager;
import library.model.BorrowRecord;
import library.model.Fine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FineRepository {

    // 需要用這個來重組 BorrowRecord 物件
    // private final BorrowRecordRepository recordRepository = new BorrowRecordRepository();

    /**
     * ✨ 1. 新增或更新罰單金額與狀態
     */
    public boolean saveOrUpdate(Fine fine) {
        String checkSql = "SELECT COUNT(*) FROM fines WHERE record_id = ?";
        String insertSql = "INSERT INTO fines (record_id, amount, is_paid) VALUES (?, ?, ?)";
        String updateSql = "UPDATE fines SET amount = ?, is_paid = ? WHERE record_id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {

            boolean exists = false;
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, fine.getRecord().getRecordId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) exists = true;
                }
            }

            if (!exists) {
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setInt(1, fine.getRecord().getRecordId());
                    pstmt.setInt(2, fine.getAmount());
                    pstmt.setInt(3, fine.isPaid() ? 1 : 0);
                    return pstmt.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setInt(1, fine.getAmount());
                    pstmt.setInt(2, fine.isPaid() ? 1 : 0);
                    pstmt.setInt(3, fine.getRecord().getRecordId());
                    return pstmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✨ 2. 統計某位學生目前「未繳清」的罰單總件數
     * 用於 FineService 自動稽核該學生能不能「滿血復權（ACTIVE）」！
     */
    public int countUnpaidFinesByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM fines f " +
                "JOIN borrow_records r ON f.record_id = r.record_id " +
                "WHERE r.user_id = ? AND f.is_paid = 0";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}