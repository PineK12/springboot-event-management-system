package com.example.vadoo.repository;

import com.example.vadoo.entity.Role;
import com.example.vadoo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    @Query("SELECT u FROM User u JOIN FETCH u.role r LEFT JOIN FETCH r.permissions WHERE u.username = :username")
    Optional<User> findByUsernameWithRoleAndPermissions(@Param("username") String username);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByUsernameContainingOrEmailContaining(String username, String email, Pageable pageable);

    Page<User> findByUsernameContainingOrEmailContainingAndRole(String username, String email, Role role, Pageable pageable);

    long countByIsActive(Boolean isActive);

    // UserRepository.java
    long countByRole_TenRole(String tenRole);

    // 1. Hàm tìm tất cả User ngoại trừ role cụ thể (Dùng cho mặc định)
    Page<User> findByRole_TenRoleNot(String tenRole, Pageable pageable);

    // 2. Hàm tìm theo từ khóa (Tên hoặc Email) VÀ loại trừ role cụ thể
    // Sử dụng @Query để đảm bảo logic (A OR B) AND C không bị sai thứ tự ưu tiên
    @Query("SELECT u FROM User u WHERE (u.username LIKE %:keyword% OR u.email LIKE %:keyword%) AND u.role.tenRole <> :excludedRole")
    Page<User> searchByKeywordAndExcludeRole(@Param("keyword") String keyword, @Param("excludedRole") String excludedRole, Pageable pageable);

    // Query để eager load SinhVien và Btc
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.sinhVien sv " +
            "LEFT JOIN FETCH sv.donVi " +
            "LEFT JOIN FETCH u.btc b " +
            "LEFT JOIN FETCH b.donVi " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.sinhVien sv " +
            "LEFT JOIN FETCH sv.donVi " +
            "LEFT JOIN FETCH u.btc b " +
            "LEFT JOIN FETCH b.donVi " +
            "LEFT JOIN FETCH u.role " +
            "WHERE u.username = :username")
    Optional<User> findByUsernameWithDetails(@Param("username") String username);
}