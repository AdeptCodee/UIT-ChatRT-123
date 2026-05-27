import Fund from "../models/Fund.js";
import FundTracking from "../models/FundTracking.js";
import Conversation from "../models/Conversation.js";
import Message from "../models/Message.js";

// 1. API: Tạo Quỹ Mới (Khởi động heo đất)
export const createFund = async (req, res) => {
    try {
        const { conversationId, title, totalAmount, totalDays } = req.body;
        const creatorId = req.user.id; // Lấy từ token đăng nhập

        // Lấy danh sách thành viên trong nhóm
        const conversation = await Conversation.findById(conversationId);
        if (!conversation) return res.status(404).json({ message: "Không tìm thấy nhóm chat" });

        const memberIds = conversation.participants.map(p => p.userId);
        const dailyAmountPerPerson = Math.ceil(totalAmount / totalDays / memberIds.length);

        // Tạo quỹ mới trong DB
        const newFund = new Fund({
            conversationId,
            creatorId,
            title,
            totalAmount,
            totalDays,
            dailyAmountPerPerson,
            memberIds,
            currentDay: 1,
            status: "active"
        });
        await newFund.save();

        // Trả về kết quả
        res.status(201).json({
            message: "Tạo quỹ thành công!",
            fund: newFund
        });

    } catch (error) {
        console.error("Lỗi tạo quỹ:", error);
        res.status(500).json({ message: "Lỗi Server khi tạo quỹ" });
    }
};

// 2. API: Hack thời gian (Chuyển sang ngày tiếp theo)
export const skipDay = async (req, res) => {
    try {
        const { conversationId } = req.body;

        // Tìm quỹ đang active của nhóm này
        const fund = await Fund.findOne({ conversationId, status: "active" });
        if (!fund) return res.status(404).json({ message: "Nhóm này không có quỹ nào đang chạy" });

        // Tăng ngày lên 1
        fund.currentDay += 1;

        // Nếu vượt quá tổng số ngày -> Hoàn thành quỹ
        if (fund.currentDay > fund.totalDays) {
            fund.status = "completed";
            await fund.save();
            return res.status(200).json({ message: "🎉 Quỹ đã hoàn thành!", isCompleted: true });
        }

        await fund.save();

        // Tự động sinh link QR của VietQR (Dùng STK demo MBBank của ông hôm trước)
        // Cú pháp nội dung: CHTRT QUY [Ngay]
        const qrUrl = `https://img.vietqr.io/image/970422-0775574799-compact.png?amount=${fund.dailyAmountPerPerson}&addInfo=CHTRT%20QUY%20NGAY%20${fund.currentDay}`;

        // Trả về thông tin ngày mới kèm QR để Mobile hiển thị
        res.status(200).json({
            message: `Đã chuyển sang Ngày ${fund.currentDay}`,
            fundInfo: {
                title: fund.title,
                currentDay: fund.currentDay,
                totalDays: fund.totalDays,
                dailyAmount: fund.dailyAmountPerPerson,
                qrUrl: qrUrl
            }
        });

    } catch (error) {
        console.error("Lỗi nhảy ngày:", error);
        res.status(500).json({ message: "Lỗi Server khi nhảy ngày" });
    }
};
