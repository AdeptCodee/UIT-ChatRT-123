import express from "express";
import {
  getMyReminders,
  createReminder,
} from "../controllers/reminderController.js";
import { protectedRoute } from "../middlewares/authMiddleware.js";

const router = express.Router();

router.get("/", protectedRoute, getMyReminders);
router.post("/", protectedRoute, createReminder);

export default router;
