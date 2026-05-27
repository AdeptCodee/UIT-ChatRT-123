import express from "express";
import { createFund, skipDay } from "../controllers/fundController.js";
import { protectedRoute } from "../middlewares/authMiddleware.js";

const router = express.Router();

// Tất cả API quỹ đều cần đăng nhập
router.use(protectedRoute);

router.post("/create", createFund);
router.post("/skipday", skipDay);

export default router;
