/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-12.1.2-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: nro
-- ------------------------------------------------------
-- Server version	12.1.2-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `task_sub_template`
--

DROP TABLE IF EXISTS `task_sub_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_sub_template` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `task_main_id` int(11) NOT NULL,
  `task_type` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `npc_id` varchar(255) DEFAULT NULL,
  `map_id` varchar(255) DEFAULT NULL,
  `mob_id` varchar(255) DEFAULT NULL,
  `boss_id` varchar(255) DEFAULT NULL,
  `pick_item_id` varchar(255) DEFAULT NULL,
  `power_require` bigint(20) DEFAULT 0,
  `max_count` int(11) NOT NULL DEFAULT 1,
  `notify` varchar(255) DEFAULT '',
  `npc_say` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `task_main_id` (`task_main_id`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_sub_template`
--

LOCK TABLES `task_sub_template` WRITE;
/*!40000 ALTER TABLE `task_sub_template` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `task_sub_template` VALUES
(1,0,'TASK_SCRIPTS','Di chuyển tới mũi tên chỉ dẫn',NULL,NULL,NULL,NULL,NULL,0,1,'Chúc mừng! Bạn đã hoàn thành nhiệm vụ',NULL),
(2,0,'GO_TO_MAP','Hãy đi đến nhà {elder} ở bên phải','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'Chúc mừng! Bạn đã hoàn thành nhiệm vụ','Làm tốt lắm..\nBây giờ bạn hãy vào nhà ông {elder} bên phải để nhận nhiệm vụ mới nhé'),
(3,0,'TALK_NPC','Nói chuyện với {elder}','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'Chúc mừng! Bạn đã hoàn thành nhiệm vụ','Ông {elder} đang đứng đợi kìa\nHãy nhấn 2 lần vào để nói chuyện'),
(4,0,'TASK_SCRIPTS','Mở rương đồ','3','21,22,23',NULL,NULL,NULL,0,1,'Rương đồ đằng kia, đi đến gần và chạm nhanh 2 lần vào rương để mở','Con vừa đi đâu về đó?\nCon hãy đến rương đồ để lấy rađa..\n..sau đó thu hoạch hết đậu trên cây đậu thần đằng kia!'),
(5,0,'CONFIRM_MENU','Thu hoạch đậu thần','4','21,22,23',NULL,NULL,NULL,0,1,'Qua bên cây đậu nhấn thu hoạch',NULL),
(6,0,'TALK_NPC','Báo cáo với {elder}','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'Nào, bây giờ bạn có thể gặp ông {elder} để báo cáo rồi!',NULL),
(7,1,'KILL_MOB','Đánh ngã 5 mộc nhân',NULL,NULL,'0',NULL,NULL,0,5,'Hãy đánh ngã 5 Bù Nhìn trước cửa làng','Tốt lắm, rađa sẽ giúp con thấy được lượng máu và thể lực ở bên góc trái\nBây giờ con hãy đi luyện tập\nCon hãy ra {village}, ở đó có những con mộc nhân cho con luyện tập dó\nHãy đốn ngã 5 con mộc nhân cho ông'),
(8,1,'TALK_NPC','Báo cáo với {elder}','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'Chúc mừng bạn đã hoàn thành nhiệm vụ. Nào, bây giờ bạn có thể gặp ông {elder} để báo cáo rồi!',NULL),
(9,2,'PICK_ITEM','Thu thập 10 đùi gà',NULL,'1,8,15',NULL,NULL,'73',0,10,'Tiêu diệt 10 con {mob_0} ở bên phải {village}','Thể lực của con cũng khá tốt\nCon à, dạo gần đây dân làng của chúng ta gặp phải vài chuyện\nBên cạnh làng ta đột nhiên xuất hiện lũ quái vật\nNó tàn sát dân làng và phá hoại nông sản làng ta\nCon hãy tìm đánh chúng và đem về đây 10 cái đùi gà, 2 ông cháu mình sẽ để dành ăn dần\nĐây là tấm bản đồ của vùng này, con hãy xem để tìm đến {map_0}\nCon có thể sử dụng đậu thần khi hết HP hoặc KI, bằng cách nhấn vào nút có hình trái tim bên góc phải dưới màn hình\nNhanh lên, ông đói lắm rồi'),
(10,2,'TALK_NPC','Báo cáo với {elder}','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'Đi báo cáo với ông {elder} nào!',NULL),
(11,3,'TASK_SCRIPTS','Sử dụng tiềm năng',NULL,NULL,NULL,NULL,NULL,0,1,'Vào phần kỹ năng sử dụng tiềm năng','Tốt lắm, đùi gà đây rồi, haha. Ông sẽ nướng tại đống lửa gần kia con có thể ăn bất cứ lúc nào nếu muốn\nÀ cháu này, vừa nãy ông có nghe thấy 1 tiếng động lớn, hình như có 1 vật thể rơi tại {map_1}, con hãy đến kiểm tra xem\nCon cũng có thể dùng tiềm năng bản thân để nâng HP, KI hoặc sức đánh'),
(12,3,'PICK_ITEM','Đi khám phá vật thể lạ',NULL,'42,43,44',NULL,NULL,'78',0,1,'Nhặt vật thể lạ',NULL),
(13,3,'TALK_NPC','Báo cáo với {elder}','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'Đi báo cáo với ông {elder} nào!',NULL),
(14,4,'KILL_MOB','Đánh 3 con {mob_mother}','0,2,1','2,9,16','4,5,6',NULL,NULL,0,3,'Tiêu diệt lũ {mob_mother}','Có em bé trong phi thuyền rơi xuống à, ông cứ tưởng là sao băng chứ\nÔng sẽ đặt tên cho em nó là Goku, từ giờ nó sẽ là thành viên trong gia đình ta\nNãy ông mới nhận được tin có bầy mãnh thú xuất hiện tại Trạm phi thuyền\nBọn chúng vừa đổ bộ xuống trái đất để trả thù việc con sát hại con chúng\nCon hãy đi tiêu diệt chúng để giúp dân làng tại đó luôn nhé'),
(15,4,'KILL_MOB','Đánh 3 con {mob_mother}','0,2,1','9,16,2','5,6,4',NULL,NULL,0,3,'Tiếp theo bạn hãy đi tiêu diệt {mob_mother}',NULL),
(16,4,'KILL_MOB','Đánh 3 con {mob_mother}','0,2,1','16,2,9','6,4,5',NULL,NULL,0,3,'Sắp xong rồi, tiêu diệt nốt {mob_mother} tại hành tinh cuối cùng nào!',NULL),
(17,4,'TALK_NPC','Báo cáo với {elder}','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'Chúc mừng con, hãy về báo cáo kết quả','Làm tốt lắm con trai!'),
(18,5,'TASK_POWER','Đạt 16.000 sức mạnh',NULL,NULL,NULL,NULL,NULL,16000,1,'Quay về kể lại cho ông {elder} nghe nào','Ông rất tự hào về con\nÔng cho con cuốn bí kíp này để nâng cao võ học\nHãy dùng sức mạnh của mình trừ gian diệt ác bảo vệ dân lành con nhé\nBây giờ con hãy đi tập luyện đi, khi nào mạnh hơn thì quay về đây ông giao cho nhiệm vụ mới\nĐi đi..'),
(19,5,'KILL_MOB','Đánh bại 20 con {mob_mother}',NULL,'3,10,17','7,8,9',NULL,NULL,0,20,'',NULL),
(20,5,'TALK_NPC','Nói chuyện với {village_shop}','60,61,62',NULL,NULL,NULL,NULL,0,1,'Hãy tới gặp {village_shop}, ông ấy đang đứng kia kìa',NULL),
(21,5,'TALK_NPC','Báo cáo với {elder}','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'Hãy tập luyện để có sức khỏe','Hiện tại em vẫn khỏe anh ạ, hơi bị trầy xước tí thôi nhưng không sao\nEm thực sự cảm ơn anh đã cứu em, nếu không có anh thì giờ này cũng không biết em sẽ thế nào nữa\nÀ em có cái món này, tuy nó không quá giá trị nhưng em mong anh nhận cho em vui'),
(22,6,'TASK_POWER','Đạt 40.000 sức mạnh',NULL,NULL,NULL,NULL,NULL,40000,1,'Đi tiêu diệt {mob_2} nào','Ôi bạn ơi, sức đề kháng bạn yếu là do bạn chưa chơi đồ đấy bạn ạ'),
(23,6,'TASK_SCRIPTS','Tìm viên ngọc rồng 7 sao',NULL,'4,12,18',NULL,NULL,'20',0,1,'Đi báo cáo thôi nào',NULL),
(24,6,'TALK_NPC','Đem ngọc về cho {elder}','0,2,1','21,22,23',NULL,NULL,'',0,1,'Tìm thấy ngọc rồng rồi, đem về thôi!',NULL),
(25,7,'TALK_NPC','Nói chuyện với Bò Mộng','17','47',NULL,NULL,NULL,0,1,'','Hắn sắp đến đây, hãy giúp ta tiêu diệt hắn\\nHãy giúp ta tiêu diệt hắn'),
(26,7,'TASK_SCRIPTS','Đụng độ Tàu Pảy Pảy','-1','-1',NULL,NULL,NULL,0,1,'Hình như có người đang tới đây','Hắn sắp đến đây, hãy giúp ta tiêu diệt hắn\\nHãy giúp ta tiêu diệt hắn'),
(27,7,'GO_TO_MAP','Bỏ chạy lên tháp Karin','-1','46',NULL,'',NULL,0,1,'',NULL),
(28,7,'TALK_NPC','Nói chuyện với Thần Mèo','18',NULL,NULL,NULL,NULL,0,1,'','\"Ngươi vừa chạm trán với Tàu Pảy Pảy, đúng chứ?\\n\"\n\"Ta tuy không thấy ánh sáng, nhưng lòng người thì nhìn rõ hơn bất kỳ ai.\\n\"\n\"Hiện tại… ngươi chưa đủ sức làm đối thủ của hắn đâu.\\n\"\n\"Tìm đến ta là quyết định sáng suốt.\\n\"\n\"Ta sẽ truyền cho ngươi vài tuyệt kỹ, nhưng thành bại đều do sự khổ luyện của ngươi.\\n\"\n\"Ngươi đã sẵn sàng bước vào con đường này chưa?\"\n'),
(29,8,'KILL_BOSS','Đánh thắng Thần mèo',NULL,NULL,NULL,NULL,'boss_than_meo_karin',0,1,'',NULL),
(30,8,'KILL_BOSS','Tiêu diệt Tàu Pảy Pảy',NULL,NULL,NULL,NULL,'boss_tau_pay_pay',0,1,'',NULL),
(31,8,'TALK_NPC','Nói chuyện với Bò Mộng','17',NULL,NULL,NULL,NULL,0,1,'',NULL),
(32,8,'TALK_NPC','Báo cáo với {elder}','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'',NULL),
(33,9,'TALK_NPC','Tìm {master} tại {map_master}','13,14,15','5,13,20',NULL,NULL,NULL,0,1,'',NULL),
(34,9,'TALK_NPC','Báo cáo với {elder}','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'',NULL),
(35,9,'TASK_SCRIPTS','Vào 1 bang hội','0,2,1','21,22,23',NULL,NULL,NULL,0,1,'',NULL),
(36,9,'TALK_NPC','Báo cáo với {master}','13,14,15','5,13,20',NULL,NULL,NULL,0,1,'',NULL),
(37,10,'KILL_MOB','Đánh 20 con {mob_mother}',NULL,NULL,'16,17,18',NULL,NULL,0,20,'Tiêu diệt lũ {mob_mother}','Có em bé trong phi thuyền rơi xuống à, ông cứ tưởng là sao băng chứ\nÔng sẽ đặt tên cho em nó là Goku, từ giờ nó sẽ là thành viên trong gia đình ta\nNãy ông mới nhận được tin có bầy mãnh thú xuất hiện tại Trạm phi thuyền\nBọn chúng vừa đổ bộ xuống trái đất để trả thù việc con sát hại con chúng\nCon hãy đi tiêu diệt chúng để giúp dân làng tại đó luôn nhé'),
(38,10,'KILL_MOB','Đánh 20 con {mob_mother}',NULL,NULL,'17,18,16',NULL,NULL,0,20,'Tiêu diệt lũ {mob_mother}','Có em bé trong phi thuyền rơi xuống à, ông cứ tưởng là sao băng chứ\nÔng sẽ đặt tên cho em nó là Goku, từ giờ nó sẽ là thành viên trong gia đình ta\nNãy ông mới nhận được tin có bầy mãnh thú xuất hiện tại Trạm phi thuyền\nBọn chúng vừa đổ bộ xuống trái đất để trả thù việc con sát hại con chúng\nCon hãy đi tiêu diệt chúng để giúp dân làng tại đó luôn nhé'),
(39,10,'KILL_MOB','Đánh 20 con {mob_mother}',NULL,NULL,'18,16,17',NULL,NULL,0,20,'Tiêu diệt lũ {mob_mother}','Có em bé trong phi thuyền rơi xuống à, ông cứ tưởng là sao băng chứ\nÔng sẽ đặt tên cho em nó là Goku, từ giờ nó sẽ là thành viên trong gia đình ta\nNãy ông mới nhận được tin có bầy mãnh thú xuất hiện tại Trạm phi thuyền\nBọn chúng vừa đổ bộ xuống trái đất để trả thù việc con sát hại con chúng\nCon hãy đi tiêu diệt chúng để giúp dân làng tại đó luôn nhé'),
(40,10,'TALK_NPC','Báo cáo với {master}','13,14,15','5,13,20',NULL,NULL,NULL,0,1,'',NULL),
(41,10,'TASK_POWER','Đạt 200.000 sức mạnh',NULL,NULL,NULL,NULL,NULL,200000,1,'','Ôi bạn ơi, sức đề kháng bạn yếu là do bạn chưa chơi đồ đấy bạn ạ'),
(42,10,'PICK_ITEM','Đánh bọn {mob_mother} lấy truyện',NULL,NULL,'13,14,15',NULL,NULL,0,1,'','Ôi bạn ơi, sức đề kháng bạn yếu là do bạn chưa chơi đồ đấy bạn ạ'),
(43,11,'TALK_NPC','Báo cáo với {master}','13,14,15','5,13,20',NULL,NULL,NULL,0,1,'',NULL),
(44,11,'KILL_MOB','Đánh 30 con {mob_mother}','13,14,15','22',NULL,NULL,NULL,0,1,'',NULL),
(45,11,'KILL_MOB','Đánh 30 con {mob_mother}','13,14,15','23',NULL,NULL,NULL,0,1,'',NULL),
(46,11,'KILL_MOB','Đánh 30 con {mob_mother}','13,14,15','24',NULL,NULL,NULL,0,1,'',NULL);
/*!40000 ALTER TABLE `task_sub_template` ENABLE KEYS */;
UNLOCK TABLES;
commit;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-02-16 15:54:51
