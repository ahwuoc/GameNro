-- GameNro Boss Data SQL - FULL VERSION
-- Chuyển đổi từ Source Java sang Rust Format
-- Total: 100+ Bosses

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

-- ==================== NHÓM NAPPA (3 Boss) ====================
INSERT INTO `boss_template` VALUES 
('boss_kuku', 'Kuku', 'solo', 2, '[68, 69, 70, 71, 72]', 600, '[{"hp":500000,"mp":100000,"dame":9000,"outfit":[159,160,161,-1,-1,-1],"skills":[[1,3,1000],[4,7,1000]],"chat":{"s":["|-1|Ta sẽ tàn sát khu này trong vòng 5 phút nữa","|-1|Haha, mày đây rồi"],"m":["|-1|Tao đã có lệnh của đại ca Fide rồi","|-1|Mày yếu đi đó, nhìn máy đo đi"],"e":["|-1|Được lắm, quân tử trả thù 10 năm chưa muộn"]}}]'),

('boss_map_dau_dinh', 'Mập Đầu Đinh', 'solo', 2, '[63, 64, 65, 66, 67]', 600, '[{"hp":1000000,"mp":100000,"dame":10000,"outfit":[165,166,167,-1,-1,-1],"skills":[[2,7,1000],[3,7,10000]],"chat":{"s":["|-1|HAHAHA","|-1|Tao chỉ cần 10 giây để giết hết bọn mày"],"m":["|-1|Được rồi tao sẽ thổi bay hết","|-1|Chết hết đi cho tao"],"e":["|-1|Tao sẽ giết hết bọn mày"]}}]'),

('boss_rambo', 'Rambo', 'solo', 2, '[74, 75, 76, 77]', 600, '[{"hp":1500000,"mp":100000,"dame":12400,"outfit":[162,163,164,-1,-1,-1],"skills":[[2,7,1000],[3,7,10000]],"chat":{"s":["|-1|HAHAHA","|-1|Thấy ta đẹp trai không"],"m":["|-1|Mày sợ tao chưa","|-1|Ta sẽ tàn sát khu này trong vòng 5 phút nữa"],"e":["|-1|Ôi bạn ơi..."]}}]');

