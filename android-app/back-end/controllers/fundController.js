import Fund from "../models/Fund.js";
import FundTracking from "../models/FundTracking.js";
import Conversation from "../models/Conversation.js";
import Message from "../models/Message.js";
import USERS from "../models/USERS.js";
import { io } from "../socket/index.js";
import { emitNewMessage, updateConversationAfterCreateMessage } from "../utils/messageHelper.js";

// 1. API: Tạo Quỹ Mới
export const createFund = async (req, res) => {
  try {
    const { conversationId, title, totalAmount, totalDays } = req.body;
    const creatorId = req.userId || req.user?.id || req.user?._id;

    const conversation = await Conversation.findById(conversationId);
    if (!conversation) return res.status(404).json({ message: "Không tìm thấy nhóm chat" });

    const memberIds = conversation.participants.map(p => p.userId || p._id || p);

    const dailyAmount = Math.ceil(totalAmount / totalDays);
    const dailyAmountPerPerson = Math.ceil(dailyAmount / memberIds.length);

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

    // Khởi tạo Tracking cho ngày 1 cho tất cả thành viên
    const trackings = memberIds.map(mId => ({
      fundId: newFund._id,
      userId: mId,
      dayNumber: 1,
      amountDue: dailyAmountPerPerson,
      status: "PENDING"
    }));
    await FundTracking.insertMany(trackings);

    res.status(201).json({ message: "Tạo quỹ thành công!", fund: newFund });
  } catch (error) {
    console.error("Lỗi tạo quỹ:", error);
    res.status(400).json({ message: "Lỗi tạo quỹ: " + error.message });
  }
};

// 2. API: Chuyển ngày, nhắc nhở tự động và cộng dồn nợ
export const skipDay = async (req, res) => {
  try {
    const { conversationId } = req.body;
    const fund = await Fund.findOne({ conversationId, status: "active" });
    if (!fund) return res.status(404).json({ message: "Nhóm này không có quỹ nào đang hoạt động" });

    const conversation = await Conversation.findById(conversationId);
    const oldDay = fund.currentDay;
    fund.currentDay += 1;

    // Kiểm tra nếu quỹ đã kết thúc
    if (fund.currentDay > fund.totalDays) {
      fund.status = "completed";
      await fund.save();
      return res.status(200).json({ message: "🎉 Quỹ đã hoàn thành!", isCompleted: true });
    }

    await fund.save();

    // 1. Tìm những người chưa đóng tiền ngày cũ
    const unpaidTrackings = await FundTracking.find({
      fundId: fund._id,
      dayNumber: oldDay,
      status: "PENDING"
    });

    const unpaidUserIds = unpaidTrackings.map(t => t.userId.toString());

    // 2. Tạo Tracking cho ngày mới với logic cộng dồn
    const nextDayTrackings = [];
    for (const memberId of fund.memberIds) {
      let amountForToday = fund.dailyAmountPerPerson;

      // Nếu ngày trước chưa đóng, cộng dồn nợ vào ngày hôm nay
      const wasUnpaid = unpaidUserIds.includes(memberId.toString());
      if (wasUnpaid) {
          const prevTracking = unpaidTrackings.find(t => t.userId.toString() === memberId.toString());
          amountForToday += prevTracking.amountDue;
          // Cập nhật trạng thái ngày cũ là OVERDUE
          prevTracking.status = "OVERDUE";
          await prevTracking.save();
      }

      nextDayTrackings.push({
        fundId: fund._id,
        userId: memberId,
        dayNumber: fund.currentDay,
        amountDue: amountForToday,
        status: "PENDING"
      });
    }
    await FundTracking.insertMany(nextDayTrackings);

    // 3. Lấy thông tin ngân hàng người tạo để tạo link QR
    const creator = await USERS.findById(fund.creatorId);
    let qrUrl = "";
    if (creator && creator.accountNo && creator.acqId) {
       // Tạo link VietQR tự động
       qrUrl = `https://img.vietqr.io/image/${creator.acqId}-${creator.accountNo}-compact.png?amount=${fund.dailyAmountPerPerson}&addInfo=DONG%20QUY%20${fund.title.replace(/ /g, '%20')}%20NGAY%20${fund.currentDay}`;
    }

    // 4. Soạn tin nhắn nhắc nhở tự động
    let reminderContent = `📢 [TỰ ĐỘNG: NHẮC ĐÓNG QUỸ - NGÀY ${fund.currentDay}]\n` +
                          `📌 Quỹ: ${fund.title}\n` +
                          `💰 Số tiền gốc mỗi ngày: ${fund.dailyAmountPerPerson.toLocaleString()} VNĐ\n`;

    if (unpaidUserIds.length > 0) {
        const users = await USERS.find({ _id: { $in: unpaidUserIds } });
        const names = users.map(u => u.displayName).join(", ");
        reminderContent += `\n⚠️ Lưu ý: Các thành viên (${names}) chưa đóng tiền ngày ${oldDay} nên số tiền đã được cộng dồn vào hôm nay!\n`;
    }

    reminderContent += `\nCác bạn vui lòng kiểm tra và đóng quỹ đúng hạn nhé. Cảm ơn!`;

    // 5. Lưu tin nhắn vào DB và bắn Socket Realtime
    const autoMessage = await Message.create({
      conversationId: fund.conversationId,
      senderId: fund.creatorId,
      content: reminderContent,
      imgUrl: qrUrl // Đưa QR vào phần ảnh của tin nhắn
    });

    // Cập nhật thông tin tin nhắn cuối cùng cho hội thoại
    updateConversationAfterCreateMessage(conversation, autoMessage, fund.creatorId);
    await conversation.save();

    // Bắn socket cho tất cả thành viên trong nhóm
    emitNewMessage(io, conversation, autoMessage);

    res.status(200).json({
      message: `Đã chuyển sang Ngày ${fund.currentDay}`,
      fundInfo: {
        currentDay: fund.currentDay,
        qrUrl: qrUrl
      }
    });
  } catch (error) {
    console.error("Lỗi khi nhảy ngày quỹ:", error);
    res.status(500).json({ message: "Lỗi hệ thống khi xử lý quỹ" });
  }
};
