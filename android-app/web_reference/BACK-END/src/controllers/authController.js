import bcrypt from "bcrypt";
import USERS from "../models/USERS.js";
import Session from "../models/Session.js";
import jwt from "jsonwebtoken";
import crypto from "crypto";

const ACCESS_TOKEN_TTL = "30m"; // TTL: Time To Live - thời gian tồn tại của accessToken
const REFRESH_TOKEN_TTL = 14 * 24 * 60 * 60 * 1000; // TTL: Time To Live - thời gian tồn tại của refreshToken

export const signUp = async (req, res) => {
  try {
    const { username, password, email, firstName, lastName } = req.body;
    if (!username || !password || !email || !firstName || !lastName) {
      return res
        .status(400)
        .json({ message: "Vui lòng điền đầy đủ thông tin!" });
    }

    // Kiểm tra user có tồn tại chưa?
    const duplicateUser = await USERS.findOne({ username });
    if (duplicateUser) {
      return res.status(409).json({ message: "Tên người dùng đã tồn tại!" });
    }
    // Mã hóa pass
    const hashedPassword = await bcrypt.hash(password, 10); //salt = 10, mã hóa 2^10 = 1024 lần

    // Tạo user mới
    await USERS.create({
      username,
      hashedPassword,
      email,
      displayName: `${lastName} ${firstName}`,
    });

    // Returrn
    return res.status(204).json({ message: "Đăng ký thành công!" });
  } catch (error) {
    console.error("Đã xảy ra lỗi khi gọi hàm đăng ký người dùng.", error);
    return res.status(500).json({ message: "Đã xảy ra lỗi máy chủ!" });
  }
};

export const signIn = async (req, res) => {
  try {
    // Lấy input từ request body
    const { username, password } = req.body;
    if (!username || !password) {
      return res
        .status(400)
        .json({ message: "Vui lòng điền đầy đủ username và password!" });
    }

    // Lấy hassedPassword từ database để so sánh với password người dùng nhập
    const user = await USERS.findOne({ username });
    if (!user) {
      return res
        .status(401)
        .json({ message: "Tên người dùng hoặc mật khẩu không đúng!" });
    }

    // Kiểm tra password
    const isMatch = await bcrypt.compare(password, user.hashedPassword);
    if (!isMatch) {
      return res
        .status(401)
        .json({ message: "Tên người dùng hoặc mật khẩu không đúng!" });
    }

    // Nếu khớp, tạo accessToken với JWT
    const accessToken = jwt.sign(
      { userId: user._id },
      process.env.ACCESS_TOKEN_SECRET,
      { expiresIn: ACCESS_TOKEN_TTL },
    );

    // Tạo refreshToken
    const refreshToken = crypto.randomBytes(64).toString("hex");

    // Tạo sesion mới để lưu refreshToken vào database
    await Session.create({
      userId: user._id,
      refreshToken,
      expiresAt: new Date(Date.now() + REFRESH_TOKEN_TTL),
    });

    // Trả refreshToken về cho cookie
    res.cookie("refreshToken", refreshToken, {
      httpOnly: true, // Chỉ cho phép truy cập cookie từ server, không cho phép truy cập từ client (JavaScript)
      secure: true,
      sameSite: "none",
      maxAge: REFRESH_TOKEN_TTL, // Thời gian tồn tại của cookie
    });

    // Trả accessToken về trong response body
    return res.status(200).json({
      message: `Chào mừng ${user.displayName} đã đăng nhập thành công!!`,
      accessToken,
    });
  } catch (error) {
    console.error("Đã xảy ra lỗi khi gọi hàm đăng nhập người dùng.", error);
    return res.status(500).json({ message: "Đã xảy ra lỗi máy chủ!" });
  }
};

export const signOut = async (req, res) => {
  try {
    // Lấy refreshToken từ cookie
    const token = req.cookies?.refreshToken;

    // Xóa refreshToken trong Session của database
    if (token) {
      await Session.deleteOne({ refreshToken: token });

      // Xóa refreshToken trong cookie
      res.clearCookie("refreshToken");
    }

    return res.status(200).json({ message: "Đăng xuất thành công!" });
  } catch (error) {
    console.error("Đã xảy ra lỗi khi gọi hàm đăng xuất người dùng.", error);
    return res.status(500).json({ message: "Đã xảy ra lỗi máy chủ!" });
  }
};
// Tạo accessToken mới từ refreshToken
export const refreshToken = async (req, res) => {
  try {
    // lấy refresh token từ cookie
    const token = req.cookies?.refreshToken;
    if (!token) {
      return res.status(401).json({ message: "Token không tồn tại." });
    }

    // so với refresh token trong database
    const session = await Session.findOne({ refreshToken: token });
    if (!session) {
      return res
        .status(403)
        .json({ message: "Token không hợp lệ hoặc hết hạn" });
    }

    // kiểm tra hết hạn
    if (session.expiresAt < new Date()) {
      return res.status(403).json({ message: "Token đã hết hạn!" });
    }

    // tạo access token mới
    const accessToken = jwt.sign(
      {
        userId: session.userId,
      },
      process.env.ACCESS_TOKEN_SECRET,
      { expiresIn: ACCESS_TOKEN_TTL },
    );

    // return
    return res.status(200).json({ accessToken });
  } catch (error) {
    console.error("Lỗi khi gọi refreshToken", error);
    return res.status(500).json({ message: "Lỗi hệ thống" });
  }
};