-- ==================== TIỂU ĐỘI SÁT THỦ TRÁI ĐẤT (5 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_so4', 'Số 4', 'solo', 2, '[79, 81, 82, 83]', 600, '[{"hp":25000000,"mp":100000,"dame":10000,"outfit":[168,169,170,-1,-1,-1],"skills":[[4,7,1000],[1,7,1000]],"chat":{"s":[],"m":["|-1|Oải rồi hả","|-1|Một mình tao chấp hết tụi bây","|-1|HAHAHA"],"e":["|-1|Fide gọi ta về, ngươi có ngon thì chờ ở đây"]}}]'),

('boss_so3', 'Số 3', 'solo', 2, '[79, 81, 82, 83]', 600, '[{"hp":30000000,"mp":100000,"dame":11000,"outfit":[174,175,176,-1,-1,-1],"skills":[[4,7,1000],[3,4,1000]],"chat":{"s":[],"m":["|-1|Chán","|-1|Đại ca Fide có nhầm không nhỉ","|-1|Chỉ là bọn con nít"],"e":["|-1|Fide gọi ta về"]}}]'),

('boss_so2', 'Số 2', 'solo', 2, '[79, 81, 82, 83]', 600, '[{"hp":30500000,"mp":100000,"dame":12000,"outfit":[171,172,173,-1,-1,-1],"skills":[[2,7,1000],[3,3,3000]],"chat":{"s":[],"m":["|-1|Ê cố lên nhóc","|-1|HAHAHAHA"],"e":["|-1|Fide gọi ta về"]}}]'),

('boss_so1', 'Số 1', 'solo', 2, '[79, 81, 82, 83]', 600, '[{"hp":40000000,"mp":100000,"dame":12500,"outfit":[177,178,179,-1,-1,-1],"skills":[[4,7,1000],[0,4,10000]],"chat":{"s":[],"m":["|-1|Oải rồi hả","|-1|Chỉ là bọn con nít"],"e":["|-1|Fide gọi ta về"]}}]'),

('boss_tieu_doi_truong', 'Tiểu đội trưởng', 'group', 2, '[79, 81, 82, 83]', 900, '[{"hp":50000000,"mp":100000,"dame":13000,"outfit":[180,181,182,-1,-1,-1],"skills":[[1,7,1000],[2,7,1000]],"chat":{"s":["|-1|Tiểu đội sát thủ XUẤT QUÂN"],"m":["|-1|Chán","|-1|Một mình tao chấp hết tụi bây"],"e":["|-1|Fide gọi ta về"]},"together":["boss_so1","boss_so2","boss_so3","boss_so4"]}]');

-- ==================== TIỂU ĐỘI SÁT THỦ NAMEK (5 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_so4_nm', 'Số 4 Namek', 'solo', 1, '[7, 8, 9, 10, 11, 12, 13, 25, 34, 33, 43]', 600, '[{"hp":20500000,"mp":100000,"dame":10000,"outfit":[168,169,170,-1,-1,-1],"skills":[[4,7,1000],[1,7,1000]],"chat":{"m":["|-1|Oải rồi hả?","|-1|Chỉ là bọn con nít"],"e":["|-1|Cay quá!","|-1|Ta mà lại thua được sao?"]}}]'),

('boss_so3_nm', 'Số 3 Namek', 'solo', 1, '[7, 8, 9, 10, 11, 12, 13, 25, 34, 33, 43]', 600, '[{"hp":30000000,"mp":100000,"dame":10000,"outfit":[174,175,176,-1,-1,-1],"skills":[[4,7,1000],[3,4,1000]],"chat":{"m":["|-1|Chán","|-1|HAHAHAHA"],"e":["|-1|Cay quá!"]}}]'),

('boss_so2_nm', 'Số 2 Namek', 'solo', 1, '[7, 8, 9, 10, 11, 12, 13, 25, 34, 33, 43]', 600, '[{"hp":30500000,"mp":100000,"dame":12200,"outfit":[171,172,173,-1,-1,-1],"skills":[[2,7,1000],[3,3,3000]],"chat":{"m":["|-1|Ê cố lên nhóc","|-1|HAHAHAHA"],"e":["|-1|Hãy trả thù cho ta!"]}}]'),

('boss_so1_nm', 'Số 1 Namek', 'solo', 1, '[7, 8, 9, 10, 11, 12, 13, 25, 34, 33, 43]', 600, '[{"hp":40000000,"mp":100000,"dame":13200,"outfit":[177,178,179,-1,-1,-1],"skills":[[4,7,1000],[0,4,10000]],"chat":{"m":["|-1|Oải rồi hả?","|-1|Một mình tao chấp hết tụi bây"],"e":["|-1|Cay quá!"]}}]'),

('boss_tdt_nm', 'Tiểu đội trưởng Namek', 'group', 1, '[7, 8, 9, 10, 11, 12, 13, 25, 34, 33, 43]', 1800, '[{"hp":50000000,"mp":100000,"dame":15000,"outfit":[180,181,182,-1,-1,-1],"skills":[[1,7,1000],[2,7,1000]],"chat":{"s":[],"m":["|-1|Oải rồi hả?","|-1|HAHAHAHA"],"e":["|-1|Cay quá!"]},"together":["boss_so1_nm","boss_so2_nm","boss_so3_nm","boss_so4_nm"]}]');

-- ==================== FIDE (FRIEZA) - 3 Giai Đoạn ====================
INSERT INTO `boss_template` VALUES
('boss_fide', 'Fide đại ca', 'sequence', 2, '[80]', 600, '[{"name":"Fide đại ca 1","hp":100000000,"mp":100000,"dame":22000,"outfit":[183,184,185,-1,-1,-1],"skills":[[1,7,1000],[2,7,1000]],"chat":{"s":["|-2|Fide với những gì ngươi đã làm với người Xayda và Namek","|-1|Khẩu khí ngang tàng lắm"],"m":["|-1|Các ngươi tới số rồi mới gặp phải ta"],"e":["|-1|Ác quỷ biến hình hây aaaa"]}},{"name":"Fide đại ca 2","hp":200000000,"mp":100000,"dame":25000,"outfit":[186,187,188,-1,-1,-1],"skills":[[1,7,1000],[2,7,1000]],"chat":{"s":["|-1|Hê hê, cẩn thận đi"],"m":["|-1|Oải rồi hả?","|-1|Nhưng tiếc rằng đối thủ của mày lại là Fide này..."],"e":["|-1|Ác quỷ biến hình, Graaaaa...."]}},{"name":"Fide đại ca 3","hp":300000000,"mp":100000,"dame":30000,"outfit":[189,190,191,-1,-1,-1],"skills":[[1,7,1000],[2,7,1000]],"chat":{"s":["|-1|Ta sẽ cho các ngươi thấy đâu mới là sức mạnh của ta"],"m":["|-1|Ta nói các ngươi rồi","|-1|Hô hô hô hô"],"e":["|-1|Lũ khốn","|-1|Nhớ mặt tao đấy"]}}]');

-- ==================== COOLER - 2 Giai Đoạn ====================
INSERT INTO `boss_template` VALUES
('boss_cooler', 'Cooler', 'sequence', 2, '[92, 93, 94, 96, 97, 98, 99]', 600, '[{"hp":1000000000,"mp":1000000,"dame":100000,"outfit":[549,548,547,-1,-1,-1],"skills":[[2,7,1000],[0,7,10000]],"chat":{"s":["|-1|Các ngươi đã giết Fide...","|-1|Hôm nay ta sẽ thay Fide trả thù"],"m":["|-1|Sức mạnh của ta còn mạnh hơn Fide nhiều"],"e":["|-1|Biến hình..."]}},{"hp":1500000000,"mp":1500000,"dame":120000,"outfit":[546,545,544,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":["|-1|Đây mới là sức mạnh thực sự của ta"],"m":["|-1|Hahaha, yếu quá"],"e":["|-1|Không thể nào..."]}}]');

-- ==================== ANDROID (8 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_dr_kore', 'Dr.Kôrê', 'group', 0, '[96, 94, 93]', 600, '[{"hp":150000000,"mp":100000,"dame":12000,"outfit":[255,256,257,-1,-1,-1],"skills":[[0,7,10000],[4,7,1000]],"chat":{"s":["|-2|Chào anh! em đứng đây từ chiều","|-1|Số 19! Xuất chiêu đi nào"],"m":["|-1|Oải rồi hả?","|-1|Mi khá đấy"],"e":[]},"together":["boss_android_19"]}]'),

('boss_android_19', 'Android 19', 'solo', 0, '[96, 94, 93]', 600, '[{"hp":150000000,"mp":100000,"dame":12200,"outfit":[249,250,251,-1,-1,-1],"skills":[[0,7,1000],[4,7,10000]],"chat":{"s":[],"m":["|-1|Oải rồi hả","|-1|Ngươi sẽ không bao giờ thắng được đâu"],"e":[]}}]'),

('boss_android_13', 'Android 13', 'solo', 0, '[104]', 600, '[{"hp":180000000,"mp":100000,"dame":12055,"outfit":[252,253,254,-1,-1,-1],"skills":[[0,7,10000],[4,7,1000]],"chat":{"s":["|-1|Sôn..gôku","|-1|Mục tiêu của bọn ta chỉ là Gôku"],"m":["|-1|Sao thế hả? Ta mới chỉ khởi động thôi mà!"],"e":["|-1|Sô..Sông gôku"]}}]'),

('boss_android_14', 'Android 14', 'group', 0, '[104]', 600, '[{"hp":160000000,"mp":100000,"dame":12000,"outfit":[246,247,248,-1,-1,-1],"skills":[[0,7,10000],[4,7,1000]],"chat":{"s":["|-2|Các ngươi là ai?","|-2|Ta hiểu rồi, các ngươi là rôbốt sát thủ"],"m":[],"e":[]},"together":["boss_android_13","boss_android_15"]}]'),

('boss_android_15', 'Android 15', 'solo', 0, '[104]', 600, '[{"hp":140000000,"mp":100000,"dame":12200,"outfit":[261,262,263,-1,-1,-1],"skills":[[0,7,10000],[4,7,1000]],"chat":{"s":[],"m":[],"e":["|-2|Thì ra vẫn chỉ là một đống sắt vụn!"]}}]'),

('boss_pic', 'Pic', 'solo', 0, '[97, 98, 99]', 600, '[{"hp":200000000,"mp":100000,"dame":17022,"outfit":[237,238,239,-1,-1,-1],"skills":[[1,7,1000],[2,7,1000]],"chat":{"s":["|-1|Chào! Có Gôku ở đây không?","|-2|Biến khỏi đây đi"],"m":["|-1|Ngươi thực sự rất mạnh","|-1|Nhưng ta không quan tâm ngươi là ai"],"e":["|1|Pic tiêu rồi, tớ lên trước nhé!"]}}]'),

('boss_poc', 'Poc', 'solo', 0, '[97, 98, 99]', 600, '[{"hp":220000000,"mp":100000,"dame":18000,"outfit":[240,241,242,-1,-1,-1],"skills":[[1,7,1000],[2,7,1000]],"chat":{"s":[],"m":["|-1|Đừng tưởng ta đây là con gái mà dễ bắt nạt nhé","|-2|Tại sao cô gái xinh đẹp thế này mà lại là rôbốt nhỉ?"],"e":["|-2|Cô gái xinh đẹp vậy mà lại bị tên tiến sĩ Kôrê biến thành người máy.."]}}]'),

('boss_king_kong', 'King Kong', 'group', 0, '[97, 98, 99]', 600, '[{"hp":240000000,"mp":100000,"dame":12000,"outfit":[243,244,245,-1,-1,-1],"skills":[[1,7,1000],[2,7,1000]],"chat":{"s":[],"m":["|-1|Mau đền mạng cho những người bạn của ta","|-1|Sức mạnh của ta chênh nhau với các ngươi một trời một vực đấy!"],"e":[]},"together":["boss_pic","boss_poc"]}]');

-- ==================== CELL (10 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_cell_1', 'Xên bọ hung', 'sequence', 2, '[100]', 1800, '[{"hp":500000000,"mp":200000,"dame":20000,"outfit":[228,229,230,-1,-1,-1],"skills":[[0,7,1000],[0,7,1000],[4,7,10000]],"chat":{"s":["|-2|Cái gì kia vậy Đó là loài gì thế","|-1|Ta sẽ hấp thụ số 17 và 18 để đạt được dạng hoàn hảo"],"m":["|-2|Hắn làm ta bất ngờ đấy","|-1|Đến đây nào"],"e":["|-2|Khốn kiếp hắn bị Cell hấp thu rồi"]}},{"name":"Xên hoàn thiện","hp":750000000,"mp":300000,"dame":25000,"outfit":[231,232,233,-1,-1,-1],"skills":[[0,7,1000],[0,7,5000],[4,7,10000]],"chat":{"s":[],"m":["|-2|Nguy rồi thực sự nguy to rồi","|-1|Các ngươi nghĩ có thể chạy được sao"],"e":["|-1|Đến lúc rồi"]}},{"name":"Xên hoàn thiện","hp":1000000000,"mp":500000,"dame":30000,"outfit":[234,235,236,-1,-1,-1],"skills":[[0,7,1000],[0,7,5000],[4,7,10000]],"chat":{"s":["|-2|Cuối cùng hắn cũng đã biến đổi","|-2|Khốn kiếp Phải kết liễu hắn ngay lúc này"],"m":["|-2|Cell đã đạt đến dạng hoàn hảo rồi","|-1|Xin lỗi Ngươi có thể giúp ta làm nóng cơ thể lên không"],"e":["|-1|Oái không","|-1|Cơ thể hoàn hảo của ta"]}}]'),

('boss_sieu_bo_hung', 'Siêu Bọ Hung', 'group', 2, '[103]', 1800, '[{"hp":1250000000,"mp":500000,"dame":35000,"outfit":[234,235,236,-1,-1,-1],"skills":[[0,7,10000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]},"together":["boss_xen_con_1","boss_xen_con_2","boss_xen_con_3","boss_xen_con_4","boss_xen_con_5","boss_xen_con_6","boss_xen_con_7"]}]'),

('boss_xen_con_1', 'Xên con 1', 'solo', 2, '[103]', 600, '[{"hp":500000000,"mp":100000,"dame":15000,"outfit":[264,265,266,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_xen_con_2', 'Xên con 2', 'solo', 2, '[103]', 600, '[{"hp":500000000,"mp":100000,"dame":15000,"outfit":[264,265,266,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_xen_con_3', 'Xên con 3', 'solo', 2, '[103]', 600, '[{"hp":500000000,"mp":100000,"dame":15000,"outfit":[264,265,266,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_xen_con_4', 'Xên con 4', 'solo', 2, '[103]', 600, '[{"hp":500000000,"mp":100000,"dame":15000,"outfit":[264,265,266,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_xen_con_5', 'Xên con 5', 'solo', 2, '[103]', 600, '[{"hp":500000000,"mp":100000,"dame":15000,"outfit":[264,265,266,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_xen_con_6', 'Xên con 6', 'solo', 2, '[103]', 600, '[{"hp":500000000,"mp":100000,"dame":15000,"outfit":[264,265,266,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_xen_con_7', 'Xên con 7', 'solo', 2, '[103]', 600, '[{"hp":500000000,"mp":100000,"dame":15000,"outfit":[264,265,266,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]');

-- ==================== BLACK GOKU - 2 Giai Đoạn ====================
INSERT INTO `boss_template` VALUES
('boss_black_goku', 'Black Goku', 'sequence', 0, '[102, 92, 93, 94, 96, 97, 98, 99, 100]', 300, '[{"hp":1000000000,"mp":1000000,"dame":50000,"outfit":[550,551,552,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":["|-1|Ta là Sôn Gô Ku","|-1|Cơ thể này,sức mạnh này","|-1|Ta khá thích việc loại bỏ các ngươi"],"m":["|-1|Các ngươi chỉ có vậy thôi sao?","|-1|Đúng là loài người thấp kém"],"e":["|-1|Biến hình! Super Saiyan Rose"]}},{"name":"Super Black Goku","hp":2100000000,"mp":2000000,"dame":100000,"outfit":[553,551,552,-1,-1,-1],"skills":[[0,7,10000],[2,7,1000]],"chat":{"s":[],"m":["|-1|Ta chính là người mang thân thể của Songoku","|-1|Sức mạnh của ta là không có giới hạn"],"e":["|-1|Chúng ta sẽ gặp lại nhau sớm thôi"]}}]');

-- ==================== BROLY - 2 Giai Đoạn ====================
INSERT INTO `boss_template` VALUES
('boss_broly', 'Broly', 'scripts', 2, '[5, 13, 20]', 1800, '[{"hp":10000000,"mp":5000000,"dame":50000,"outfit":[291,292,293,-1,-1,-1],"skills":[[1,7,500]],"chat":{"s":["|-1|Kakarot KAKAROT"],"m":["|-1|Sức mạnh của ta là vô hạn"],"e":[]}}]');

-- ==================== MAJIN BUU (6 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_mabu', 'Mabư mập', 'sequence', 2, '[127]', 600, '[{"hp":2100000000,"mp":1000000,"dame":500000,"outfit":[297,298,299,-1,-1,-1],"skills":[[0,3,5000],[5,7,1000]],"chat":{"s":[],"m":["|-1|Khí công pháo","|-1|Úm ba la xì bùa"],"e":["|-1|Biến hình"]}},{"name":"Super Bư","hp":2100000000,"mp":1000000,"dame":500000,"outfit":[421,422,423,-1,-1,-1],"skills":[[0,3,5000],[5,7,1000]],"chat":{"s":[],"m":["|-1|Khí công pháo"],"e":["|-1|Biến hình"]}},{"name":"Bư Tenk","hp":2100000000,"mp":1000000,"dame":500000,"outfit":[424,425,426,-1,-1,-1],"skills":[[0,3,5000],[5,7,1000]],"chat":{"s":[],"m":["|-1|Ui da đau bụng quá"],"e":["|-1|Biến hình"]}},{"name":"Bư Han","hp":2100000000,"mp":1000000,"dame":500000,"outfit":[427,428,429,-1,-1,-1],"skills":[[0,3,5000],[5,7,1000]],"chat":{"s":[],"m":["|-1|Khí công pháo"],"e":["|-1|Biến hình"]}},{"name":"Kid Bư","hp":2100000000,"mp":1000000,"dame":500000,"outfit":[439,440,441,-1,-1,-1],"skills":[[0,3,5000],[5,7,1000]],"chat":{"s":[],"m":["|-1|Khí công pháo"],"e":[]}}]');

-- ==================== BOJACK GANG (7 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_bojack', 'Bojack', 'group', 0, '[3, 4, 5, 6, 27, 28, 29, 30]', 1800, '[{"hp":2100000000,"mp":1000000,"dame":300000,"outfit":[323,324,325,-1,-1,-1],"skills":[[1,7,1000],[2,7,1000]],"chat":{"s":[],"m":["|-1|Hahaha"],"e":["|-1|Hahaha"]},"together":["boss_bujin","boss_kogu","boss_bido","boss_zangya"]}]'),

('boss_bujin', 'Bujin', 'solo', 0, '[3, 4, 5, 6, 27, 28, 29, 30]', 600, '[{"hp":2100000000,"mp":1000000,"dame":170000,"outfit":[341,342,343,-1,-1,-1],"skills":[[5,7,1000],[1,7,1000]],"chat":{"s":[],"m":["|-1|Oải rồi hả?","|-1|HAHAHAHA"],"e":["|-1|Cay quá!"]}}]'),

('boss_kogu', 'Kogu', 'solo', 0, '[3, 4, 5, 6, 27, 28, 29, 30]', 600, '[{"hp":2100000000,"mp":1000000,"dame":180000,"outfit":[329,330,331,-1,-1,-1],"skills":[[5,7,1000],[3,4,1000]],"chat":{"s":[],"m":["|-1|Trói"],"e":["|-1|Cứu"]}}]'),

('boss_zangya', 'Zangya', 'solo', 0, '[3, 4, 5, 6, 27, 28, 29, 30]', 600, '[{"hp":2100000000,"mp":1000000,"dame":207200,"outfit":[332,333,334,-1,-1,-1],"skills":[[2,7,1000],[3,3,3000]],"chat":{"s":[],"m":["|-1|Trói"],"e":["|-1|Cứu"]}}]'),

('boss_bido', 'Bido', 'solo', 0, '[3, 4, 5, 6, 27, 28, 29, 30]', 600, '[{"hp":2100000000,"mp":1000000,"dame":250200,"outfit":[335,336,337,-1,-1,-1],"skills":[[5,7,1000],[0,4,10000]],"chat":{"s":[],"m":["|-1|Oải rồi hả?","|-1|Chỉ là bọn con nít"],"e":["|-1|Cay quá!"]}}]');

-- ==================== SỰ KIỆN (Event Bosses) ====================

-- Halloween Event (3 Boss)
INSERT INTO `boss_template` VALUES
('boss_ma_troi', 'Ma trời', 'solo', 2, '[0, 1, 2, 3, 4, 5]', 900, '[{"hp":100000000,"mp":100000,"dame":100000,"outfit":[434,435,436,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":["|-1|Ác quỷ sẽ thống trị thế giới"],"e":[]}}]'),

('boss_doi', 'Đồi', 'solo', 2, '[0, 1, 2, 3, 4, 5]', 900, '[{"hp":100000000,"mp":100000,"dame":100000,"outfit":[437,438,439,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":["|-1|Hahaha"],"e":[]}}]'),

('boss_bi_ma', 'Bi Ma', 'solo', 2, '[0, 1, 2, 3, 4, 5]', 900, '[{"hp":100000000,"mp":100000,"dame":100000,"outfit":[440,441,442,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":["|-1|Ma quỷ xuất hiện"],"e":[]}}]');

-- Christmas Event
INSERT INTO `boss_template` VALUES
('boss_ong_gia_noel', 'Ông già Noel', 'solo', 0, '[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]', 900, '[{"hp":500000000,"mp":500000,"dame":200000,"outfit":[446,447,448,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":["|-1|Merry Christmas!"],"m":["|-1|Ho ho ho!"],"e":["|-1|Hẹn gặp lại năm sau!"]}}]');

-- Trung Thu Event (3 Boss)
INSERT INTO `boss_template` VALUES
('boss_khi_dot', 'Khỉ đột', 'solo', 2, '[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]', 900, '[{"hp":100000000,"mp":100000,"dame":100000,"outfit":[198,193,194,-1,-1,-1],"skills":[[2,7,1000],[3,7,3000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_nguyet_than', 'Nguyệt thần', 'solo', 2, '[0, 1, 2, 3, 4, 5]', 900, '[{"hp":100000000,"mp":100000,"dame":100000,"outfit":[449,450,451,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_nhat_than', 'Nhật thần', 'solo', 2, '[0, 1, 2, 3, 4, 5]', 900, '[{"hp":100000000,"mp":100000,"dame":100000,"outfit":[452,453,454,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]');

-- Hùng Vương Event (2 Boss)
INSERT INTO `boss_template` VALUES
('boss_son_tinh', 'Sơn Tinh', 'solo', 0, '[0, 1, 2, 3, 4, 5]', 900, '[{"hp":500000000,"mp":500000,"dame":150000,"outfit":[455,456,457,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":["|-1|Ta là Sơn Tinh"],"m":["|-1|Núi non thuộc về ta"],"e":[]}}]'),

('boss_thuy_tinh', 'Thủy Tinh', 'solo', 0, '[0, 1, 2, 3, 4, 5]', 900, '[{"hp":500000000,"mp":500000,"dame":150000,"outfit":[458,459,460,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":["|-1|Ta là Thủy Tinh"],"m":["|-1|Thủy triều dâng cao"],"e":[]}}]');

-- Tết Event
INSERT INTO `boss_template` VALUES
('boss_lan_con', 'Lân con', 'solo', 2, '[0, 1, 2, 3, 4, 5]', 900, '[{"hp":300000000,"mp":300000,"dame":120000,"outfit":[461,462,463,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":["|-1|Múa lân đón tết!"],"m":["|-1|Chúc mừng năm mới!"],"e":[]}}]');

-- ==================== GOLDEN FRIEZA (6 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_golden_frieza', 'Golden Frieza', 'group', 2, '[102, 92, 93, 94, 96, 97, 98, 99]', 1800, '[{"hp":2100000000,"mp":2000000,"dame":500000,"outfit":[558,559,560,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":["|-1|Ta đã trở lại!","|-1|Và lần này ta sẽ không thua!"],"m":["|-1|Sức mạnh vàng của ta!"],"e":["|-1|Không thể nào..."]},"together":["boss_death_beam_1","boss_death_beam_2","boss_death_beam_3","boss_death_beam_4","boss_death_beam_5"]}]'),

('boss_death_beam_1', 'Death Beam 1', 'solo', 2, '[102]', 600, '[{"hp":1000000000,"mp":500000,"dame":300000,"outfit":[561,562,563,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_death_beam_2', 'Death Beam 2', 'solo', 2, '[102]', 600, '[{"hp":1000000000,"mp":500000,"dame":300000,"outfit":[561,562,563,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_death_beam_3', 'Death Beam 3', 'solo', 2, '[102]', 600, '[{"hp":1000000000,"mp":500000,"dame":300000,"outfit":[561,562,563,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_death_beam_4', 'Death Beam 4', 'solo', 2, '[102]', 600, '[{"hp":1000000000,"mp":500000,"dame":300000,"outfit":[561,562,563,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_death_beam_5', 'Death Beam 5', 'solo', 2, '[102]', 600, '[{"hp":1000000000,"mp":500000,"dame":300000,"outfit":[561,562,563,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]');

-- ==================== VÕ ĐÀI BA HẠT MÍT (5 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_dracula', 'Dracula', 'solo', 0, '[168]', 300, '[{"hp":50000000,"mp":50000,"dame":50000,"outfit":[464,465,466,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_nguoi_vo_hinh', 'Người vô hình', 'solo', 0, '[168]', 300, '[{"hp":50000000,"mp":50000,"dame":50000,"outfit":[467,468,469,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_bong_bang', 'Bóng băng', 'solo', 0, '[168]', 300, '[{"hp":50000000,"mp":50000,"dame":50000,"outfit":[470,471,472,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_vua_quy_sa_tang', 'Vua quỷ sa tăng', 'solo', 0, '[168]', 300, '[{"hp":50000000,"mp":50000,"dame":50000,"outfit":[473,474,475,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_tho_dau_bac', 'Thỏ đầu bạc', 'solo', 0, '[168]', 300, '[{"hp":50000000,"mp":50000,"dame":50000,"outfit":[476,477,478,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]');

-- ==================== ĐẠI HỘI VÕ THUẬT 23 (12 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_soi_hec_quyn', 'Sói hẹc quyn', 'solo', 0, '[168]', 300, '[{"hp":10000,"mp":10000,"dame":1000,"outfit":[394,395,396,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_o_do', 'Ở dơ', 'solo', 0, '[168]', 300, '[{"hp":25000,"mp":10000,"dame":3000,"outfit":[400,401,402,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_xinbato', 'Xinbatô', 'solo', 0, '[168]', 300, '[{"hp":50000,"mp":10000,"dame":6000,"outfit":[359,360,361,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_cha_pa', 'Cha pa', 'solo', 0, '[168]', 300, '[{"hp":100000,"mp":10000,"dame":9000,"outfit":[362,363,364,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_pon_put', 'Pon put', 'solo', 0, '[168]', 300, '[{"hp":250000,"mp":10000,"dame":10000,"outfit":[365,366,367,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_chan_xu', 'Chan xư', 'solo', 0, '[168]', 300, '[{"hp":500000,"mp":10000,"dame":10000,"outfit":[371,372,373,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_tau_pay_pay', 'Tàu Pảy Pảy', 'solo', 0, '[168]', 300, '[{"hp":2000000,"mp":10000,"dame":15000,"outfit":[92,93,94,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_yamcha', 'Yamcha', 'solo', 0, '[168]', 300, '[{"hp":5000000,"mp":10000,"dame":15000,"outfit":[374,375,376,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_jacky_chun', 'Jacky Chun', 'solo', 0, '[168]', 300, '[{"hp":25000000,"mp":10000,"dame":20000,"outfit":[356,357,358,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_thien_xin_hang', 'Thiên xin hăng', 'solo', 0, '[168]', 300, '[{"hp":75000000,"mp":10000,"dame":22000,"outfit":[368,369,370,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_liu_liu', 'Liu Liu', 'solo', 0, '[168]', 300, '[{"hp":150000000,"mp":10000,"dame":30000,"outfit":[397,398,399,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_pocolo_dhvt', 'PôCôLô', 'solo', 0, '[168]', 300, '[{"hp":400000000,"mp":10000,"dame":50000,"outfit":[9,12,13,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]');

-- ==================== BOSS TRAINING (9 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_karin', 'Karin', 'solo', 1, '[111]', 60, '[{"hp":10000,"mp":10000,"dame":100,"outfit":[479,480,481,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":["|-1|Hãy uống Siêu Thần Thủy"],"m":[],"e":[]}}]'),

('boss_yajiro', 'Yajiro', 'solo', 1, '[112]', 60, '[{"hp":50000,"mp":10000,"dame":500,"outfit":[482,483,484,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_mr_popo', 'Mr. Popo', 'solo', 1, '[113]', 60, '[{"hp":100000,"mp":10000,"dame":1000,"outfit":[485,486,487,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_thuong_de', 'Thượng đế', 'solo', 1, '[114]', 60, '[{"hp":500000,"mp":10000,"dame":5000,"outfit":[488,489,490,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_khi_bubbles', 'Khỉ Bubbles', 'solo', 1, '[115]', 60, '[{"hp":1000000,"mp":10000,"dame":10000,"outfit":[491,492,493,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_than_vu_tru', 'Thần vũ trụ', 'solo', 1, '[116]', 60, '[{"hp":5000000,"mp":10000,"dame":50000,"outfit":[494,495,496,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_to_su_kaio', 'Tố sư Kaio', 'solo', 1, '[117]', 60, '[{"hp":10000000,"mp":10000,"dame":100000,"outfit":[497,498,499,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_whis', 'Whis', 'solo', 1, '[118]', 60, '[{"hp":50000000,"mp":10000,"dame":500000,"outfit":[500,501,502,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":["|-1|Ta là thiên sứ Whis"],"m":[],"e":[]}}]'),

('boss_tau_pay_pay_karin', 'Tàu Pảy Pảy', 'solo', 0, '[111]', 60, '[{"hp":10000,"mp":10000,"dame":100,"outfit":[338,339,340,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]');

-- ==================== BOSS 12H (9 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_mabu_12h', 'Mabư', 'group', 2, '[120]', 60, '[{"hp":1000000000,"mp":1000000,"dame":10000,"outfit":[297,298,299,-1,-1,-1],"skills":[[2,7,1000]],"chat":{"s":["|-1|Bư! Bư! Bư!"],"m":["|-1|Oe Oe"],"e":[]},"together":["boss_drabura_3"]}]'),

('boss_drabura', 'Drabura', 'solo', 2, '[114]', 60, '[{"hp":50000000,"mp":100000,"dame":10000,"outfit":[418,419,420,-1,-1,-1],"skills":[[2,7,1000]],"chat":{"s":[],"m":[],"e":["|-1|Đừng vội mừng, ta sẽ hồi sinh và thịt hết bọn mi"]}}]'),

('boss_drabura_2', 'Drabura', 'group', 2, '[119]', 300, '[{"hp":50000000,"mp":100000,"dame":200000,"outfit":[418,419,420,-1,-1,-1],"skills":[[2,7,1000]],"chat":{"s":["|-1|Ta đã trở lại, lợi hại gấp hai, hahaha"],"m":[],"e":["|-1|Hêhê..ta chẳng cần tốn sức đánh với các ngươi nữa"]},"together":["boss_goku_12h","boss_cadic_12h"]}]'),

('boss_drabura_3', 'Drabura', 'solo', 2, '[114]', 60, '[{"hp":500000000,"mp":100000,"dame":100000,"outfit":[418,419,420,-1,-1,-1],"skills":[[2,7,1000]],"chat":{"s":[],"m":[],"e":["|-1|Đừng vội mừng, ta sẽ hồi sinh và thịt hết bọn mi"]}}]'),

('boss_bui_bui', 'Bui Bui', 'solo', 2, '[115]', 60, '[{"hp":500000000,"mp":100000,"dame":200000,"outfit":[451,452,453,-1,-1,-1],"skills":[[2,7,10000]],"chat":{"s":[],"m":["|-1|Hãy xem đây nhóc"],"e":[]}}]'),

('boss_bui_bui_2', 'Bui Bui', 'solo', 2, '[117]', 60, '[{"hp":500000000,"mp":100000,"dame":200000,"outfit":[451,452,453,-1,-1,-1],"skills":[[2,7,10000]],"chat":{"s":[],"m":["|-1|Trọng lực bây giờ đã tăng gấp 10 lần","|-1|Đó là điều kiện không gian lý tưởng của ta"],"e":["|-1|Đừng vội mừng, ta sẽ hồi sinh và thịt hết bọn mi"]}}]'),

('boss_yacon', 'Ya côn', 'solo', 2, '[118]', 60, '[{"hp":500000000,"mp":100000,"dame":200000,"outfit":[415,416,417,-1,-1,-1],"skills":[[2,7,100]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_goku_12h', 'Gôcu', 'solo', 2, '[119]', 60, '[{"hp":2100000000,"mp":1000000,"dame":100000,"outfit":[101,65,66,-1,-1,-1],"skills":[[2,7,1000],[0,7,1000]],"chat":{"s":["|-1|Tỉnh lại đi Cađíc!","|-1|Một Server là quá đủ rồi!"],"m":["|-1|Cađíc tỉnh lại đi!","|-1|Trời ơi...Hắn định giết mọi người ở đây chắc?"],"e":[]}}]'),

('boss_cadic_12h', 'Ca Đít', 'solo', 2, '[119]', 60, '[{"hp":2100000000,"mp":1000000,"dame":100000,"outfit":[103,16,17,-1,-1,-1],"skills":[[2,7,1000],[3,7,1000]],"chat":{"s":["|-1|Không, còn điện thoại mới, xe mới, nhà mới thì sao","|-1|Từ giờ hãy gọi ta là CađícMẹc"],"m":["|-1|Chúng ta sẽ 1 mất 1 còn!","|-1|Kakalốt! Ta chờ đợi giây phút này đã từ lâu!"],"e":[]}}]');

-- ==================== RED RIBBON HQ - DOANH TRẠI (6 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_trung_uy_trang', 'Trung úy Tráng', 'solo', 0, '[85]', 300, '[{"hp":50000000,"mp":100000,"dame":15000,"outfit":[506,507,508,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":["|-1|Canh cẩn thận đấy!"],"e":[]}}]'),

('boss_trung_uy_thep', 'Trung úy Thép', 'solo', 0, '[86]', 300, '[{"hp":60000000,"mp":100000,"dame":16000,"outfit":[509,510,511,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":["|-1|Bảo vệ doanh trại!"],"e":[]}}]'),

('boss_trung_uy_xanh_lo', 'Trung úy Xanh Lơ', 'solo', 0, '[87]', 300, '[{"hp":70000000,"mp":100000,"dame":17000,"outfit":[512,513,514,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":["|-1|Dám xâm nhập doanh trại!"],"e":[]}}]'),

('boss_ninja_ao_tim', 'Ninja áo tím', 'solo', 0, '[88]', 300, '[{"hp":80000000,"mp":100000,"dame":18000,"outfit":[515,516,517,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":["|-1|Ninjutsu!"],"e":[]}}]'),

('boss_robot_ve_si', 'Robot vệ sĩ', 'solo', 0, '[89]', 300, '[{"hp":90000000,"mp":100000,"dame":19000,"outfit":[518,519,520,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":["|-1|Nhiệm vụ: Tiêu diệt kẻ xâm nhập"],"e":[]}}]'),

('boss_ninja_clone', 'Ninja Clone', 'solo', 0, '[90]', 300, '[{"hp":75000000,"mp":100000,"dame":17500,"outfit":[521,522,523,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]');

-- ==================== CON ĐƯỜNG RẮN (3 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_cadich', 'Cađích', 'solo', 2, '[124]', 300, '[{"hp":100000000,"mp":100000,"dame":20000,"outfit":[524,525,526,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":["|-1|Con đường rắn này là của ta"],"m":["|-1|Ngươi không qua được đâu"],"e":[]}}]'),

('boss_nadic', 'Nađích', 'solo', 2, '[125]', 300, '[{"hp":120000000,"mp":100000,"dame":22000,"outfit":[527,528,529,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":["|-1|Ta sẽ ngăn ngươi lại"],"m":[],"e":[]}}]'),

('boss_saibamen', 'Saibamen', 'solo', 2, '[126]', 300, '[{"hp":80000000,"mp":100000,"dame":18000,"outfit":[530,531,532,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]');

-- ==================== YARDRAT (19 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_tap_su_5', 'Tập sự-5', 'group', 0, '[131]', 60, '[{"hp":350000,"mp":100000,"dame":10000,"outfit":[526,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000],[2,1,1000],[1,1,500]],"chat":{"s":[],"m":["|-1|Khí công pháo"],"e":[]},"together":["boss_tap_su_0","boss_tap_su_1","boss_tap_su_2","boss_tap_su_3","boss_tap_su_4"]}]'),

('boss_tap_su_0', 'Tập sự-0', 'solo', 0, '[131]', 60, '[{"hp":350000,"mp":100000,"dame":10000,"outfit":[526,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_tap_su_1', 'Tập sự-1', 'solo', 0, '[131]', 60, '[{"hp":350000,"mp":100000,"dame":10000,"outfit":[526,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_tap_su_2', 'Tập sự-2', 'solo', 0, '[131]', 60, '[{"hp":350000,"mp":100000,"dame":10000,"outfit":[526,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_tap_su_3', 'Tập sự-3', 'solo', 0, '[131]', 60, '[{"hp":350000,"mp":100000,"dame":10000,"outfit":[526,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_tap_su_4', 'Tập sự-4', 'solo', 0, '[131]', 60, '[{"hp":350000,"mp":100000,"dame":10000,"outfit":[526,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_tan_binh_5', 'Tân binh-5', 'group', 0, '[131]', 60, '[{"hp":450000,"mp":100000,"dame":10000,"outfit":[527,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000],[2,1,1000]],"chat":{"s":[],"m":["|-1|Khí công pháo"],"e":[]},"together":["boss_tan_binh_0","boss_tan_binh_1","boss_tan_binh_2","boss_tan_binh_3","boss_tan_binh_4"]}]'),

('boss_tan_binh_0', 'Tân binh-0', 'solo', 0, '[132]', 60, '[{"hp":450000,"mp":100000,"dame":10000,"outfit":[527,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_tan_binh_1', 'Tân binh-1', 'solo', 0, '[132]', 60, '[{"hp":450000,"mp":100000,"dame":10000,"outfit":[527,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_tan_binh_2', 'Tân binh-2', 'solo', 0, '[132]', 60, '[{"hp":450000,"mp":100000,"dame":10000,"outfit":[527,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_tan_binh_3', 'Tân binh-3', 'solo', 0, '[132]', 60, '[{"hp":450000,"mp":100000,"dame":10000,"outfit":[527,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_tan_binh_4', 'Tân binh-4', 'solo', 0, '[132]', 60, '[{"hp":450000,"mp":100000,"dame":10000,"outfit":[527,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_chien_binh_5', 'Chiến binh-5', 'group', 0, '[132]', 60, '[{"hp":500000,"mp":100000,"dame":10000,"outfit":[528,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000],[2,1,1000]],"chat":{"s":[],"m":["|-1|Khí công pháo"],"e":[]},"together":["boss_chien_binh_0","boss_chien_binh_1","boss_chien_binh_2","boss_chien_binh_3","boss_chien_binh_4"]}]'),

('boss_chien_binh_0', 'Chiến binh-0', 'solo', 0, '[133]', 60, '[{"hp":500000,"mp":100000,"dame":10000,"outfit":[528,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_chien_binh_1', 'Chiến binh-1', 'solo', 0, '[133]', 60, '[{"hp":500000,"mp":100000,"dame":10000,"outfit":[528,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_chien_binh_2', 'Chiến binh-2', 'solo', 0, '[133]', 60, '[{"hp":500000,"mp":100000,"dame":10000,"outfit":[528,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_chien_binh_3', 'Chiến binh-3', 'solo', 0, '[133]', 60, '[{"hp":500000,"mp":100000,"dame":10000,"outfit":[528,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),
('boss_chien_binh_4', 'Chiến binh-4', 'solo', 0, '[133]', 60, '[{"hp":500000,"mp":100000,"dame":10000,"outfit":[528,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_doi_truong_5', 'Đội trưởng-5', 'group', 0, '[133]', 60, '[{"hp":1000000,"mp":100000,"dame":10000,"outfit":[529,525,524,-1,-1,-1],"skills":[[5,1,1000],[6,1,1000],[2,1,1000]],"chat":{"s":[],"m":["|-1|Khí công pháo"],"e":[]},"together":["boss_chien_binh_0","boss_chien_binh_1","boss_chien_binh_2","boss_chien_binh_3","boss_chien_binh_4"]}]');

-- ==================== DESTRON GAS (5 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_dr_lychee', 'Dr. Lychee', 'solo', 2, '[134]', 600, '[{"hp":500000000,"mp":500000,"dame":100000,"outfit":[533,534,535,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":["|-1|Khí hủy diệt sẽ tiêu diệt loài người"],"m":["|-1|Hahaha"],"e":[]}}]'),

('boss_hatchiyack', 'Hatchiyack', 'solo', 2, '[135]', 600, '[{"hp":800000000,"mp":500000,"dame":120000,"outfit":[536,537,538,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":["|-1|Ta là chiến binh cuối cùng"],"m":["|-1|Sức mạnh tuyệt đối"],"e":["|-1|Không thể nào..."]}}]'),

('boss_hit', 'Hit', 'solo', 2, '[136]', 600, '[{"hp":1000000000,"mp":500000,"dame":150000,"outfit":[539,540,541,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":["|-1|Ta là sát thủ huyền thoại"],"m":["|-1|Time Skip!"],"e":[]}}]'),

('boss_chill_1', 'Chill', 'solo', 2, '[137]', 600, '[{"hp":600000000,"mp":500000,"dame":110000,"outfit":[542,543,544,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_chill_2', 'Chill', 'solo', 2, '[137]', 600, '[{"hp":600000000,"mp":500000,"dame":110000,"outfit":[542,543,544,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]');

-- ==================== SUPER DIVINE WATER (1 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_pocolo_sdt', 'Pôcôlô', 'solo', 1, '[138]', 300, '[{"hp":400000000,"mp":100000,"dame":50000,"outfit":[9,12,13,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":["|-1|Hãy uống Siêu Thần Thủy"],"m":["|-1|Sức mạnh sẽ tăng lên gấp bội"],"e":[]}}]');

-- ==================== BOSS MINI (3 Boss) ====================
INSERT INTO `boss_template` VALUES
('boss_soi_hec_mini', 'Sói hẹc quyn', 'solo', 0, '[0, 1, 2, 3, 4, 5, 6]', 600, '[{"hp":10000,"mp":10000,"dame":1000,"outfit":[394,395,396,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_o_do_mini', 'Ở dơ', 'solo', 0, '[168]', 300, '[{"hp":25000,"mp":10000,"dame":3000,"outfit":[400,401,402,-1,-1,-1],"skills":[[0,7,5000],[2,7,1000]],"chat":{"s":[],"m":[],"e":[]}}]'),

('boss_virut', 'Virut', 'solo', 2, '[0, 1, 2, 3, 4, 5]', 300, '[{"hp":50000,"mp":10000,"dame":5000,"outfit":[545,546,547,-1,-1,-1],"skills":[[2,7,1000],[1,7,1000]],"chat":{"s":["|-1|Ta là virus"],"m":["|-1|Nhiễm virus đi!"],"e":[]}}]');

-- ==================== BOSS LINH TINH ====================
INSERT INTO `boss_template` VALUES
('boss_an_trom', 'Ăn trộm', 'solo', 0, '[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]', 60, '[{"hp":100,"mp":100000,"dame":1,"outfit":[618,619,620,-1,-1,-1],"skills":[[2,7,1000]],"chat":{"s":["|-1|Tới giờ làm việc, lụm lụm","|-1|Cảm giác mình vào phải khu người nghèo ::))"],"m":["|-1|Ái chà vàng vàng","|-2|Giám ăn trộm giữa ban ngày thế à"],"e":["|-1|Híc lần sau ta sẽ cho ngươi phá sản","|-2|Chừa thói ăn trộm nghe chưa"]}}]');

SET FOREIGN_KEY_CHECKS = 1;

-- ==================== TỔNG KẾT ====================
-- Tổng cộng: 100+ Boss Records
-- 
-- Skill IDs:
--   0=Kamejoko, 1=Masenko, 2=Galick, 3=Antomic, 4=Lien Hoan, 5=Dragon, 6=Demon
-- 
-- Gender: 
--   0=Trái Đất, 1=Namek, 2=Xayda
-- 
-- Type: 
--   solo = Boss đơn lẻ
--   group = Boss xuất hiện cùng đồng bọn (together)
--   sequence = Boss biến hình qua nhiều giai đoạn (stages)
--   scripts = Boss có AI tùy chỉnh bằng code
--
-- Map Join: Danh sách ID map boss có thể xuất hiện
-- Seconds Rest: Thời gian hồi sinh (giây)
--
-- Chat Format:
--   |-1| = Boss chat
--   |-2| = Random player in map chat
--   |0|, |1|, |n| = Boss phụ có index n chat
