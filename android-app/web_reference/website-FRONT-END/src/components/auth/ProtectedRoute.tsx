import { Navigate, Outlet } from "react-router";
import { useAuthStore } from "@/stores/useAuthStore";
import { useEffect, useState } from "react";

const ProtectedRoute = () => {
  const { accessToken, user, loading, refresh, fetchMe } = useAuthStore();
  const [starting, setStarting] = useState(true);

  const init = async () => {
    try {
      // Có thể xảy ra khi refresh trang 
      if (!accessToken) {
        await refresh();
      }
      
      // Kiểm tra lại sau khi refresh, nếu vẫn không có accessToken thì không cho vào
      const currentToken = useAuthStore.getState().accessToken;
      if (!currentToken) {
        setStarting(false);
        return;
      }
      
      if (currentToken && !user) {
        await fetchMe();
      }
    } catch (error) {
      console.error("Error in ProtectedRoute init:", error);
    }
    setStarting(false);
  }
  
  useEffect(() => {
    init();
  }, [])
  
  if (starting || loading) {
    return <div className="flex h-screen items-center justify-center">Đang tải trang... </div>
  }
  
  // Kiểm tra lại token hiện tại (không phải từ closure)
  const finalToken = useAuthStore.getState().accessToken;
  if (!finalToken) {
    return (<Navigate to="/signin" replace />)
  }
  
  return (<Outlet></Outlet>)
}

export default ProtectedRoute;
