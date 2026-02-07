-- ==========================================================
-- 1. BẢNG GIỚI HẠN SỨC MẠNH (POWER LIMITS)
-- Dùng để nâng giới hạn chỉ số gốc (HP/MP/SD...) tại NPC Quốc Vương.
-- ==========================================================
DROP TABLE IF EXISTS `power_limit`;
CREATE TABLE `power_limit` (
  `id` int(11) NOT NULL,
  `power` bigint(20) NOT NULL,
  `hp` bigint(20) NOT NULL,
  `mp` bigint(20) NOT NULL,
  `damage` bigint(20) NOT NULL,
  `defense` int(11) NOT NULL,
  `critical` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `power_limit` (`id`, `power`, `hp`, `mp`, `damage`, `defense`, `critical`) VALUES
(1, 17999999999, 220000, 220000, 11000, 550, 5),
(2, 19999999999, 240000, 240000, 12000, 600, 6),
(3, 24999999999, 300000, 300000, 15000, 700, 7),
(4, 29999999999, 350000, 350000, 18000, 800, 8),
(5, 39999999999, 400000, 400000, 20000, 1000, 9),
(6, 50010000000, 450000, 450000, 22000, 1200, 10),
(7, 60010000000, 500000, 500000, 24000, 1400, 10),
(8, 70010000000, 525000, 525000, 24500, 1500, 10),
(9, 80010000000, 550000, 550000, 25000, 1600, 10),
(10, 90010000000, 575000, 575000, 26000, 1700, 10),
(11, 100010000000, 600000, 600000, 27500, 1800, 10),
(12, 130010000000, 625000, 625000, 30000, 2000, 10),
(13, 150010000000, 655000, 655000, 32000, 5000, 10),
(14, 280010000000, 700000, 700000, 35000, 10000, 10),
(15, 5000010000000, 750000, 750000, 40000, 15000, 12),
(16, 5000010000000, 750000, 750000, 40000, 15000, 12);


-- ==========================================================
-- 2. BẢNG DANH HIỆU (PLAYER CAPTIONS)
-- Dùng để hiển thị danh hiệu trên đầu nhân vật dựa trên sức mạnh.
-- ==========================================================
DROP TABLE IF EXISTS `power_caption`;
CREATE TABLE `power_caption` (
  `id` int(11) NOT NULL,
  `power_required` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `power_caption` (`id`, `power_required`, `name`) VALUES
(0, 0, 'Tân thủ'),
(1, 3000, 'Tập sự sơ cấp'),
(2, 15000, 'Tập sự trung cấp'),
(3, 40000, 'Tập sự cao cấp'),
(4, 90000, 'Tân binh'),
(5, 170000, 'Chiến binh'),
(6, 340000, 'Chiến binh cao cấp'),
(7, 700000, 'Vệ binh'),
(8, 1500000, 'Vệ binh hoàng gia'),
(9, 15000000, 'Siêu {planet} cấp 1'),
(10, 150000000, 'Siêu {planet} cấp 2'),
(11, 1500000000, 'Siêu {planet} cấp 3'),
(12, 5000000000, 'Siêu {planet} cấp 4'),
(13, 10000000000, 'Thần {planet} cấp 1'),
(14, 40000000000, 'Thần {planet} cấp 2'),
(15, 50010000000, 'Thần {planet} cấp 3'),
(16, 60010000000, 'Giới Vương Thần cấp 1'),
(17, 70010000000, 'Giới Vương Thần cấp 2'),
(18, 80010000000, 'Giới Vương Thần cấp 3'),
(19, 100000000000, 'Thần hủy diệt cấp 1'),
(20, 110000000000, 'Thần hủy diệt cấp 2'),
(21, 120000000000, 'Thiên đạo'),
(22, 130000000000, 'Kẻ hũy diệt vũ trụ'),
(23, 140000000000, 'Vô địch đa giới'),
(24, 150000000000, 'Đức Pro'),
(25, 160000000000, 'Huyền thoại'),
(26, 170000000000, 'Thượng cổ'),
(27, 180000000000, 'Sáng thế thần');
