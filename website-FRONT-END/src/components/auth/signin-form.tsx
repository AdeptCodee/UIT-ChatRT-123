import { cn } from "@/lib/utils";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useAuthStore } from "@/stores/useAuthStore";
import { useNavigate } from "react-router";

const signInSchema = z.object({
  username: z
    .string()
    .min(3, { message: "Tên đăng nhập phải có ít nhất 3 ký tự" })
    .max(20, { message: "Tên đăng nhập không được quá 20 ký tự" })
    .regex(/^[a-zA-Z0-9_]+$/, {
      message: "Tên đăng nhập chỉ được chứa chữ cái, số và gạch dưới",
    }),
  password: z.string().min(6, { message: "Mật khẩu phải có ít nhất 6 ký tự" }),
});

type SignInFormValues = z.infer<typeof signInSchema>;

export function SignInForm({
  className,
  ...props
}: React.ComponentProps<"div">) {
  const { signIn } = useAuthStore();
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignInFormValues>({
    resolver: zodResolver(signInSchema),
  });

  const onSubmit = async (data: SignInFormValues) => {
    const { username, password } = data;

    // Gọi backend API để đăng nhập
    await signIn(username, password);

    // Kiểm tra xem đăng nhập có thành công hay không bằng cách kiểm tra accessToken
    const { accessToken } = useAuthStore.getState();
    if (accessToken) {
      navigate("/"); // Chỉ chuyển hướng nếu đăng nhập thành công
    }
  };

  return (
    <div className={cn("flex flex-col gap-4", className)} {...props}>
      <Card className="overflow-hidden p-0 border-border">
        <CardContent className="grid p-0 md:grid-cols-2">
          {/* Đã giảm padding từ p-6 md:p-8 xuống p-4 md:p-6 */}
          <form className="p-4 md:p-6" onSubmit={handleSubmit(onSubmit)}>
            {/* Đã giảm gap-6 xuống gap-4 để các nhóm sát lại hơn */}
            <div className="flex flex-col gap-4">
              {/* Header */}
              <div className="flex flex-col items-center text-center gap-1">
                <a href="/" className="mx-auto block w-fit text-center">
                  {/* Thêm giới hạn chiều cao h-12 cho logo để tránh bị chiếm diện tích */}
                  <img src="/logo.png" alt="Logo" className="h-12 w-auto" />
                </a>
                <h1 className="text-xl font-bold">Đăng nhập vào ChatRT</h1>
                <p className="text-sm text-muted-foreground text-balance">
                  Nhập thông tin của bạn để đăng nhập
                </p>
              </div>

              {/* Username */}
              {/* Đổi từ thẻ flex gap-3 sang space-y-1 cho chuẩn form */}
              <div className="space-y-1">
                <Label htmlFor="username" className="block text-sm">
                  Tên đăng nhập
                </Label>
                <Input
                  type="text"
                  id="username"
                  placeholder="username123"
                  className="h-9"
                  {...register("username")}
                />
                {errors.username && (
                  <p className="text-sm text-destructive">
                    {errors.username.message}
                  </p>
                )}
              </div>

              {/* Password */}
              <div className="space-y-1">
                <Label htmlFor="password" className="block text-sm">
                  Mật khẩu
                </Label>
                <Input
                  type="password"
                  id="password"
                  placeholder="12345678"
                  className="h-9"
                  {...register("password")}
                />
                {errors.password && (
                  <p className="text-sm text-destructive">
                    {errors.password.message}
                  </p>
                )}
              </div>

              {/* Nút đăng nhập */}
              <Button
                type="submit"
                className="w-full mt-2 h-9"
                disabled={isSubmitting}
              >
                Đăng nhập
              </Button>

              <div className="text-center text-sm">
                Chưa có tài khoản?{" "}
                <a
                  href="/signup"
                  className="underline underline-offset-4 hover:text-primary"
                >
                  Đăng ký
                </a>
              </div>
            </div>
          </form>

          {/* Phần ảnh bên phải */}
          <div className="relative hidden bg-muted md:block">
            <img
              src="/placeholder.png"
              alt="Image"
              // Thêm h-full w-full để ảnh tự căn chỉnh lấp đầy thẻ Card khi form ngắn lại
              className="absolute top-1/2 -translate-y-1/2 object-cover h-full w-full"
            />
          </div>
        </CardContent>
      </Card>

      <div className="text-xs text-balance px-6 text-center text-muted-foreground *:[a]:hover:text-primary *:[a]:underline *:[a]:underline-offset-4">
        Bằng cách tiếp tục, bạn đồng ý với <a href="#">Điều khoản Dịch vụ</a> và{" "}
        <a href="#">Chính sách Bảo mật</a> của chúng tôi.
      </div>
    </div>
  );
}
