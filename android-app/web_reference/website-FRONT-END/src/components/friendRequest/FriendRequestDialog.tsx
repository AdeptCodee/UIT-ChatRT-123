import { useEffect, useState, type Dispatch, type SetStateAction } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useFriendStore } from "@/stores/useFriendStore";
import SentRequests from "./SentRequests";
import ReceivedRequests from "./ReceivedRequests";

interface FriendRequestDialogProps {
  open: boolean;
  setOpen: Dispatch<SetStateAction<boolean>>;
}

const FriendRequestDialog = ({ open, setOpen }: FriendRequestDialogProps) => {
  const [tab, setTab] = useState("received");
  const { getAllFriendRequests } = useFriendStore();

  useEffect(() => {
    const loadRequest = async () => {
      try {
        await getAllFriendRequests();
      } catch (error) {
        console.error("Lỗi xảy ra khi load requests", error);
      }
    };

    loadRequest();
  }, []);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      {/* ĐÃ SỬA: Thêm bo góc lớn (rounded-2xl) và bỏ viền (border-0) cho hộp thoại */}
      <DialogContent className="sm:max-w-md rounded-2xl border-0 p-6 shadow-2xl">
        <DialogHeader className="pb-2">
          <DialogTitle className="text-xl font-bold">
            Lời mời kết bạn
          </DialogTitle>
        </DialogHeader>

        <Tabs
          value={tab}
          onValueChange={setTab}
          className="w-full flex flex-col"
        >
          {/* ĐÃ SỬA: Làm thanh bọc bên ngoài bo tròn dạng viên thuốc (rounded-full) */}
          <TabsList className="grid w-full grid-cols-2 bg-muted p-1 rounded-full h-12 mb-2">
            {/* ĐÃ SỬA: Định dạng lại nút Tab, khi được chọn (active) sẽ có viền tím, nền trắng và bo tròn */}
            <TabsTrigger
              value="received"
              className="rounded-full font-medium data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:border-2 data-[state=active]:border-primary data-[state=active]:shadow-sm transition-all"
            >
              Đã nhận
            </TabsTrigger>

            <TabsTrigger
              value="sent"
              className="rounded-full font-medium data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:border-2 data-[state=active]:border-primary data-[state=active]:shadow-sm transition-all"
            >
              Đã gửi
            </TabsTrigger>
          </TabsList>

          <TabsContent value="received" className="mt-0 outline-none border-0">
            <ReceivedRequests />
          </TabsContent>

          <TabsContent value="sent" className="mt-0 outline-none border-0">
            <SentRequests />
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
};

export default FriendRequestDialog;
