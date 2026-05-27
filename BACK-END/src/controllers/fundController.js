import Fund from "../models/Fund.js";
import FundTracking from "../models/FundTracking.js";
import Conversation from "../models/Conversation.js";
import Message from "../models/Message.js";
import USERS from "../models/USERS.js";
import { io } from "../socket/index.js";
import {
  emitNewMessage,
  updateConversationAfterCreateMessage,
} from "../utils/messageHelper.js";

// 1. API: Tạo Quỹ Mới
export const createFund = async (req, res) => {
  try {
    const { conversationId, title, totalAmount, totalDays } = req.body;

    // 1. Kiểm tra User ID (Lấy từ Middleware auth)
    const creatorId = req.userId || req.user?._id || req.user?.id;
    if (!creatorId) {
      return res
        .status(401)
        .json({
          message: "Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại.",
        });
    }

    // 2. Validate dữ liệu đầu vào
    if (!conversationId || !title || !totalAmount || !totalDays) {
      return res
        .status(400)
        .json({
          message:
            "Thiếu thông tin: Cần có conversationId, title, totalAmount và totalDays.",
        });
    }

    // 3. Tìm nhóm chat và lấy thành viên
    const conversation = await Conversation.findById(conversationId);
    if (!conversation) {
      return res.status(404).json({ message: "Không tìm thấy nhóm chat này." });
    }

    // Lấy mảng ID thành viên và lọc bỏ các giá trị null/undefined
    const memberIds = conversation.participants
      .map((p) => p.userId || p._id)
      .filter((id) => id != null);

    if (memberIds.length === 0) {
      return res
        .status(400)
        .json({ message: "Nhóm chat không có thành viên nào hợp lệ." });
    }

    // 4. Tính toán số tiền (Ép kiểu Number để tránh lỗi chuỗi)
    const amount = Number(totalAmount);
    const days = Number(totalDays);

    if (isNaN(amount) || isNaN(days) || days <= 0) {
      return res
        .status(400)
        .json({ message: "Số tiền hoặc số ngày không hợp lệ." });
    }

    const dailyAmount = Math.ceil(amount / days);
    const dailyAmountPerPerson = Math.ceil(dailyAmount / memberIds.length);

    // 5. Tạo Object Fund
    const newFund = new Fund({
      conversationId,
      creatorId,
      title: title.trim(),
      totalAmount: amount,
      totalDays: days,
      dailyAmount,
      dailyAmountPerPerson,
      memberIds,
      currentDay: 1,
      status: "active",
    });

    await newFund.save();

    // 6. Khởi tạo Tracking cho ngày đầu tiên
    const trackings = memberIds.map((mId) => ({
      fundId: newFund._id,
      userId: mId,
      dayNumber: 1,
      amountDue: dailyAmountPerPerson,
      status: "PENDING",
    }));
    await FundTracking.insertMany(trackings);

    res.status(201).json({
      message: "Tạo quỹ thành công!",
      fund: newFund,
    });
  } catch (error) {
    console.error("=== LỖI TẠO QUỸ CHI TIẾT ===", error);
    // Nếu là lỗi Validation của Mongoose, trả về chi tiết trường bị lỗi
    if (error.name === "ValidationError") {
      const messages = Object.values(error.errors).map((val) => val.message);
      return res
        .status(400)
        .json({ message: "Lỗi dữ liệu: " + messages.join(", ") });
    }
    res.status(500).json({ message: "Lỗi Server: " + error.message });
  }
};

// 2. API: Chuyển ngày (skipDay)
export const skipDay = async (req, res) => {
  try {
    const { conversationId } = req.body;
    const fund = await Fund.findOne({ conversationId, status: "active" });
    if (!fund)
      return res
        .status(404)
        .json({ message: "Không có quỹ nào đang chạy trong nhóm này." });

    const conversation = await Conversation.findById(conversationId);
    const oldDay = fund.currentDay;
    fund.currentDay += 1;

    if (fund.currentDay > fund.totalDays) {
      fund.status = "completed";
      await fund.save();
      return res
        .status(200)
        .json({ message: "🎉 Quỹ đã hoàn thành!", isCompleted: true });
    }
    await fund.save();

    // Logic cộng dồn tiền nợ
    const unpaidTrackings = await FundTracking.find({
      fundId: fund._id,
      dayNumber: oldDay,
      status: "PENDING",
    });
    const unpaidUserIds = unpaidTrackings.map((t) => t.userId.toString());

    const nextDayTrackings = [];
    for (const memberId of fund.memberIds) {
      let amountForToday = fund.dailyAmountPerPerson;
      if (unpaidUserIds.includes(memberId.toString())) {
        const prevTracking = unpaidTrackings.find(
          (t) => t.userId.toString() === memberId.toString(),
        );
        amountForToday += prevTracking.amountDue;
        prevTracking.status = "OVERDUE";
        await prevTracking.save();
      }
      nextDayTrackings.push({
        fundId: fund._id,
        userId: memberId,
        dayNumber: fund.currentDay,
        amountDue: amountForToday,
        status: "PENDING",
      });
    }
    await FundTracking.insertMany(nextDayTrackings);

    // Tự động nhắc nhở kèm QR
    const creator = await USERS.findById(fund.creatorId);
    let qrUrl = "";
    if (creator && creator.accountNo && creator.acqId) {
      qrUrl = `https://img.vietqr.io/image/${creator.acqId}-${creator.accountNo}-compact.png?amount=${fund.dailyAmountPerPerson}&addInfo=DONG%20QUY%20${fund.title.replace(/ /g, "%20")}%20NGAY%20${fund.currentDay}`;
    }

    let reminderContent =
      `📢 [NHẮC ĐÓNG QUỸ - NGÀY ${fund.currentDay}]\n` +
      `📌 Quỹ: ${fund.title}\n` +
      `💰 Số tiền đóng: ${fund.dailyAmountPerPerson.toLocaleString()} VNĐ\n`;

    if (unpaidUserIds.length > 0) {
      const users = await USERS.find({ _id: { $in: unpaidUserIds } });
      reminderContent += `⚠️ Lưu ý: ${users.map((u) => u.displayName).join(", ")} chưa đóng ngày cũ (tiền đã cộng dồn).\n`;
    }

    const autoMessage = await Message.create({
      conversationId: fund.conversationId,
      senderId: fund.creatorId,
      content: reminderContent,
      imgUrl: qrUrl,
    });

    updateConversationAfterCreateMessage(
      conversation,
      autoMessage,
      fund.creatorId,
    );
    await conversation.save();
    emitNewMessage(io, conversation, autoMessage);

    res
      .status(200)
      .json({
        message: `Đã chuyển sang Ngày ${fund.currentDay}`,
        currentDay: fund.currentDay,
      });
  } catch (error) {
    console.error("Lỗi skipDay:", error);
    res.status(500).json({ message: "Lỗi hệ thống khi nhảy ngày." });
  }
};
