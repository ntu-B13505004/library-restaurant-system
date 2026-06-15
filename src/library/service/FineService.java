package library.service;

import library.model.BorrowRecord;
import library.model.Fine;
import library.model.UserStatus;
import library.repository.BorrowRecordRepository;
import library.repository.FineRepository;
import library.repository.UserRepository;

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
     */
    public Fine createOrUpdateFine(BorrowRecord record) {
        if (record == null || !record.isOverdue()) {
            return null;
        }

        try {
            Fine existingFine = fineRepository.findByRecordId(record.getRecordId());

            if (existingFine == null) {
                // 首次逾期：建立罰單並寫入 DB
                Fine newFine = new Fine(0, record);
                fineRepository.saveOrUpdate(newFine);

                // 💡 聯動停權：將該學生狀態改為 SUSPENDED
                userRepository.updateUserStatus(record.getUser().getUserId(), UserStatus.SUSPENDED);
                return newFine;
            } else {
                // 已存在且未付：動態更新最新罰金（根據天數累積）寫入資料庫
                if (!existingFine.isPaid()) {
                    existingFine.getAmount(); // 觸發 Model 內部更新金額
                    fineRepository.saveOrUpdate(existingFine);
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
     * ✨ 核心功能 2：辦理繳納罰款 (含自動復權邏輯)
     */
    public String payFine(int fineId) {
        try {
            Fine fine = fineRepository.findById(fineId);

            if (fine == null) {
                return "❌ 系統錯誤：找不到該筆罰款紀錄。";
            }
            if (fine.isPaid()) {
                return "⚠️ 提示：此筆罰款先前已繳清，請勿重複繳納。";
            }

            // 1. 執行繳款
            fine.pay();
            fineRepository.saveOrUpdate(fine);

            // 2. 💡 自動復權稽核：檢查該學生是否已還清所有未繳罰款？
            int userId = fine.getRecord().getUser().getUserId();
            int remainingUnpaid = fineRepository.countUnpaidFinesByUserId(userId);

            if (remainingUnpaid == 0) {
                // 債務全部清空，自動將學生改回 ACTIVE 狀態，使其恢復借書權利！
                userRepository.updateUserStatus(userId, UserStatus.ACTIVE);
                return "SUCCESS_AND_ACTIVATED";
            }

            return "SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 資料庫更新失敗，繳費交易未完成。";
        }
    }

    // 在 FineService.java 中的獲取罰款方法 (概念示意)
    public List<Fine> getUnpaidFinesByUser(int userId) {
        // 1. 先抓出這個學生所有「尚未歸還」的借閱紀錄
        List<BorrowRecord> activeRecords = BorrowRecordRepository.findActiveByUserId(userId);

        // 2. 巡覽這些紀錄，只要發現現在時間已經超過 dueDate，就確保它在 fines 表裡有資料
        for (BorrowRecord record : activeRecords) {
            if (LocalDateTime.now().isAfter(record.getDueDate())) {
                // 如果是過期的，建立一個未繳清的 Fine 物件並存進資料庫 (這會觸發你的 saveOrUpdate)
                Fine newOrUpdatedFine = new Fine(0, record);
                fineRepository.saveOrUpdate(newOrUpdatedFine);
            }
        }

        // 3. 檢查完畢後，這時資料庫裡該有的罰單都有了，再呼叫原本的查詢
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
     * ✨ 管理者功能 5：全校未繳罰款清單
     */
    public List<Fine> getAllUnpaidFines() {
        List<Fine> allUnpaid = fineRepository.findAllUnpaid();
        for(Fine f : allUnpaid) { f.getAmount(); }
        return allUnpaid;
    }
}
