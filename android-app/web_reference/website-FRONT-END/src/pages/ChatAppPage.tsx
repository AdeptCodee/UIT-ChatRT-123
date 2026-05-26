
import ChatWindowLayout from "@/components/chat/ChatWindowLayout";
import { AppSidebar } from "@/components/sidebar/app-sidebar";
import { SidebarProvider } from "@/components/ui/sidebar";
// THÊM DÒNG IMPORT NÀY
import { TooltipProvider } from "@/components/ui/tooltip";

const ChatAppPage = () => {
  return (
    /* BỌC THÊM THẺ TOOLTIP PROVIDER Ở NGOÀI CÙNG */
    <TooltipProvider>
      <SidebarProvider>
        <AppSidebar />

        <div className="flex h-screen w-full p-2">
          <ChatWindowLayout />
        </div>
      </SidebarProvider>
    </TooltipProvider>
  );
};

export default ChatAppPage;
