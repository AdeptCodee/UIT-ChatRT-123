import mongoose from "mongoose";

const fundTrackingSchema = new mongoose.Schema(
  {
    fundId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Fund",
      required: true,
      index: true,
    },
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "USERS",
      required: true,
    },
    dayNumber: {
      type: Number,
      required: true,
      min: 1,
    },
    amountDue: {
      type: Number,
      required: true,
      min: 0,
    },
    status: {
      type: String,
      enum: ["PENDING", "PAID", "OVERDUE"],
      default: "PENDING",
    },
    paidAt: {
      type: Date,
      default: null,
    },
  },
  {
    timestamps: true,
  },
);

// Composite index để tìm nhanh tracking cho từng user trong fund
fundTrackingSchema.index({ fundId: 1, userId: 1 });
fundTrackingSchema.index({ fundId: 1, dayNumber: 1 });

const FundTracking = mongoose.model("FundTracking", fundTrackingSchema);
export default FundTracking;
