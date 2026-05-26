# ChatRT Backend API Documentation

## Tổng quan

API này phục vụ cho hệ thống ChatRT: đăng ký, đăng nhập, quản lý người dùng, bạn bè, hội thoại, tin nhắn thời gian thực, thanh toán QR và nhắc hẹn.

> Base URL chung: `{{SERVER_URL}}/api`
>
> Trong môi trường phát triển: `http://localhost:5001/api`
> Trong production (ứng với mobile hiện tại): `https://uit-chatrt-123-backend.onrender.com/api`

## Authentication

### 1. POST /auth/signup
- Mô tả: Đăng ký tài khoản mới
- Thân yêu cầu (JSON):
  - `username` (string) - bắt buộc
  - `password` (string) - bắt buộc
  - `email` (string) - bắt buộc
  - `firstName` (string)
  - `lastName` (string)
- Trả về:
  - `200` đăng ký thành công
  - `400` thiếu tham số
  - `409` username trùng

### 2. POST /auth/signin
- Mô tả: Đăng nhập và nhận Access Token
- Thân yêu cầu (JSON):
  - `username` (string) - bắt buộc
  - `password` (string) - bắt buộc
- Trả về:
  - `200` đăng nhập thành công
  - `401` sai username/password
- Ghi chú: Refresh token được trả qua cookie và được lưu trên client khi đăng nhập thành công.

### 3. POST /auth/signout
- Mô tả: Đăng xuất
- Thân yêu cầu: Không
- Trả về: `200` đăng xuất thành công

### 4. POST /auth/refresh
- Mô tả: Cấp lại Access Token từ Refresh Token
- Yêu cầu: Refresh Token phải có trong cookie trình duyệt/client
- Trả về:
  - `200` cấp token thành công
  - `401` không có token hoặc token hết hạn

## Users

> Tất cả endpoint trong nhóm này yêu cầu header `Authorization: Bearer <accessToken>`.

### 5. GET /users/me
- Mô tả: Lấy thông tin user hiện tại
- Query: không
- Thân yêu cầu: không
- Trả về:
  - `200` thông tin user

### 6. GET /users/search
- Mô tả: Tìm người dùng theo username
- Query params:
  - `username` (string) - bắt buộc
- Trả về:
  - `200` danh sách kết quả

### 7. POST /users/uploadAvatar
- Mô tả: Upload ảnh đại diện
- Thân yêu cầu: multipart/form-data
  - `file` - file ảnh
- Trả về:
  - `200` upload thành công, trả về URL ảnh Cloudinary

## Friends

> Tất cả endpoint yêu cầu `Authorization: Bearer <accessToken>`.

### 8. POST /friends/requests
- Mô tả: Gửi lời mời kết bạn
- Thân yêu cầu (JSON):
  - `to` (string) - ID người nhận, bắt buộc
  - `message` (string)
- Trả về:
  - `200` đã gửi lời mời

### 9. GET /friends/requests
- Mô tả: Lấy danh sách lời mời kết bạn
- Trả về:
  - `200` danh sách lời mời đã gửi và nhận

### 10. POST /friends/requests/{requestId}/accept
- Mô tả: Chấp nhận lời mời kết bạn
- Path param:
  - `requestId` (string) - bắt buộc
- Trả về:
  - `200` đã thêm bạn bè thành công

### 11. POST /friends/requests/{requestId}/decline
- Mô tả: Từ chối lời mời kết bạn
- Path param:
  - `requestId` (string) - bắt buộc
- Trả về:
  - `204` từ chối thành công

### 12. GET /friends/
- Mô tả: Lấy danh sách bạn bè
- Trả về:
  - `200` danh sách bạn bè

## Conversations

> Tất cả endpoint yêu cầu `Authorization: Bearer <accessToken>`.

### 13. POST /conversations/
- Mô tả: Tạo cuộc hội thoại mới (cá nhân hoặc nhóm)
- Thân yêu cầu (JSON):
  - `type` (string) - `direct` hoặc `group`
  - `name` (string) - bắt buộc nếu `type` là `group`
  - `memberIds` (array[string]) - danh sách ID thành viên, bắt buộc
- Trả về:
  - `200` tạo thành công

### 14. GET /conversations/
- Mô tả: Lấy danh sách tất cả cuộc hội thoại của user
- Trả về:
  - `200` danh sách các cuộc hội thoại

### 15. GET /conversations/{conversationId}/messages
- Mô tả: Lấy tin nhắn trong một cuộc hội thoại
- Path param:
  - `conversationId` (string) - bắt buộc
- Query params:
  - `limit` (integer) - số lượng tin nhắn, mặc định 50
  - `cursor` (string) - con trỏ phân trang
- Trả về:
  - `200` danh sách tin nhắn

