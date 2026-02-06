package com.example.vadoo.aop;

import com.example.vadoo.service.admin.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingAspect {

    private final AdminLogService adminLogService;

    // Chạy sau khi method return thành công
    @AfterReturning(pointcut = "@annotation(logActivity)", returning = "result")
    public void logActivity(JoinPoint joinPoint, LogActivity logActivity, Object result) {
        try {
            // 1. Lấy Username hiện tại
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null) ? auth.getName() : "system";

            // 2. Lấy Request để lấy IP
            HttpServletRequest request = null;
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    request = attributes.getRequest();
                }
            } catch (Exception ignored) {}

            // 3. Xác định Target ID (ID của đối tượng vừa tạo/sửa)
            String targetId = extractTargetId(result, joinPoint);

            // 4. Lấy dữ liệu Input (Làm newValue)
            Object newValue = null;
            if (joinPoint.getArgs().length > 0) {
                newValue = joinPoint.getArgs()[joinPoint.getArgs().length - 1]; // Thường DTO là tham số cuối
            }

            // 5. GỌI LOG SERVICE CỦA BẠN
            adminLogService.logAction(
                    username,
                    logActivity.action(),
                    logActivity.targetTable(),
                    targetId,
                    logActivity.description(),
                    null,       // Old Value (Khó lấy tự động, tạm để null)
                    newValue,   // New Value (Là DTO truyền vào)
                    request
            );

        } catch (Exception e) {
            log.error("AOP Logging failed: {}", e.getMessage());
        }
    }

    // Helper: Cố gắng lấy ID từ kết quả trả về hoặc tham số đầu vào
    private String extractTargetId(Object result, JoinPoint joinPoint) {
        // Cách 1: Nếu hàm trả về Object có method getId() (Ví dụ UserDTO, Entity)
        if (result != null) {
            try {
                Method getId = result.getClass().getMethod("getId");
                Object id = getId.invoke(result);
                return String.valueOf(id);
            } catch (Exception ignored) {}
        }

        // Cách 2: Nếu hàm trả về void (ví dụ delete), lấy tham số đầu tiên (thường là ID)
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Integer) {
            return String.valueOf(args[0]);
        }
        if (args.length > 0 && args[0] instanceof Long) {
            return String.valueOf(args[0]);
        }

        return "UNKNOWN";
    }
}