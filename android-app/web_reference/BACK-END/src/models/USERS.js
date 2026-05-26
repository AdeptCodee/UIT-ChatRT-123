import mongoose from "mongoose";

const userSchema = new mongoose.Schema(
  {
    username: {
      type: String,
      required: true,
      unique: true,
      trim: true, // Loại bỏ khoảng trắng ở đầu và cuối chuỗi
      lowercase: true, // Chuyển đổi chuỗi thành chữ thường
    },
    hashedPassword: {
      type: String,
      required: true,
    },
    email: {
      type: String,
      required: true,
      unique: true,
      trim: true,
      lowercase: true,
    },
    displayName: {
      type: String,
      required: true,
      trim: true,
    },
    avatarUrl: {
      type: String,
    },
    avatarId: {
      type: String,
    },
    bio: {
      type: String,
      maxLength: 500,
    },
    phone: {
      type: String,
      sparse: true, // Cho phép giá trị trống
    },
    },
  {
    timestamps: true,
  },
);

const USERS = mongoose.model("USERS", userSchema);

export default USERS;
