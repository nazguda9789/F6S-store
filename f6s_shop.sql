-- =====================================================
-- WEBSITE QUẢN LÝ BÁN GIÀY THỂ THAO F6S
-- Hệ quản trị CSDL: MySQL / MariaDB
-- =====================================================

-- Xóa database cũ nếu tồn tại và tạo lại
DROP DATABASE IF EXISTS f6s_shop;
CREATE DATABASE f6s_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Chọn database để sử dụng
USE f6s_shop;

-- 1. TÀI KHOẢN (dùng chung cho Admin/Nhân viên và Khách hàng)
CREATE TABLE tai_khoan (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    ten_dang_nhap  VARCHAR(50)  NOT NULL UNIQUE,
    mat_khau       VARCHAR(255) NOT NULL,
    vai_tro        VARCHAR(20)  NOT NULL DEFAULT 'KHACH_HANG' CHECK (vai_tro IN ('ADMIN','NHAN_VIEN','KHACH_HANG')),
    trang_thai     TINYINT      NOT NULL DEFAULT 1,        -- 1: hoạt động, 0: khóa
    ngay_tao       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. NHÂN VIÊN
CREATE TABLE nhan_vien (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    ma_nhan_vien   VARCHAR(20)  NOT NULL UNIQUE,
    ho_ten         VARCHAR(100) NOT NULL,
    gioi_tinh      TINYINT,
    ngay_sinh      DATE,
    so_dien_thoai  VARCHAR(15) UNIQUE,
    email          VARCHAR(100) UNIQUE,
    dia_chi        VARCHAR(255),
    trang_thai     TINYINT NOT NULL DEFAULT 1,
    tai_khoan_id   INT NOT NULL UNIQUE,
    FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id)
);

-- 3. KHÁCH HÀNG
CREATE TABLE khach_hang (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    ma_khach_hang  VARCHAR(20)  NOT NULL UNIQUE,
    ho_ten         VARCHAR(100) NOT NULL,
    gioi_tinh      TINYINT,
    ngay_sinh      DATE,
    so_dien_thoai  VARCHAR(15),
    email          VARCHAR(100),
    dia_chi        VARCHAR(255),
    trang_thai     TINYINT NOT NULL DEFAULT 1,
    tai_khoan_id   INT UNIQUE,                               -- NULL = khách mua tại quầy
    FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id)
);

-- 4. DANH MỤC
CREATE TABLE danh_muc (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    ten_danh_muc   VARCHAR(100) NOT NULL UNIQUE,
    mo_ta          VARCHAR(255),
    trang_thai     TINYINT NOT NULL DEFAULT 1
);

-- 5. SẢN PHẨM
CREATE TABLE san_pham (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    ma_san_pham    VARCHAR(20)  NOT NULL UNIQUE,
    ten_san_pham   VARCHAR(200) NOT NULL,
    thuong_hieu    VARCHAR(100),
    mo_ta          TEXT,
    gia_ban        DECIMAL(15,0) NOT NULL CHECK (gia_ban >= 0),
    hinh_anh       VARCHAR(255),
    trang_thai     TINYINT NOT NULL DEFAULT 1,
    ngay_tao       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    danh_muc_id    INT NOT NULL,
    FOREIGN KEY (danh_muc_id) REFERENCES danh_muc(id)
);

-- 5b. SẢN PHẨM CHI TIẾT
CREATE TABLE san_pham_chi_tiet (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    san_pham_id    INT NOT NULL,
    kich_co        VARCHAR(10) NOT NULL,
    mau_sac        VARCHAR(50) NOT NULL,
    so_luong_ton   INT NOT NULL DEFAULT 0 CHECK (so_luong_ton >= 0),
    trang_thai     TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT UQ_SP_Size_Color UNIQUE (san_pham_id, kich_co, mau_sac),
    FOREIGN KEY (san_pham_id) REFERENCES san_pham(id) ON DELETE CASCADE
);

-- 6. GIỎ HÀNG
CREATE TABLE gio_hang (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    khach_hang_id   INT NOT NULL UNIQUE,
    ngay_tao        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(id) ON DELETE CASCADE
);

