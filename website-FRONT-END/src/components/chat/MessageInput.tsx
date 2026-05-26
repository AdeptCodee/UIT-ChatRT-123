import { useAuthStore } from "@/stores/useAuthStore";
import { useEffect, useRef, useState } from "react";
import type { Conversation } from "@/types/chat";
import {
  ChevronDown,
  CreditCard,
  ImagePlus,
  QrCode,
  Send,
  Settings,
  X,
} from "lucide-react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import EmojiPicker from "./EmojiPicker";
import { useChatStore } from "@/stores/useChatStore";
import { toast } from "sonner";

// Danh sách ngân hàng VietQR
const BANKS_LIST = [
  { id: "970436", name: "Vietcombank" },
  { id: "970415", name: "VietinBank" },
  { id: "970418", name: "BIDV" },
  { id: "970423", name: "Techcombank" },
  { id: "970432", name: "VPBank" },
  { id: "970422", name: "MB Bank" },
  { id: "970405", name: "Agribank" },
  { id: "970407", name: "VIB" },
  { id: "970427", name: "SHB" },
  { id: "970441", name: "HDBank" },
  { id: "970451", name: "ACB" },
  { id: "970433", name: "SCB" },
  { id: "970443", name: "MSB" },
  { id: "970449", name: "OCB" },
  { id: "970429", name: "Kienlongbank" },
  { id: "970456", name: "IVB" },
  { id: "970458", name: "Eximbank" },
  { id: "970460", name: "TPBank" },
  { id: "970461", name: "Vietbank" },
  { id: "970462", name: "LPBank" },
  { id: "970467", name: "UVB" },
  { id: "970468", name: "Unirule" },
];

