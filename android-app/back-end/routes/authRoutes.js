import express from "express";
import { signUp } from "../controllers/authController.js";
import { signIn } from "../controllers/authController.js";
import { signOut } from "../controllers/authController.js";
import { refreshToken } from "../controllers/authController.js";

const router = express.Router();

// Đăng ký
router.post("/signup", signUp);

// Đăng nhập
router.post("/signin", signIn);

// Đăng xuất
router.post("/signout", signOut);

// Giữ trang luôn đăng nhập khi còn refeshToken
router.post("/refresh", refreshToken);

export default router;
