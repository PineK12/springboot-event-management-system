package com.example.vadoo.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {
    String action();        // Ví dụ: CREATE, UPDATE, DELETE
    String description();   // Ví dụ: "Tạo người dùng mới"
    String targetTable();   // Ví dụ: "users", "don_vi"
}