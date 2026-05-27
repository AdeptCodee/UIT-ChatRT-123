import mongoose from "mongoose";

const fundSchema = new mongoose.Schema(
  {
    conversationId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Conversation",
      required: true,
      index: true,
    },
    creatorId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "USERS",
      required: true,
    },
    title: {
      type: String,
      required: true,
      trim: true,
    },
    totalAmount: {
      type: Number,
      required: true,
      min: 0,
    },
    totalDays: {
      type: Number,
      required: true,
      min: 1,
    },
    dailyAmount: {
      type: Number,
      required: true,
      min: 0,
    },
    currentDay: {
      type: Number,
      default: 1,
      min: 1,
    },
    memberIds: {
      type: [mongoose.Schema.Types.ObjectId],
      ref: "USERS",
      required: true,
    },
    dailyAmountPerPerson: {
      type: Number,
      required: true,
      min: 0,
    },
    status: {
      type: String,
      enum: ["active", "completed", "cancelled"],
      default: "active",
    },
  },
  {
    timestamps: true,
  },
);

fundSchema.index({ conversationId: 1, createdAt: -1 });

const Fund = mongoose.model("Fund", fundSchema);
export default Fund;
