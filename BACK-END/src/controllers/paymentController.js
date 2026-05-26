import USERS from "../models/USERS.js";

// 1. API: Cập nhật thông tin tài khoản ngân hàng của User
export const updateBankDetails = async (req, res) => {
  try {
    const { accountNo, accountName, acqId } = req.body;
    const userId = req.user._id; 

    if (!accountNo || !acqId) {
      return res.status(400).json({ message: "Vui lòng nhập đầy đủ Số tài khoản và Ngân hàng." });
    }

    // Cập nhật vào DB (Đã đổi thành biến USERS)
    const updatedUser = await USERS.findByIdAndUpdate(
      userId,
      { accountNo, accountName: accountName.toUpperCase(), acqId },
      { new: true }
    );

    return res.status(200).json({
      message: "Cập nhật thông tin ngân hàng thành công",
      user: {
        accountNo: updatedUser.accountNo,
        accountName: updatedUser.accountName,
        acqId: updatedUser.acqId
      }
    });
  } catch (error) {
    console.error("Lỗi cập nhật ngân hàng:", error);
    return res.status(500).json({ message: "Lỗi hệ thống khi lưu thông tin" });
  }
};

// 2. API: Tạo mã QR động dựa trên thông tin của chính User đang đăng nhập
export const generatePaymentQR = async (req, res) => {
  try {
    // Lấy thông tin ngân hàng từ chính user đang gọi API 
    const { accountNo, accountName, acqId } = req.user;
    const { amount, addInfo } = req.body;

    // Kiểm tra xem user này đã cài đặt tài khoản ngân hàng chưa
    if (!accountNo || !acqId) {
      return res.status(400).json({ 
        message: "Bạn chưa thiết lập tài khoản ngân hàng nhận tiền. Vui lòng vào cài đặt!" 
      });
    }

    if (!amount || Number(amount) <= 0) {
      return res.status(400).json({ message: "Số tiền không hợp lệ." });
    }

    const qrData = {
      accountNo,
      accountName,
      acqId,
      amount,
      addInfo: addInfo || "Chuyen tien",
      format: "text",
      template: "compact"
    };

    const response = await fetch("https://api.vietqr.io/v2/generate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(qrData),
    });

    const data = await response.json();

    if (data.code !== "00") {
      return res.status(400).json({ message: "Không thể tạo mã QR", error: data.desc });
    }

    return res.status(200).json({
      message: "Tạo mã QR thành công",
      qrDataURL: data.data.qrDataURL
    });

  } catch (error) {
    console.error("Lỗi tạo mã QR:", error);
    return res.status(500).json({ message: "Lỗi hệ thống" });
  }
};