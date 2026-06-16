package library.src.service;

import library.src.model.BorrowRecord;
import library.src.model.Fine;
import library.src.model.UserStatus;
import library.src.repository.BorrowRecordRepository;
import library.src.repository.FineRepository;
import library.src.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

public class FineService {

    private final FineRepository fineRepository;
    private final UserRepository userRepository;

    public FineService() {
        this.fineRepository = new FineRepository();
        this.userRepository = new UserRepository();
    }

    /**
     * ✨ 核心功能 1：為逾期紀錄建立/更新罰單
     * 💡 逾期即開罰：只要 now() > dueDate，不需等到還書
     */
    public Fine createOrUpdateFine(BorrowRecord record) {
        if (record == null) return null;

        // 動態判斷是否逾期（不依賴 isOverdue 旗標，直接比時間）
        record.checkOverdue();
        if (!record.isOverdue()) return null;

        try {
            Fine existingFine = fineRepository.findByRecordId(record.getRecordId());

            if (existingFine == null) {
                // 首次逾期：建立罰單並寫入 DB
                Fine newFine = new Fine(0, record);
                fineRepository.saveOrUpdate(newFine);

                // 聯動停權：將該學生狀態改為 SUSPENDED
                userRepository.updateUserStatus(record.getUser().getUserId(), UserStatus.SUSPENDED);
                return newFine;
            } else {
                // 已存在且未付：累積罰金，更新 DB 金額快照
                if (!existingFine.isPaid()) {
                    fineRepository.saveOrUpdate(existingFine); // getAmount() 在 saveOrUpdate 內會動態取最新值
                }
                return existingFine;
            }
        } catch (Exception e) {
            System.err.println("❌處理罰單紀錄失敗");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ✨ 核心功能 2：辦理繳納罰款 (含自動復權邏輯)
     */
    public String payFine(int fineId) {
        try {
            Fine fine = fineRepository.findById(fineId);

            if (fine == null) {
                return "❌系統錯誤：找不到該筆罰款紀錄。";
            }
            if (fine.isPaid()) {
                return "⚠️提示：此筆罰款先前已繳清，請勿重複繳納。";
            }

            // 執行繳款（金額鎖定為繳款當下的累積值）
            fine.pay();
            fineRepository.saveOrUpdate(fine);

            // 自動復權稽核：確認是否已無其他未繳罰款
            int userId = fine.getRecord().getUser().getUserId();
            int remainingUnpaid = fineRepository.countUnpaidFinesByUserId(userId);

            if (remainingUnpaid == 0) {
                userRepository.updateUserStatus(userId, UserStatus.ACTIVE);
                return "SUCCESS_AND_ACTIVATED";
            }

            return "SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌資料庫更新失敗，繳費交易未完成。";
        }
    }

    /**
     * ✨ 查詢功能 3：取得特定學生所有未繳罰款
     * 💡 逾期即開罰：掃描所有「尚未還書且已過期」的紀錄，確保罰單存在於 DB
     */
    public List<Fine> getUnpaidFinesByUser(int userId) {
        // 1. 掃描這個學生所有「尚未歸還」的借閱紀錄
        List<BorrowRecord> activeRecords = BorrowRecordRepository.findActiveByUserId(userId);

        for (BorrowRecord record : activeRecords) {
            // 只要現在時間已超過 dueDate，就確保罰單存在並更新金額
            if (LocalDateTime.now().isAfter(record.getDueDate())) {
                Fine existingFine = fineRepository.findByRecordId(record.getRecordId());
                if (existingFine == null) {
                    // 首次逾期：建立罰單 + 停權
                    Fine newFine = new Fine(0, record);
                    fineRepository.saveOrUpdate(newFine);
                    userRepository.updateUserStatus(userId, UserStatus.SUSPENDED);
                } else if (!existingFine.isPaid()) {
                    // 已有罰單未繳清：更新累積金額快照到 DB
                    fineRepository.saveOrUpdate(existingFine);
                }
            }
        }

        // 2. 重新從 DB 撈取最新未繳罰單列表回傳
        return fineRepository.findUnpaidByUserId(userId);
    }

    /**
     * ✨ 查詢功能 4：計算特定學生目前的累積未繳總金額
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
     * ✨ 管理者功能 5：全館未繳罰款清單
     * 💡 同樣觸發「逾期即開罰」掃描，確保資料完整
     */
    public List<Fine> getAllUnpaidFines() {
        // 掃描全館所有未還書的逾期紀錄，補建罰單
        List<BorrowRecord> allActiveRecords = BorrowRecordRepository.findAllActive();
        for (BorrowRecord record : allActiveRecords) {
            if (LocalDateTime.now().isAfter(record.getDueDate())) {
                Fine existingFine = fineRepository.findByRecordId(record.getRecordId());
                if (existingFine == null) {
                    Fine newFine = new Fine(0, record);
                    fineRepository.saveOrUpdate(newFine);
                    userRepository.updateUserStatus(record.getUser().getUserId(), UserStatus.SUSPENDED);
                } else if (!existingFine.isPaid()) {
                    fineRepository.saveOrUpdate(existingFine);
                }
            }
        }
        return fineRepository.findAllUnpaid();
    }
}