### 16. PATCH /conversations/{conversationId}/seen
- Mô tả: Đánh dấu toàn bộ tin nhắn trong cuộc hội thoại là đã xem
- Path param:
  - `conversationId` (string) - bắt buộc
- Trả về:
  - `200` cập nhật thành công

## Messages

> Tất cả endpoint yêu cầu `Authorization: Bearer <accessToken>`.

### 17. POST /messages/direct
- Mô tả: Gửi tin nhắn 1-1
- Thân yêu cầu: multipart/form-data
  - `recipientId` (string) - bắt buộc
  - `conversationId` (string)
  - `content` (string)
  - `image` (binary file) - tùy chọn
- Trả về:
  - `200` gửi thành công
  - `403` nếu không đủ quyền (không phải bạn bè hoặc bị block)

### 18. POST /messages/group
- Mô tả: Gửi tin nhắn nhóm
- Thân yêu cầu: multipart/form-data
  - `conversationId` (string) - bắt buộc
  - `content` (string)
  - `image` (binary file) - tùy chọn
- Trả về:
  - `200` gửi thành công
  - `403` nếu user không thuộc nhóm

## Payments

> Tất cả endpoint yêu cầu `Authorization: Bearer <accessToken>`.

### 19. PUT /payments/bank-setup
- Mô tả: Lưu thông tin tài khoản ngân hàng cho user hiện tại
- Thân yêu cầu (JSON):
  - `accountNo` (string) - số tài khoản, bắt buộc
  - `accountName` (string) - tên chủ tài khoản
  - `acqId` (string) - mã ngân hàng/đơn vị thanh toán, bắt buộc
- Trả về:
  - `200` cập nhật thành công
  - `400` thiếu dữ liệu hoặc `accountNo`/`acqId` không hợp lệ
- Response model mẫu:
  ```json
  {
    "message": "Cập nhật thông tin ngân hàng thành công",
    "user": {
      "accountNo": "123456789",
      "accountName": "NGUYEN VAN A",
      "acqId": "MB"
    }
  }
  ```

### 20. POST /payments/qr
- Mô tả: Tạo mã QR thanh toán dựa trên tài khoản ngân hàng đã lưu của user
- Thân yêu cầu (JSON):
  - `amount` (number|string) - số tiền, bắt buộc
  - `addInfo` (string) - nội dung bổ sung, tùy chọn
- Trả về:
  - `200` tạo mã QR thành công
  - `400` nếu chưa thiết lập tài khoản ngân hàng hoặc số tiền không hợp lệ
- Response model mẫu:
  ```json
  {
    "message": "Tạo mã QR thành công",
    "qrDataURL": "data:image/png;base64,..."
  }
  ```

## Reminders

> Tất cả endpoint yêu cầu `Authorization: Bearer <accessToken>`.

### 21. GET /reminders/
- Mô tả: Lấy danh sách reminder hiện tại của user
- Mô tả thêm: Các reminder quá hạn sẽ bị xóa tự động trước khi trả về.
- Trả về:
  - `200` danh sách reminder

### 22. POST /reminders/
- Mô tả: Tạo reminder mới
- Thân yêu cầu (JSON):
  - `conversationId` (string)
  - `partnerId` (string) - ID người được nhắc
  - `content` (string) - nội dung reminder
  - `dueDate` (string|date) - ngày giờ nhắc
  - `messageId` (string) - ID tin nhắn gốc
  - `creatorId` (string) - tùy chọn, mặc định dùng `req.user._id`
- Trả về:
  - `201` reminder được tạo thành công
  - `200` nếu reminder đã tồn tại với cùng `messageId`

## Lưu ý chung

- Tất cả endpoint thuộc nhóm API riêng (`/api/*`) dùng middleware `protectedRoute` nếu yêu cầu `Authorization`.
- `Authorization` header format:
  - `Authorization: Bearer <accessToken>`
- `POST /auth/refresh` cần refresh token lưu trong cookie; API client phải gửi cookie nếu muốn cấp lại access token.
- Swagger UI hiện tại: `/api-docs`

## Các đường dẫn router chính

- `/api/auth` - Authentication
- `/api/users` - User info
- `/api/friends` - Friend management
- `/api/messages` - Message gửi 1-1 và nhóm
- `/api/conversations` - Conversation và tin nhắn phân trang
- `/api/payments` - Bank setup và tạo QR
- `/api/reminders` - Reminder

## Gợi ý định danh responses

- Trả về `message` trong nhiều API để hiển thị lỗi/đã thành công.
- `GET /users/me` trả về toàn bộ user hiện tại, bao gồm các trường như `username`, `email`, `displayName`, `avatarUrl`, `bio`, `phone`, `accountNo`, `accountName`, `acqId`.
