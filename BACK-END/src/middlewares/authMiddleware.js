import jwt from "jsonwebtoken";
import USERS from "../models/USERS.js";

export const protectedRoute = async (req, res, next) => {
    try {
        // Lấy token từ header
        const authHeader = req.headers["authorization"];
        const token = authHeader && authHeader.split(" ")[1]; // Bearer <token>
        
        if (!token) {
            return res.status(401).json({ message: "Acces Token không được tìm thấy!" });
        }
        // Xác nhận token là hợp lệ
        jwt.verify(token, process.env.ACCESS_TOKEN_SECRET, async (err, decodedUser) => {
            if (err) {
                console.error(err);

                return res.status(403).json({ message: "Acces Token không hợp lệ hoặc hết hạn!" });
            }

        // Tìm user tương ứng với token để xác minh 
        // tài khoản này là thật hoặc chưa bị xóa trong database
        const user = await USERS.findById(decodedUser.userId).select("-hashedPassword");

        if (!user) {
            return res.status(404).json({ message: "Người dùng không tồn tại!" });
        }

        // Trả user về trong req để các api sau có thể sử dụng thông tin user
        req.user = user;
        next();
        });

    } catch (error) {
        console.error("Đã xảy ra lỗi xác minh trong authMiddleware.", error);
        return res.status(500).json({ message: "Đã xảy ra lỗi máy chủ!" });
    }
}