const MessageInput = ({ selectedConvo }: { selectedConvo: Conversation }) => {
  const { user } = useAuthStore();
  const { sendDirectMessage, sendGroupMessage } = useChatStore();
  const [value, setValue] = useState("");
  const [selectedImage, setSelectedImage] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [isQRModalOpen, setIsQRModalOpen] = useState(false);
  const [qrTab, setQrTab] = useState<"create" | "setup">("create");
  const [qrAmount, setQrAmount] = useState("");
  const [qrContent, setQrContent] = useState("Chuyen tien");
  const [isGeneratingQR, setIsGeneratingQR] = useState(false);
  const [bankAccountNo, setBankAccountNo] = useState("");
  const [bankAccountName, setBankAccountName] = useState("");
  const [bankACQId, setBankACQId] = useState("");
  const [isBankListOpen, setIsBankListOpen] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const bankListRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!user) return;
    setBankAccountNo(user.accountNo || "");
    setBankAccountName(user.accountName || "");
    setBankACQId(user.acqId || "");
  }, [user]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        bankListRef.current &&
        !bankListRef.current.contains(event.target as Node)
      ) {
        setIsBankListOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelectBank = (bankId: string) => {
    setBankACQId(bankId);
    setIsBankListOpen(false);
  };

  if (!user) return null;

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.type.startsWith("image/")) {
        toast.error("Vui lòng chọn file hình ảnh");
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        toast.error("Kích thước file không được vượt quá 5MB");
        return;
      }
      setSelectedImage(file);
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const clearImage = () => {
    setSelectedImage(null);
    setImagePreview(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const sendMessage = async () => {
    if (!value.trim() && !selectedImage) return;

    const currValue = value;
    const currImage = selectedImage || undefined;
    setValue("");
    clearImage();

    try {
      if (selectedConvo.type === "direct") {
        const participants = selectedConvo.participants;
        const otherUser = participants.filter((p) => p._id !== user._id)[0];
        await sendDirectMessage(otherUser._id, currValue, currImage);
      } else {
        await sendGroupMessage(selectedConvo._id, currValue, currImage);
      }
      toast.success("Gửi tin nhắn thành công!");
    } catch (error) {
      console.error("Failed to send message:", error);
      toast.error("Lỗi xảy ra khi gửi tin nhắn!");
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const handleSaveBankDetails = async () => {
    if (!bankAccountNo || !bankACQId) {
      toast.error("Vui lòng nhập số tài khoản và chọn ngân hàng.");
      return;
    }

    const { accessToken } = useAuthStore.getState();
    if (!accessToken) {
      toast.error("Vui lòng đăng nhập lại!");
      return;
    }

    try {
      const res = await fetch("http://localhost:5001/api/payments/bank-setup", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        credentials: "include",
        body: JSON.stringify({
          accountNo: bankAccountNo,
          accountName: bankAccountName,
          acqId: bankACQId,
        }),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || "Lưu thông tin ngân hàng thất bại");
      }

      toast.success("Lưu tài khoản thành công! Giờ có thể tạo QR.");
      setQrTab("create");
    } catch (error) {
      console.error(error);
      toast.error("Không thể lưu thông tin ngân hàng.");
    }
  };

  const handleSendMoneyQR = async () => {
    if (!qrAmount || Number(qrAmount) <= 0) {
      toast.error("Vui lòng nhập số tiền hợp lệ.");
      return;
    }

    const { accessToken } = useAuthStore.getState();
    if (!accessToken) {
      toast.error("Vui lòng đăng nhập lại!");
      return;
    }

    setIsGeneratingQR(true);
    try {
      const res = await fetch("http://localhost:5001/api/payments/qr", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        credentials: "include",
        body: JSON.stringify({
          amount: Number(qrAmount),
          addInfo: qrContent || "Chuyen tien",
        }),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || "Không thể tạo QR");
      }

      const fetchImage = await fetch(data.qrDataURL);
      const blob = await fetchImage.blob();
      const imageFile = new File([blob], "vietqr-payment.png", {
        type: "image/png",
      });

      const messageText = `💸 Gửi yêu cầu chuyển tiền: ${Number(qrAmount).toLocaleString("vi-VN")} VNĐ. Nội dung: ${qrContent}`;

      if (selectedConvo.type === "direct") {
        const participants = selectedConvo.participants;
        const otherUser = participants.filter((p) => p._id !== user._id)[0];
        await sendDirectMessage(otherUser._id, messageText, imageFile);
      } else {
        await sendGroupMessage(selectedConvo._id, messageText, imageFile);
      }

      toast.success("Đã quăng mã QR vào khung chat!");
      setIsQRModalOpen(false);
      setQrAmount("");
    } catch (error) {
      console.error(error);
      toast.error("Không thể tạo mã QR.");
    } finally {
      setIsGeneratingQR(false);
    }
  };

  return (
    <div className="flex flex-col bg-background p-3 gap-2">
      {imagePreview && (
        <div className="relative w-20 h-20 rounded-lg overflow-hidden bg-muted">
          <img
            src={imagePreview}
            alt="preview"
            className="w-full h-full object-cover"
          />
          <button
            onClick={clearImage}
            className="absolute top-1 right-1 p-1 bg-destructive rounded-full hover:bg-destructive/80"
          >
            <X className="size-3 text-white" />
          </button>
        </div>
      )}

      <div className="flex items-center gap-2 min-h-[56px]">
        <div className="relative">
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            onChange={handleImageSelect}
            className="hidden"
            aria-label="Upload image"
          />
          <Button
            variant="ghost"
            size="icon"
            onClick={() => fileInputRef.current?.click()}
            className="hover:bg-primary/10 transition-smooth"
            title="Gửi hình ảnh"
          >
            <ImagePlus className="size-4" />
          </Button>
        </div>

        <div className="flex-1 relative">
          <Input
            onKeyDown={handleKeyPress}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder="Nhập tin nhắn..."
            className="pr-20 h-9 bg-white border-border/50 focus:border-primary/50 transition-smooth resize-none"
          />
          <div className="absolute right-2 top-1/2 transform -translate-y-1/2 flex items-center gap-1">
            <Button
              asChild
              variant="ghost"
              size="icon"
              className="size-8 hover:bg-primary/10 transition-smooth"
            >
              <div>
                <EmojiPicker
                  onChange={(emoji: string) => setValue(`${value}${emoji}`)}
                />
              </div>
            </Button>
          </div>
        </div>

        <Button
          variant="ghost"
          size="icon"
          onClick={() => setIsQRModalOpen(!isQRModalOpen)}
          className="hover:bg-blue-500/10 transition-smooth"
          title="Tạo QR chuyển tiền"
        >
          <QrCode className="size-4 text-blue-600" />
        </Button>

        <Button
          onClick={sendMessage}
          className="bg-gradient-chat hover:shadow-glow transition-smooth hover:scale-105"
          disabled={!value.trim() && !selectedImage}
        >
          <Send className="size-4 text-white" />
        </Button>
      </div>

      {isQRModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-lg rounded-3xl bg-background p-6 shadow-2xl border border-border">
            <div className="flex items-center justify-between gap-3 mb-4">
              <div>
                <div className="flex items-center gap-2 text-lg font-semibold">
                  <CreditCard className="size-4" />
                  <span>Thanh toán QR VietQR</span>
                </div>
                <p className="text-sm text-muted-foreground">
                  Lưu tài khoản ngân hàng và tạo mã QR nhận tiền.
                </p>
              </div>
              <button
                onClick={() => setIsQRModalOpen(false)}
                className="px-3 hover:bg-destructive/10 hover:text-destructive text-muted-foreground rounded-lg"
              >
                Đóng
              </button>
            </div>

            <div className="grid grid-cols-2 gap-2 mb-6">
              <button
                onClick={() => setQrTab("create")}
                className={`flex items-center justify-center gap-2 rounded-xl border p-3 text-sm font-semibold ${qrTab === "create" ? "border-blue-600 bg-blue-50 text-blue-600" : "border-border bg-muted text-foreground"}`}
              >
                <QrCode className="size-4" /> Tạo QR
              </button>
              <button
                onClick={() => setQrTab("setup")}
                className={`flex items-center justify-center gap-2 rounded-xl border p-3 text-sm font-semibold ${qrTab === "setup" ? "border-blue-600 bg-blue-50 text-blue-600" : "border-border bg-muted text-foreground"}`}
              >
                <Settings className="size-4" /> Cài đặt ngân hàng
              </button>
            </div>

            {qrTab === "create" ? (
              <div className="space-y-4">
                <div className="grid gap-3">
                  <label className="block text-sm font-medium">
                    Số tiền cần nhận
                  </label>
                  <Input
                    type="number"
                    placeholder="Nhập số tiền (VD: 50000)"
                    value={qrAmount}
                    onChange={(e) => setQrAmount(e.target.value)}
                    className="text-sm"
                  />
                </div>
                <div className="grid gap-3">
                  <label className="block text-sm font-medium">
                    Nội dung giao dịch
                  </label>
                  <Input
                    type="text"
                    placeholder="Nội dung (Không dấu)"
                    value={qrContent}
                    onChange={(e) => setQrContent(e.target.value)}
                    className="text-sm"
                  />
                </div>
                <Button
                  onClick={handleSendMoneyQR}
                  disabled={isGeneratingQR}
                  className="w-full bg-blue-600 hover:bg-blue-700 text-white mt-1"
                >
                  {isGeneratingQR ? "Đang xử lý..." : "Gửi yêu cầu nhận tiền"}
                </Button>
                <p className="text-sm text-muted-foreground">
                  Chú ý: người nhận phải quét QR để chuyển khoản vào tài khoản
                  đã cài đặt.
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="grid gap-3">
                  <label className="block text-sm font-medium">
                    Số tài khoản
                  </label>
                  <Input
                    type="text"
                    value={bankAccountNo}
                    onChange={(e) => setBankAccountNo(e.target.value)}
                    className="text-sm"
                  />
                </div>
                <div className="grid gap-3">
                  <label className="block text-sm font-medium">
                    Chủ tài khoản
                  </label>
                  <Input
                    type="text"
                    value={bankAccountName}
                    onChange={(e) => setBankAccountName(e.target.value)}
                    className="text-sm"
                  />
                </div>
                <div className="grid gap-3">
                  <label className="block text-sm font-medium">
                    Mã ngân hàng (ACQ ID)
                  </label>
                  <div className="relative" ref={bankListRef}>
                    <button
                      type="button"
                      onClick={() => setIsBankListOpen(!isBankListOpen)}
                      className="w-full px-3 py-2 text-sm border rounded-md border-border bg-background text-left flex items-center justify-between hover:bg-muted transition-colors"
                    >
                      <span>
                        {bankACQId
                          ? `${bankACQId} - ${BANKS_LIST.find((b) => b.id === bankACQId)?.name || "Không xác định"}`
                          : "Chọn ngân hàng..."}
                      </span>
                      <ChevronDown
                        className={`size-4 transition-transform ${isBankListOpen ? "rotate-180" : ""}`}
                      />
                    </button>
                    {isBankListOpen && (
                      <div className="absolute top-full left-0 right-0 mt-1 border border-border bg-background rounded-md shadow-lg z-50 max-h-64 overflow-y-auto">
                        {BANKS_LIST.map((bank) => (
                          <button
                            key={bank.id}
                            type="button"
                            onClick={() => handleSelectBank(bank.id)}
                            className={`w-full text-left px-3 py-2 text-sm hover:bg-muted transition-colors ${
                              bankACQId === bank.id
                                ? "bg-blue-50 text-blue-600 font-medium"
                                : ""
                            }`}
                          >
                            {bank.id} - {bank.name}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
                <Button
                  onClick={handleSaveBankDetails}
                  className="w-full bg-green-600 hover:bg-green-700 text-white mt-1"
                >
                  Lưu thông tin ngân hàng
                </Button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default MessageInput;
