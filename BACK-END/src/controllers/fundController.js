import Fund from "../models/Fund.js";
import FundTracking from "../models/FundTracking.js";
import Conversation from "../models/Conversation.js";
import Message from "../models/Message.js";

// 1. API: Tạo Quỹ Mới (Khởi động heo đất)
export const createFund = async (req, res) => {
  try {
    const { conversationId, title, totalAmount, totalDays } = req.body;
    // Bắt an toàn ID người dùng (phòng hờ token trả về _id thay vì id)
    const creatorId = req.user?.id || req.user?._id;

    // Lấy danh sách thành viên trong nhóm
    const conversation = await Conversation.findById(conversationId);
    if (!conversation)
      return res.status(404).json({ message: "Không tìm thấy nhóm chat" });

    // Lấy mảng ID thành viên cực kỳ an toàn
    const memberIds = conversation.participants.map(
      (p) => p.userId || p._id || p,
    );

    // TÍNH TOÁN ĐẦY ĐỦ 2 TRƯỜNG BỊ THIẾU
    const dailyAmount = Math.ceil(totalAmount / totalDays); // Tổng tiền chia cho số ngày
    const dailyAmountPerPerson = Math.ceil(dailyAmount / memberIds.length); // Tiền 1 ngày chia cho số người

    // Tạo quỹ mới trong DB
    const newFund = new Fund({
      conversationId,
      creatorId,
      title,
      totalAmount,
      totalDays,
      dailyAmount, // <-- ĐÃ BỔ SUNG: Trị dứt điểm lỗi 400
      dailyAmountPerPerson,
      memberIds,
      currentDay: 1,
      status: "active",
    });
    await newFund.save();

    // Trả về kết quả
    res.status(201).json({
      message: "Tạo quỹ thành công!",
      fund: newFund,
    });
  } catch (error) {
    console.error("Lỗi tạo quỹ:", error);
    // Ép Mongoose phun ra đúng cái tên trường bị lỗi để dễ bắt bệnh hơn
    if (error.name === "ValidationError") {
      return res
        .status(400)
        .json({ message: "Dữ liệu thiếu: " + error.message });
    }
    res.status(500).json({ message: "Lỗi Server khi tạo quỹ" });
  }
};

// ... (Hàm skipDay bên dưới ông giữ nguyên nhé, không có lỗi gì đâu)

// 2. API: Hack thời gian (Chuyển sang ngày tiếp theo)
export const skipDay = async (req, res) => {
  try {
    const { conversationId } = req.body;

    // Tìm quỹ đang active của nhóm này
    const fund = await Fund.findOne({ conversationId, status: "active" });
    if (!fund)
      return res
        .status(404)
        .json({ message: "Nhóm này không có quỹ nào đang chạy" });

    // Tăng ngày lên 1
    fund.currentDay += 1;

    // Nếu vượt quá tổng số ngày -> Hoàn thành quỹ
    if (fund.currentDay > fund.totalDays) {
      fund.status = "completed";
      await fund.save();
      return res
        .status(200)
        .json({ message: "🎉 Quỹ đã hoàn thành!", isCompleted: true });
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
        qrUrl: qrUrl,
      },
    });
  } catch (error) {
    console.error("Lỗi nhảy ngày:", error);
    res.status(500).json({ message: "Lỗi Server khi nhảy ngày" });
  }

  // 1. API: Tạo Quỹ Mới (Khởi động heo đất)
  export const createFund = async (req, res) => {
    try {
      const { conversationId, title, totalAmount, totalDays } = req.body;

      // SỬA LỖI 1: Lấy ID người tạo siêu an toàn (Cover mọi loại Middleware)
      const creatorId = req.userId || req.user?.id || req.user?._id || req.user;

      // Lấy danh sách nhóm
      const conversation = await Conversation.findById(conversationId);
      if (!conversation)
        return res.status(404).json({ message: "Không tìm thấy nhóm chat" });

      // SỬA LỖI 2: Quét mảng thành viên an toàn (Cover cả 'participants' và 'members')
      const participantsList =
        conversation.participants || conversation.members || [];
      const memberIds = participantsList.map((p) => p.userId || p._id || p);

      // Tính toán chi tiết 2 trường bị thiếu
      const dailyAmount = Math.ceil(totalAmount / totalDays);
      const dailyAmountPerPerson = Math.ceil(dailyAmount / memberIds.length);

      // Lưu DB
      const newFund = new Fund({
        conversationId,
        creatorId,
        title,
        totalAmount,
        totalDays,
        dailyAmount,
        dailyAmountPerPerson,
        memberIds,
        currentDay: 1,
        status: "active",
      });

      await newFund.save();

      res.status(201).json({
        message: "Tạo quỹ thành công!",
        fund: newFund,
      });
    } catch (error) {
      // RADAR BẮT BỆNH: In thẳng lỗi ra màn hình Terminal của VS Code
      console.error("=== 🚨 LỖI CHI TIẾT TẠO QUỸ 🚨 ===");
      console.error(error.message);

      res.status(400).json({ message: "Lỗi tạo quỹ: " + error.message });
    }
  };
};
