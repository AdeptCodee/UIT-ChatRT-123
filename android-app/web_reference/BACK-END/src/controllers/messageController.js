import {
  emitNewMessage,
  updateConversationAfterCreateMessage,
} from "../utils/messageHelper.js";
import Conversation from "../models/Conversation.js";
import Message from "../models/Message.js";
import { uploadMessageImage } from "../middlewares/uploadMiddleware.js";
import { io } from "../socket/index.js";

export const sendDirectMessage = async (req, res) => {
  try {
    const { recipientId, content, conversationId } = req.body;
    const senderId = req.user._id;
    const file = req.file;

    if (!content && !file) {
      return res.status(400).json({ message: "Thiếu nội dung!" });
    }

    let imgUrl;
    if (file) {
      const uploadResult = await uploadMessageImage(file.buffer);
      imgUrl = uploadResult.secure_url;
    }

    let conversation;
    if (conversationId) {
      conversation = await Conversation.findById(conversationId);
    }

    if (!conversation) {
      conversation = await Conversation.create({
        type: "direct",
        participants: [
          { userId: senderId, joinedAt: new Date() },
          { userId: recipientId, joinedAt: new Date() },
        ],
        lastMessageAt: new Date(),
        unreadCounts: new Map(),
      });
    }

    const message = await Message.create({
      conversationId: conversation._id,
      senderId,
      content: content || "",
      imgUrl,
    });

    updateConversationAfterCreateMessage(conversation, message, senderId);

    await conversation.save();

    emitNewMessage(io, conversation, message);

    return res.status(201).json({ message });
  } catch (error) {
    console.error("Lỗi xảy ra khi gửi tin nhắn trực tiếp", error);
    return res.status(500).json({ message: "Lỗi hệ thống!" });
  }
};

export const sendGroupMessage = async (req, res) => {
  try {
    const { conversationId, content } = req.body;
    const senderId = req.user._id;
    const conversation = req.conversation;
    const file = req.file;

    if (!content && !file) {
      return res.status(400).json({ message: "Thiếu nội dung hoặc hình ảnh!" });
    }

    let imgUrl;
    if (file) {
      const uploadResult = await uploadMessageImage(file.buffer);
      imgUrl = uploadResult.secure_url;
    }

    const message = await Message.create({
      conversationId,
      senderId,
      content: content || "",
      imgUrl,
    });

    updateConversationAfterCreateMessage(conversation, message, senderId);

    await conversation.save();

    emitNewMessage(io, conversation, message);

    return res.status(201).json({ message });
  } catch (error) {
    console.error("Lỗi xảy ra khi gửi tin nhắn nhóm", error);
    return res.status(500).json({ message: "Lỗi hệ thống." });
  }
};
