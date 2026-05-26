import mongoose from "mongoose";

const sessionSchmema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      required: true,
      ref: "USERS",
      index: true, // Tạo index cho trường userId để tăng tốc truy vấn
    },
    refreshToken: {
      type: String,
      required: true,
      undefined: true,
    },
    expiresAt: {
      type: Date,
      required: true,
    },
  },
  {
    timestamps: true,
  },
);

// Tự động xóa session khi refreshToken hết hạn
sessionSchmema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

export default mongoose.model("Session", sessionSchmema);