-- 6b. GIỎ HÀNG CHI TIẾT
CREATE TABLE gio_hang_chi_tiet (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    gio_hang_id     INT NOT NULL,
    san_pham_chi_tiet_id INT NOT NULL,
    so_luong        INT NOT NULL CHECK (so_luong > 0),
    CONSTRAINT UQ_GioHang_SPCT UNIQUE (gio_hang_id, san_pham_chi_tiet_id),
    FOREIGN KEY (gio_hang_id) REFERENCES gio_hang(id) ON DELETE CASCADE,
    FOREIGN KEY (san_pham_chi_tiet_id) REFERENCES san_pham_chi_tiet(id)
);

-- 7. ĐỢT GIẢM GIÁ
CREATE TABLE dot_giam_gia (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    ma_dot           VARCHAR(20)  NOT NULL UNIQUE,
    ten_dot          VARCHAR(150) NOT NULL,
    phan_tram_giam   DECIMAL(5,2) NOT NULL CHECK (phan_tram_giam BETWEEN 0 AND 100),
    ngay_bat_dau     DATETIME NOT NULL,
    ngay_ket_thuc    DATETIME NOT NULL,
    trang_thai       TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT CK_DotGiamGia_Time CHECK (ngay_ket_thuc > ngay_bat_dau)
);

CREATE TABLE dot_giam_gia_san_pham (
    dot_giam_gia_id INT NOT NULL,
    san_pham_id     INT NOT NULL,
    PRIMARY KEY (dot_giam_gia_id, san_pham_id),
    FOREIGN KEY (dot_giam_gia_id) REFERENCES dot_giam_gia(id) ON DELETE CASCADE,
    FOREIGN KEY (san_pham_id) REFERENCES san_pham(id) ON DELETE CASCADE
);

-- 8. PHIẾU GIẢM GIÁ
CREATE TABLE phieu_giam_gia (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    ma_phieu           VARCHAR(20)  NOT NULL UNIQUE,
    ten_phieu          VARCHAR(150) NOT NULL,
    loai_giam          VARCHAR(20)  NOT NULL CHECK (loai_giam IN ('PHAN_TRAM','TIEN_MAT')),
    gia_tri_giam       DECIMAL(15,2) NOT NULL CHECK (gia_tri_giam > 0),
    giam_toi_da        DECIMAL(15,0),
    don_toi_thieu      DECIMAL(15,0) NOT NULL DEFAULT 0,
    so_luong           INT NOT NULL DEFAULT 0,
    ngay_bat_dau       DATETIME NOT NULL,
    ngay_ket_thuc      DATETIME NOT NULL,
    trang_thai         TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT CK_PhieuGiamGia_Time CHECK (ngay_ket_thuc > ngay_bat_dau)
);

-- 9. HÓA ĐƠN
CREATE TABLE hoa_don (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    ma_hoa_don          VARCHAR(20) NOT NULL UNIQUE,
    khach_hang_id       INT,
    nhan_vien_id        INT,
    phieu_giam_gia_id   INT,
    loai_hoa_don        VARCHAR(20) NOT NULL DEFAULT 'ONLINE' CHECK (loai_hoa_don IN ('ONLINE','TAI_QUAY')),
    trang_thai          VARCHAR(30) NOT NULL DEFAULT 'CHO_XAC_NHAN' CHECK (trang_thai IN ('CHO_XAC_NHAN','DA_XAC_NHAN','DANG_GIAO','HOAN_THANH','DA_HUY')),
    phuong_thuc_thanh_toan VARCHAR(20) NOT NULL DEFAULT 'COD' CHECK (phuong_thuc_thanh_toan IN ('TIEN_MAT','CHUYEN_KHOAN','COD')),
    ten_nguoi_nhan      VARCHAR(100),
    sdt_nguoi_nhan      VARCHAR(15),
    dia_chi_giao        VARCHAR(255),
    tong_tien           DECIMAL(15,0) NOT NULL DEFAULT 0,
    tien_giam           DECIMAL(15,0) NOT NULL DEFAULT 0,
    phi_van_chuyen      DECIMAL(15,0) NOT NULL DEFAULT 0,
    tong_thanh_toan     DECIMAL(15,0) NOT NULL DEFAULT 0,
    ngay_tao            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_thanh_toan     DATETIME,
    ghi_chu             VARCHAR(255),
    FOREIGN KEY (khach_hang_id)      REFERENCES khach_hang(id),
    FOREIGN KEY (nhan_vien_id)       REFERENCES nhan_vien(id),
    FOREIGN KEY (phieu_giam_gia_id)  REFERENCES phieu_giam_gia(id)
);

