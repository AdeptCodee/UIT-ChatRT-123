import { Button } from "../ui/button";
import { useAuthStore } from "@/stores/useAuthStore";
import { LogOut } from "lucide-react";
import { useNavigate } from "react-router";

const Logout = () => {
  const { signOut } = useAuthStore();
  const navigate = useNavigate();
  const handleLogout = async () => {
    try {
      await signOut();
      navigate("/signin"); // Chuyển hướng đến trang đăng nhập sau khi đăng xuất thành công
    } catch (error) {
      console.error("Error during logout:", error);
    }
  };
  return (
    <Button variant="completeGhost" onClick={handleLogout}>
      <LogOut className="text-destructive" />
      Log out
    </Button>
  );
};
export default Logout;
