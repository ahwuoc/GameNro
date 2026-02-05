-- GameNro Boss Data SQL (Back to JSON stages format)
-- Cấu trúc bảng và dữ liệu mẫu (Sửa lại để khớp với code sau khi revert)

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for boss_template
-- ----------------------------
DROP TABLE IF EXISTS `boss_template`;
CREATE TABLE `boss_template` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `type` varchar(50) DEFAULT 'solo',
  `gender` tinyint(4) DEFAULT '2',
  `map_join` json DEFAULT NULL,
  `seconds_rest` int(11) DEFAULT '600',
  `stages` json DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of boss_template
-- ----------------------------

-- Android 19 (Solo)
INSERT INTO `boss_template` (`id`, `name`, `type`, `gender`, `map_join`, `seconds_rest`, `stages`) VALUES 
('android_19', 'Số 19', 'solo', 2, '[92, 93, 94]', 600, '[{"hp":800000,"mp":100000,"dame":12000,"outfit":[247,248,249,-1,-1,-1],"skills":[[1,7,1000],[4,5,2000]],"chat":{"s":["|-1|Năng lượng của các ngươi sẽ thuộc về ta","|-1|Tính toán cho thấy khả năng thắng của ngươi là 0%"],"e":["|-1|Pin... yếu... quá..."]}}]');

-- Kuku (Solo)
INSERT INTO `boss_template` (`id`, `name`, `type`, `gender`, `map_join`, `seconds_rest`, `stages`) VALUES 
('boss_kuku', 'Kuku', 'solo', 2, '[68, 69, 70, 71, 72]', 600, '[{"hp":500000,"mp":100000,"dame":9000,"outfit":[159,160,161,-1,-1,-1],"skills":[[1,3,1000],[4,7,1000]],"chat":{"s":["|-1|Tao đã có lệnh của đại ca Fide rồi","|-1|Mày yếu đi đó, nhìn máy đo đi"],"m":["|-1|Tao đã có lệnh của đại ca Fide rồi","|-1|Mày yếu đi đó, nhìn máy đo đi"],"e":["|-1|Được lắm, quân tử trả thù 10 năm chưa muộn"]}}]');

-- Broly (Scripts)
INSERT INTO `boss_template` (`id`, `name`, `type`, `gender`, `map_join`, `seconds_rest`, `stages`) VALUES 
('boss_broly', 'Broly', 'scripts', 2, '[5, 13, 20]', 1800, '[{"hp":10000000,"mp":5000000,"dame":50000,"outfit":[291,292,293,-1,-1,-1],"skills":[[1,7,500]],"chat":{"s":["|-1|Kakarot... KAKAROT!!!!"],"m":["|-1|Sức mạnh của ta là vô hạn!!"]}}]');

-- Xên Bọ Hung 1 (Sequence Start -> calls boss_cell_2 -> boss_cell_3)
INSERT INTO `boss_template` (`id`, `name`, `type`, `gender`, `map_join`, `seconds_rest`, `stages`) VALUES 
('boss_cell_1', 'Xên Bọ Hung', 'sequence', 2, '[100]', 1200, '[{"hp":1500000,"mp":200000,"dame":18000,"outfit":[174,175,176,-1,-1,-1],"skills":[[0,7,1000],[1,7,1000]],"chat":{"s":["|-1|Thế giới này sắp thuộc về ta"],"e":["|-1|Không thể nào..."]},"together":["boss_cell_2","boss_cell_3"]}]');

-- Xên Bọ Hung 2 (Called by Cell 1)
INSERT INTO `boss_template` (`id`, `name`, `type`, `gender`, `map_join`, `seconds_rest`, `stages`) VALUES 
('boss_cell_2', 'Xên Bọ Hung 2', 'scripts', 2, '[92, 93, 94]', 600, '[{"hp":5000000,"mp":1000000,"dame":35000,"outfit":[231,232,233,-1,-1,-1],"skills":[[1,7,1000],[2,7,2000]],"chat":{"s":["|-1|Sức mạnh thật tuyệt vời!"],"e":["|-1|Ta sẽ đạt đến trạng thái hoàn hảo!"]}}]');

-- Xên Bọ Hung 3 (Called by Cell 2)
INSERT INTO `boss_template` (`id`, `name`, `type`, `gender`, `map_join`, `seconds_rest`, `stages`) VALUES 
('boss_cell_3', 'Xên Bọ Hung 3', 'scripts', 2, '[30]', 300, '[{"hp":15000000,"mp":5000000,"dame":60000,"outfit":[234,235,236,-1,-1,-1],"skills":[[1,7,1000],[2,7,2000],[3,7,3000]],"chat":{"s":["|-1|Giờ thì không ai cản nổi ta!"],"m":["|-1|Dáng đứng của ta đẹp không?"]}}]');

-- Ginyu (Group Leader -> calls Recoome, Burter)
INSERT INTO `boss_template` (`id`, `name`, `type`, `gender`, `map_join`, `seconds_rest`, `stages`) VALUES 
('boss_ginyu', 'Ginyu', 'group', 2, '[68, 69, 70]', 900, '[{"hp":2000000,"mp":500000,"dame":20000,"outfit":[174,175,176,-1,-1,-1],"skills":[[1,7,1000]],"chat":{"s":["|-1|Tiểu đội sát thủ... XUẤT QUÂN!!"]},"together":["boss_recoome","boss_burter"]}]');

-- Recoome (Member)
INSERT INTO `boss_template` (`id`, `name`, `type`, `gender`, `map_join`, `seconds_rest`, `stages`) VALUES 
('boss_recoome', 'Recoome', 'solo', 2, '[68, 69, 70]', 600, '[{"hp":1000000,"mp":200000,"dame":15000,"outfit":[171,172,173,-1,-1,-1],"skills":[[1,7,1000]],"chat":{"s":["|-1|Chuẩn bị ăn đòn đi nhóc"]}}]');

-- Burter (Member)
INSERT INTO `boss_template` (`id`, `name`, `type`, `gender`, `map_join`, `seconds_rest`, `stages`) VALUES 
('boss_burter', 'Burter', 'solo', 2, '[68, 69, 70]', 600, '[{"hp":1200000,"mp":300000,"dame":16000,"outfit":[177,178,179,-1,-1,-1],"skills":[[1,7,1000]],"chat":{"m":["|-1|Ta là người nhanh nhất thiên hà!!"]}}]');

SET FOREIGN_KEY_CHECKS = 1;
