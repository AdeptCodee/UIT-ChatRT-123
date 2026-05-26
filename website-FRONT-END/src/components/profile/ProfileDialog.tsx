import type { Dispatch, SetStateAction } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../ui/dialog";
import ProfileCard from "./ProfileCard";
import { useAuthStore } from "@/stores/useAuthStore";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import PersonalInfoForm from "./PersonalInfoForm.tsx";
import PreferencesForm from "./PreferencesForm.tsx";
import PrivacySettings from "./PrivacySettings";

interface ProfileDialogProps {
  open: boolean;
  setOpen: Dispatch<SetStateAction<boolean>>;
}

const ProfileDialog = ({ open, setOpen }: ProfileDialogProps) => {
  const { user } = useAuthStore();
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      {/* ĐÃ SỬA 1: Thêm w-[95vw] và sm:max-w-4xl để mở rộng khung Dialog, phá bỏ giới hạn mặc định của shadcn */}
      <DialogContent className="overflow-y-auto max-h-[95vh] w-[95vw] sm:max-w-4xl p-0 bg-transparent border-0 shadow-2xl">
        <div className="bg-gradient-glass">
          <div className="max-w-4xl mx-auto p-4">
            {/* heading */}
            <DialogHeader className="mb-6">
              <DialogTitle className="text-2xl font-bold text-foreground">
                Profile & Settings
              </DialogTitle>
            </DialogHeader>

            <ProfileCard user={user} />

            {/* ĐÃ SỬA 2: Thêm w-full, flex và flex-col để ép thanh Tabs nằm trên và Form nằm dưới */}
            <Tabs
              defaultValue="personal"
              className="w-full flex flex-col space-y-4 my-4"
            >
              <TabsList className="grid w-full grid-cols-3 glass-light">
                <TabsTrigger
                  value="personal"
                  className="data-[state=active]:glass-strong"
                >
                  Tài Khoản
                </TabsTrigger>
                <TabsTrigger
                  value="preferences"
                  className="data-[state=active]:glass-strong"
                >
                  Cấu Hình
                </TabsTrigger>
                <TabsTrigger
                  value="privacy"
                  className="data-[state=active]:glass-strong"
                >
                  Bảo Mật
                </TabsTrigger>
              </TabsList>

              {/* ĐÃ SỬA 3: Đảm bảo các nội dung bên trong Tab cũng mở rộng tối đa 100% */}
              <TabsContent value="personal" className="w-full">
                <PersonalInfoForm userInfo={user} />
              </TabsContent>

              <TabsContent value="preferences" className="w-full">
                <PreferencesForm />
              </TabsContent>

              <TabsContent value="privacy" className="w-full">
                <PrivacySettings />
              </TabsContent>
            </Tabs>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default ProfileDialog;
