import express from "express";
import {
  sendDirectMessage,
  sendGroupMessage,
} from "../controllers/messageController.js";
import {
  checkFriendShip,
  checkGroupMembership,
} from "../middlewares/friendMiddleware.js";
import { upload } from "../middlewares/uploadMiddleware.js";

const router = express.Router();

router.post(
  "/direct",
  upload.single("image"),
  checkFriendShip,
  sendDirectMessage,
);
router.post(
  "/group",
  upload.single("image"),
  checkGroupMembership,
  sendGroupMessage,
);

export default router;
