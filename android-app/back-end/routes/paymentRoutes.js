import express from "express";
import { protectedRoute } from "../middlewares/authMiddleware.js";
import { generatePaymentQR, updateBankDetails } from "../controllers/paymentController.js";

const router = express.Router();

router.post("/qr", protectedRoute, generatePaymentQR);
router.put("/bank-setup", protectedRoute, updateBankDetails); // <-- CỔNG MỚI ĐỂ LƯU STK

export default router;