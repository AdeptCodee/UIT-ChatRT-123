// File: test-qr.js
async function runTest() {
  try {
    console.log("1. Đang gọi API đăng nhập để lấy Token...");
    const loginRes = await fetch("http://localhost:5001/api/auth/signin", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      // Dùng tạm tài khoản test của đồ án ông
      body: JSON.stringify({ username: "user1", password: "usertest123" }) 
    });
    
    const loginData = await loginRes.json();
    const token = loginData.accessToken;

    if (!token) {
      return console.log("❌ Đăng nhập thất bại! Kiểm tra lại tài khoản test.");
    }
    console.log("✅ Lấy token thành công!\n");

    console.log("2. Đang gọi API tạo mã VietQR...");
    const qrRes = await fetch("http://localhost:5001/api/payments/qr", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}` // Gắn token vào đây
      },
      body: JSON.stringify({
        accountNo: "0775574799", // <-- ĐIỀN SỐ TÀI KHOẢN VÀO ĐÂY
        accountName: "PHAM TRUNG KIEN",     // Tên chủ tài khoản viết không dấu
        acqId: "970422",                    // Mã ngân hàng (ví dụ 970422 là MBBank)
        amount: 50000,
        addInfo: "Test tinh nang QR"
      })
    });

    const qrData = await qrRes.json();
    console.log("✅ Kết quả Server trả về:", qrData.message);
    console.log("\n👉 Copy toàn bộ đoạn mã Base64 dưới đây, dán vào thanh địa chỉ Chrome rồi nhấn Enter để xem ảnh:");
    console.log("--------------------------------------------------");
    console.log(qrData.qrDataURL);
    console.log("--------------------------------------------------");

  } catch (error) {
    console.error("Lỗi:", error);
  }
}

runTest();