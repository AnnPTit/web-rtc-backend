-- =============================================
-- DATA MẪU CHO BẢNG USERS
-- Chạy script này trong MySQL Workbench hoặc command line
-- Password cho tất cả user là: password123
-- =============================================

USE english_db;

-- Xóa dữ liệu cũ (nếu cần)
-- TRUNCATE TABLE users;

-- =============================================
-- GIẢNG VIÊN (LECTURER)
-- =============================================

INSERT INTO users (username, password, full_name, email, role, created_at) VALUES
('admin', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Administrator', 'admin@university.edu.vn', 'LECTURER', NOW()),
('nguyenvana', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Nguyễn Văn A', 'nguyenvana@university.edu.vn', 'LECTURER', NOW()),
('tranthib', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Trần Thị B', 'tranthib@university.edu.vn', 'LECTURER', NOW()),
('levanc', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Lê Văn C', 'levanc@university.edu.vn', 'LECTURER', NOW()),
('phamthid', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Phạm Thị D', 'phamthid@university.edu.vn', 'LECTURER', NOW());

-- =============================================
-- HỌC VIÊN (STUDENT)
-- =============================================

INSERT INTO users (username, password, full_name, email, role, created_at) VALUES
('hoangvane', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Hoàng Văn E', 'hoangvane@student.edu.vn', 'STUDENT', NOW()),
('vuthif', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Vũ Thị F', 'vuthif@student.edu.vn', 'STUDENT', NOW()),
('dovang', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Đỗ Văn G', 'dovang@student.edu.vn', 'STUDENT', NOW()),
('ngothih', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Ngô Thị H', 'ngothih@student.edu.vn', 'STUDENT', NOW()),
('dinhvani', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Đinh Văn I', 'dinhvani@student.edu.vn', 'STUDENT', NOW()),
('buithik', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Bùi Thị K', 'buithik@student.edu.vn', 'STUDENT', NOW()),
('dangvanl', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Đặng Văn L', 'dangvanl@student.edu.vn', 'STUDENT', NOW()),
('lyethim', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Lý Thị M', 'lyethim@student.edu.vn', 'STUDENT', NOW()),
('maivann', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Mai Văn N', 'maivann@student.edu.vn', 'STUDENT', NOW()),
('trinhthio', '$2a$10$rDkPvvAFV8kqwvKJzwlCv.tsmFSgPqcMx/nt0IKeaHwf0cKzljyVi', 'Trịnh Thị O', 'trinhthio@student.edu.vn', 'STUDENT', NOW());

-- =============================================
-- KIỂM TRA DỮ LIỆU
-- =============================================

SELECT 'Dữ liệu đã được thêm thành công!' AS message;
SELECT role, COUNT(*) as total FROM users GROUP BY role;
SELECT id, username, full_name, email, role, created_at FROM users ORDER BY role, id;

