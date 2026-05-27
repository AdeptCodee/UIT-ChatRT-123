import mongoose from "mongoose";

const reminderSchema = new mongoose.Schema({
  conversationId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "Conversation",
    required: true,
  },
  messageId: {
    type: String,
    unique: true, // Tránh trùng lặp khi cả 2 bên cùng nhận tin nhắn
  },
  creatorId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "USERS",
    required: true,
  },
  partnerId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "USERS",
    required: true,
  },
  content: { type: String, required: true },
  dueDate: { type: Date, required: true },
  createdAt: { type: Date, default: Date.now },
});

const Reminder = mongoose.model("Reminder", reminderSchema);
export default Reminder;
