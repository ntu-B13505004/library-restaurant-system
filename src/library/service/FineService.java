package library.service;

import library.model.BorrowRecord;
import library.model.Fine;
import java.util.ArrayList;
import java.util.List;

// 以下為概念性 import，視你實際的 Repository 命名與架構調整
// import library.repository.FineRepository;
// import library.repository.UserRepository;

public class FineService {

    // private final FineRepository fineRepository;
    // private final UserRepository userRepository;

    public FineService() {
        // this.fineRepository = new FineRepository();
        // this.userRepository = new UserRepository();
    }

    /**
     * ✨ 核心交易 1：為逾期紀錄建立/更新罰單
     * 當學生「還書時」發現逾期，或是管理者「手動刷新整理」時呼叫。
     * * @param record 該筆逾期的借閱紀錄
     * @return 建立或更新後的 Fine 物件
     */
    public Fine createOrUpdateFine(BorrowRecord record) {
        if (record == null || !record.isOverdue()) {
            return null;
        }

        try {
            // 1. 先去資料庫查這筆借閱紀錄是否已經有對應的罰單紀錄
            // Fine existingFine = fineRepository.findByRecordId(record.getRecordId());
            Fine existingFine = null; // 暫代 stub

            if (existingFine == null) {
                // 如果是新逾期，new 一個 Fine 物件（此時會呼叫你寫的 calculateCurrentAmount() 算錢）
                Fine newFine = new Fine(0, record);

                // 寫入資料庫 fines 表
                // fineRepository.save(newFine);

                // 💡 聯動停權邏輯：一旦產生新罰單，立刻將該學生狀態改為 SUSPENDED (簡報第14頁規格)
                // userRepository.updateStatus(record.getUserId(), "SUSPENDED");

                return newFine;
            } else {
                // 如果罰單早就存在且還沒付錢，每次呼叫 getAmount() 都會動態根據最新時間重新計價
                if (!existingFine.isPaid()) {
                    int currentAmount = existingFine.getAmount(); // 觸發你寫的動態更新邏輯
                    // fineRepository.updateAmount(existingFine.getFineId(), currentAmount);
                }
                return existingFine;
            }
        } catch (Exception e) {
            System.err.println("❌ 處理罰單紀錄失敗");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ✨ 核心交易 2：辦理繳納罰款 (為 GUI 「繳費按鈕」量身打造)
     * * @param fineId 罰單的 ID
     * @return 繳費結果訊息（"SUCCESS" 或 錯誤原因）
     */
    public String payFine(int fineId) {
        try {
            // 1. 從資料庫撈出這筆罰單
            // Fine fine = fineRepository.findById(fineId);
            Fine fine = null; // 暫代 stub

            if (fine == null) {
                return "❌ 系統錯誤：找不到該筆罰款紀錄。";
            }
            if (fine.isPaid()) {
                return "⚠️ 提示：此筆罰款先前已繳清，請勿重複繳納。";
            }

            // 2. 執行繳款（鎖定最終金額，將 isPaid 設為 true）
            fine.pay();

            // 3. 更新資料庫中該筆罰單的狀態與最終金額
            // fineRepository.update(fine);

            // 4. 💡 自動復權稽核（關鍵加分業務）：檢查該學生是否「所有罰單都繳清了」？
            int userId = fine.getRecord().getUserId();
            // int remainingUnpaid = fineRepository.countUnpaidByUserId(userId);
            int remainingUnpaid = 0; // 模擬全部繳清的情境

            if (remainingUnpaid == 0) {
                // 【簡報規格：復權】如果債務全部清空，自動幫學生改回 ACTIVE 狀態！
                // userRepository.updateStatus(userId, "ACTIVE");
                return "SUCCESS_AND_ACTIVATED"; // 讓 GUI 知道學生滿血復活了
            }

            return "SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 資料庫更新失敗，繳費交易未完成。";
        }
    }

    /**
     * ✨ 查詢功能 3：獲取特定學生目前「所有未繳清」的罰單清單（學生個人專區、未繳警示表格使用）
     */
    public List<Fine> getUnpaidFinesByUser(int userId) {
        List<Fine> unpaidList = new ArrayList<>();
        // 1. 從 DB 撈出該用戶 is_paid = 0 的所有罰單
        // unpaidList = fineRepository.findUnpaidByUserId(userId);

        // 2. 💡 極度重要：因為未繳清的罰單金額會隨時間每天+10元，
        //    所以在拋給 GUI 顯示前，跑迴圈呼叫一次 getAmount() 確保畫面上顯示的是「今日最新應繳總額」
        for (Fine fine : unpaidList) {
            fine.getAmount();
        }
        return unpaidList;
    }

    /**
     * ✨ 查詢功能 4：計算特定學生目前的累積未繳總金額
     * 用於 GUI 主畫面右上角顯示：「您目前累積未繳罰款：$ X 元」
     */
    public int getTotalUnpaidAmount(int userId) {
        List<Fine> unpaidFines = getUnpaidFinesByUser(userId);
        int total = 0;
        for (Fine fine : unpaidFines) {
            total += fine.getAmount();
        }
        return total;
    }

    /**
     * ✨ 管理者功能 5：全校未繳罰款黑名單流水帳（管理員後台報表）
     */
    public List<Fine> getAllUnpaidFines() {
        // List<Fine> allUnpaid = fineRepository.findAllUnpaid();
        // for(Fine f : allUnpaid) { f.getAmount(); }
        // return allUnpaid;
        return new ArrayList<>();
    }
}