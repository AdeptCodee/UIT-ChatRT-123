import jwt from "jsonwebtoken";
import USER from "../models/USERS.js";

export const socketAuthMiddleware = async (socket, next) => {
    try {
        const token = socket.handshake.auth?.token;
        if (!token) {
            return next(new Error("Token không tồn tại!"))
        };

        const decoded = jwt.verify(token, process.env.ACCESS_TOKEN_SECRET);
        if (!decoded) {
            return next(new Error("Token không hợp lệ hoặc đã hết hạn!"))
        }

        const user = await USER.findById(decoded.userId).select("-hashedPassword");
        
        if (!user) {
            return next(new Error("Người dùng không tồn tại!"))
        }
        socket.user = user;

        next();
    } catch (error) {
        console.error("Lỗi xác thực jwt socket trong socketMiddleWare", error);
        next(new Error("Lỗi xác thực!"));
    }
};