-- 9b. HÓA ĐƠN CHI TIẾT
CREATE TABLE hoa_don_chi_tiet (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    hoa_don_id          INT NOT NULL,
    san_pham_chi_tiet_id INT NOT NULL,
    so_luong            INT NOT NULL CHECK (so_luong > 0),
    don_gia             DECIMAL(15,0) NOT NULL,
    giam_gia            DECIMAL(15,0) NOT NULL DEFAULT 0,
    thanh_tien          DECIMAL(15,0) NOT NULL,
    FOREIGN KEY (hoa_don_id) REFERENCES hoa_don(id) ON DELETE CASCADE,
    FOREIGN KEY (san_pham_chi_tiet_id) REFERENCES san_pham_chi_tiet(id)
);

-- 10. BÀI VIẾT
CREATE TABLE bai_viet (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    tieu_de        VARCHAR(255) NOT NULL,
    noi_dung       LONGTEXT NOT NULL,
    hinh_anh       VARCHAR(255),
    ngay_dang      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trang_thai     TINYINT NOT NULL DEFAULT 1,
    tai_khoan_id   INT NOT NULL,
    FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id)
);

-- INDEX
CREATE INDEX idx_sp_ten      ON san_pham(ten_san_pham);
CREATE INDEX idx_hd_ngay     ON hoa_don(ngay_tao);
CREATE INDEX idx_hd_trangthai ON hoa_don(trang_thai);

-- VIEW: thống kê doanh thu theo ngày
CREATE VIEW v_doanh_thu_ngay AS
SELECT CAST(ngay_tao AS DATE) AS ngay,
       COUNT(*)             AS so_hoa_don,
       SUM(tong_thanh_toan) AS doanh_thu
FROM hoa_don
WHERE trang_thai = 'HOAN_THANH'
GROUP BY CAST(ngay_tao AS DATE);

-- =====================================================
-- DỮ LIỆU MẪU
-- =====================================================
INSERT INTO tai_khoan (id, ten_dang_nhap, mat_khau, vai_tro) VALUES
(1, 'admin',  '123456', 'ADMIN'),
(2, 'nv01',   '123456', 'NHAN_VIEN'),
(3, 'kh01',   '123456', 'KHACH_HANG');

INSERT INTO nhan_vien (ma_nhan_vien, ho_ten, gioi_tinh, so_dien_thoai, email, tai_khoan_id)
VALUES ('NV01', 'Nguyễn Văn A', 1, '0900000001', 'nva@f6s.vn', 2);

INSERT INTO khach_hang (ma_khach_hang, ho_ten, gioi_tinh, so_dien_thoai, email, dia_chi, tai_khoan_id)
VALUES ('KH01', 'Trần Thị B', 0, '0900000002', 'ttb@gmail.com', 'Tây Hồ, Hà Nội', 3);

INSERT INTO gio_hang (khach_hang_id) VALUES (1);

INSERT INTO danh_muc (ten_danh_muc) VALUES ('Giày chạy bộ'), ('Giày bóng đá'), ('Giày bóng rổ'), ('Giày sneaker');

INSERT INTO san_pham (ma_san_pham, ten_san_pham, thuong_hieu, gia_ban, danh_muc_id) VALUES
('SP01', 'Nike Air Zoom Pegasus', 'Nike', 2800000, 1),
('SP02', 'Adidas Predator Elite', 'Adidas', 3500000, 2);

INSERT INTO san_pham_chi_tiet (san_pham_id, kich_co, mau_sac, so_luong_ton) VALUES
(1, '40', 'Đen', 20), (1, '41', 'Trắng', 15), (2, '42', 'Đỏ', 10);

INSERT INTO dot_giam_gia (ma_dot, ten_dot, phan_tram_giam, ngay_bat_dau, ngay_ket_thuc)
VALUES ('DGG01', 'Khai trương F6S', 10, '2026-10-01 00:00:00', '2026-10-31 23:59:59');

INSERT INTO dot_giam_gia_san_pham VALUES (1, 1);

INSERT INTO phieu_giam_gia (ma_phieu, ten_phieu, loai_giam, gia_tri_giam, giam_toi_da, don_toi_thieu, so_luong, ngay_bat_dau, ngay_ket_thuc)
VALUES ('VC50K', 'Giảm 50K', 'TIEN_MAT', 50000, NULL, 500000, 100, '2026-10-01 00:00:00', '2026-12-31 23:59:59');