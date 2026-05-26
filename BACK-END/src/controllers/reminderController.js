import Reminder from "../models/Reminder.js";

export const createReminder = async (req, res) => {
  try {
    const {
      conversationId,
      partnerId,
      content,
      dueDate,
      messageId,
      creatorId,
    } = req.body;

    const existing = await Reminder.findOne({ messageId });
    if (existing) {
      return res.status(200).json(existing);
    }

    const reminder = await Reminder.create({
      conversationId,
      messageId,
      creatorId: creatorId || req.user._id,
      partnerId,
      content,
      dueDate: new Date(dueDate),
    });

    res.status(201).json(reminder);
  } catch (error) {
    console.error("Lỗi tạo reminder:", error);
    res.status(500).json({ message: "Lỗi tạo nhắc hẹn" });
  }
};

export const getMyReminders = async (req, res) => {
  try {
    const now = new Date();
    await Reminder.deleteMany({ dueDate: { $lt: now } });

    const reminders = await Reminder.find({
      $or: [{ creatorId: req.user._id }, { partnerId: req.user._id }],
      dueDate: { $gte: now },
    })
      .populate("creatorId partnerId", "username displayName avatarUrl")
      .sort({ dueDate: 1 });

    res.status(200).json(reminders);
  } catch (error) {
    res.status(500).json({ message: "Lỗi lấy danh sách nhắc hẹn" });
  }
};
