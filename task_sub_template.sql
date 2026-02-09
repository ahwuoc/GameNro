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
  `task_main_id` int(11) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `max_count` int(11) NOT NULL DEFAULT -1,
  `notify` varchar(255) NOT NULL DEFAULT '',
  `npc_id` int(11) NOT NULL DEFAULT -1,
  `map` int(11) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `task_type` varchar(32) DEFAULT NULL,
  `target_id` varchar(255) NOT NULL DEFAULT '-1',
  `npc_say` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `task_main_id` (`task_main_id`)
) ENGINE=InnoDB AUTO_INCREMENT=163 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_sub_template`
--

LOCK TABLES `task_sub_template` WRITE;
/*!40000 ALTER TABLE `task_sub_template` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `task_sub_template` VALUES
(0,'Di chuyển tới mũi tên chỉ dẫn',1,'Chúc mừng! Bạn đã hoàn thành nhiệm vụ',-1,-1,1,'TASK_SCRIPTS','-1',NULL),
(0,'Hãy đi đến nhà %2 ở bên phải',1,'Chúc mừng! Bạn đã hoàn thành nhiệm vụ',-2,-2,2,'GO_TO_MAP','-1','Làm tốt lắm..\nBây giờ bạn hãy vào nhà ông %2 bên phải để nhận nhiệm vụ mới nhé'),
(0,'Nói chuyện với %2',1,'Chúc mừng! Bạn đã hoàn thành nhiệm vụ',-2,-2,3,'TALK_NPC','-1','Ông %2 đang đứng đợi kìa\nHãy nhấn 2 lần vào để nói chuyện'),
(0,'Mở rương đồ',1,'Rương đồ đằng kia, đi đến gần và chạm nhanh 2 lần vào rương để mở',3,-2,4,'TASK_SCRIPTS','-1','Con vừa đi đâu về đó?\nCon hãy đến rương đồ để lấy rađa..\n..sau đó thu hoạch hết đậu trên cây đậu thần đằng kia!'),
(0,'Thu hoạch đậu thần',1,'Qua bên cây đậu nhấn thu hoạch',4,-2,5,'CONFIRM_MENU','-1',NULL),
(0,'Báo cáo với %2',1,'Nào, bây giờ bạn có thể gặp ông %2 để báo cáo rồi!',-2,-2,6,'TALK_NPC','-1',NULL),
(1,'Đánh ngã 5 mộc nhân',5,'Hãy đánh ngã 10 Bù Nhìn trước cửa làng',-1,-1,7,'KILL_MOB','0','Tốt lắm, rađa sẽ giúp con thấy được lượng máu và thể lực ở bên góc trái\nBây giờ con hãy đi luyện tập\nCon hãy ra %1, ở đó có những con mộc nhân cho con luyện tập dó\nHãy đốn ngã 5 con mộc nhân cho ông'),
(1,'Báo cáo với %2',1,'Chúc mừng bạn đã hoàn thành nhiệm vụ. Nào, bây giờ bạn có thể gặp ông %2 để báo cáo rồi!',-2,-2,8,'TALK_NPC','-1',NULL),
(2,'Thu thập 10 đùi gà',10,'Tiêu diệt 10 con %4 ở bên phải %1',-1,-3,9,'PICK_ITEM','73','Thể lực của con cũng khá tốt\nCon à, dạo gần đây dân làng của chúng ta gặp phải vài chuyện\nBên cạnh làng ta đột nhiên xuất hiện lũ quái vật\nNó tàn sát dân làng và phá hoại nông sản làng ta\nCon hãy tìm đánh chúng và đem về đây 10 cái đùi gà, 2 ông cháu mình sẽ để dành ăn dần\nĐây là tấm bản đồ của vùng này, con hãy xem để tìm đến %3\nCon có thể sử dụng đậu thần khi hết HP hoặc KI, bằng cách nhấn vào nút có hình trái tim bên góc phải dưới màn hình\nNhanh lên, ông đói lắm rồi'),
(2,'Báo cáo với %2',1,'Đi báo cáo với ông %2 nào!',-2,-2,10,'TALK_NPC','-1',NULL),
(3,'Sử dụng tiềm năng',1,'Vào phần kỹ năng sử dụng tiềm năng',-1,-1,11,'TASK_SCRIPTS','2','Tốt lắm, đùi gà đây rồi, haha. Ông sẽ nướng tại đống lửa gần kia con có thể ăn bất cứ lúc nào nếu muốn\nÀ cháu này, vừa nãy ông có nghe thấy 1 tiếng động lớn, hình như có 1 vật thể rơi tại %5, con hãy đến kiểm tra xem\nCon cũng có thể dùng tiềm năng bản thân để nâng HP, KI hoặc sức đánh'),
(3,'Đi khám phá vật thể lạ',1,'Tìm kiếm vật thể lạ',-1,-4,12,'GO_TO_MAP','-1',NULL),
(3,'Báo cáo với %2',1,'Đi báo cáo với ông %2 nào!',-2,-2,13,'TALK_NPC','-1',NULL),
(4,'Đánh 3 con khủng long mẹ',3,'Tiêu diệt lũ %4 mẹ',-1,-5,14,'KILL_MOB','4','Có em bé trong phi thuyền rơi xuống à, ông cứ tưởng là sao băng chứ\nÔng sẽ đặt tên cho em nó là Goku, từ giờ nó sẽ là thành viên trong gia đình ta\nNãy ông mới nhận được tin có bầy mãnh thú xuất hiện tại Trạm phi thuyền\nBọn chúng vừa đổ bộ xuống trái đất để trả thù việc con sát hại con chúng\nCon hãy đi tiêu diệt chúng để giúp dân làng tại đó luôn nhé'),
(4,'Đánh 3 con lợn lòi mẹ',3,'Tốt lắm! Bây giờ hãy quay về và báo cáo cho ông %2 nào',-1,-5,15,'KILL_MOB','5',NULL),
(4,'Đánh 3 con quỷ đất mẹ',3,'Hãy cố gắng luyện tập để trở lên mạnh hơn',-1,-5,16,'KILL_MOB','6',NULL),
(4,'Báo cáo với %2',1,'Hãy đến rừng Karin gần Đồi Hoa Cúc (Trái Đất)',-2,-2,17,'TALK_NPC','-1',NULL),
(5,'Đánh 3 con lợn lòi mẹ',3,'Gặp Bò Mộng nào',-1,-5,18,'KILL_MOB','5','Có em bé trong phi thuyền rơi xuống à, ông cứ tưởng là sao băng chứ\nÔng sẽ đặt tên cho em nó là Goku, từ giờ nó sẽ là thành viên trong gia đình ta\nNãy ông mới nhận được tin có bầy mãnh thú xuất hiện tại Trạm phi thuyền\nBọn chúng vừa đổ bộ xuống trái đất để trả thù việc con sát hại con chúng\nCon hãy đi tiêu diệt chúng để giúp dân làng tại đó luôn nhé'),
(5,'Đánh 3 con khủng long mẹ',3,'',-1,-5,19,'KILL_MOB','4',NULL),
(5,'Đánh 3 con quỷ đất mẹ',3,'%7 kia rồi, hãy tới nói chuyện với cậu ấy',-1,-5,20,'KILL_MOB','6',NULL),
(5,'Báo cáo với %2',1,'Lên đường giải cứu',-2,-2,21,'TALK_NPC','-1',NULL),
(6,'Đánh 3 con quỷ đất mẹ',3,'Bọn chúng kia rồi, hạ sát bọn chúng nào',-1,-5,22,'KILL_MOB','6','Có em bé trong phi thuyền rơi xuống à, ông cứ tưởng là sao băng chứ\nÔng sẽ đặt tên cho em nó là Goku, từ giờ nó sẽ là thành viên trong gia đình ta\nNãy ông mới nhận được tin có bầy mãnh thú xuất hiện tại Trạm phi thuyền\nBọn chúng vừa đổ bộ xuống trái đất để trả thù việc con sát hại con chúng\nCon hãy đi tiêu diệt chúng để giúp dân làng tại đó luôn nhé'),
(6,'Đánh 3 con khủng long mẹ',3,'Hoàn thành nhiệm vụ, quay về báo cáo với %7 nào',-1,-5,23,'KILL_MOB','4',NULL),
(6,'Đánh 3 con lợn lòi mẹ',3,'',-1,-5,24,'KILL_MOB','5',NULL),
(6,'Báo cáo với %2',1,'%8 đang đứng đợi bên kia, tới hỏi thăm bé nào',-2,-2,25,'TALK_NPC','-1',NULL),
(7,'Đạt 16.000 sức mạnh',1,'Quay về kể lại cho ông %2 nghe nào',-1,-1,26,'TASK_SCRIPTS','1','Ông rất tự hào về con\nÔng cho con cuốn bí kíp này để nâng cao võ học\nHãy dùng sức mạnh của mình trừ gian diệt ác bảo vệ dân lành con nhé\nBây giờ con hãy đi tập luyện đi, khi nào mạnh hơn thì quay về đây ông giao cho nhiệm vụ mới\nĐi đi..'),
(7,'Đánh bại 20 con %9',20,'',-1,-7,27,'KILL_MOB','7,8,9',NULL),
(7,'Nói chuyện với %8',1,'Hãy tới gặp %10, ông ấy đang đứng kia kìa',-4,-8,28,'TALK_NPC','-1',NULL),
(7,'Báo cáo với %2',1,'Hãy tập luyện để có sức khỏe',-2,-2,29,'TALK_NPC','-1','Hiện tại em vẫn khỏe anh ạ, hơi bị trầy xước tí thôi nhưng không sao\nEm thực sự cảm ơn anh đã cứu em, nếu không có anh thì giờ này cũng không biết em sẽ thế nào nữa\nÀ em có cái món này, tuy nó không quá giá trị nhưng em mong anh nhận cho em vui'),
(8,'Đạt 40.000 sức mạnh',1,'Đi tiêu diệt %12 nào',-1,-1,30,'TASK_SCRIPTS','1','Ôi bạn ơi, sức đề kháng bạn yếu là do bạn chưa chơi đồ đấy bạn ạ'),
(8,'Tìm viên ngọc rồng 7 sao',1,'Đi báo cáo thôi nào',-1,-3,31,'TASK_SCRIPTS','-1',NULL),
(8,'Đem ngọc về cho %2',1,'Tìm người Trái Đất và kết bạn nào',-2,-2,32,'TALK_NPC','-1',NULL),
(9,'Lên đường',1,'Tìm người Namếc và kết bạn nào',-1,-9,33,'GO_TO_MAP','-1','Cháu trai của ông, con làm ông tự hào lắm. Con đã biết dùng sức mạnh của mình để giúp kẻ yếu\nBây giờ con đã trưởng thành thực sự rồi, ông sẽ bàn giao con lại cho %10 - người bạn lâu ngày không gặp của ông\nCon hãy tìm đường tới %11 và gửi lời chào của ông tới lão ấy nhé\nĐi đi con...'),
(9,'Chào hỏi %10',1,'Tìm người Xayda và kết bạn nào',-5,-9,34,'TALK_NPC','-1',NULL),
(10,'Đạt 200k sức mạnh',1,'Đi báo cáo thôi nào',-1,-1,35,'TASK_SCRIPTS','1','Chào cậu bé, cháu có phải cháu nội ông %2 phải không?\nTa cũng đã gặp cháu 1 lần hồi cháu còn bé xíu à\nBây giờ cháu muốn ta nhận cháu làm đệ tử à? Ta cũng không biết thực lực của cháu hiện tại như nào nữa\nCháu bé hãy đi đánh mấy con %12 ở quanh đây thể hiện tài năng và ta sẽ coi như đó là học phí nhé'),
(10,'Diệt 10 con %12',10,'Quay về nhà',-1,-9,36,'KILL_MOB','13,14,15',NULL),
(10,'Báo cáo với %10',1,'Mạnh dạn lên, cứ nhiệt tình thuyết phục',-5,-9,37,'TALK_NPC','-1',NULL),
(11,'Đạt 500k sức mạnh',1,'Báo cáo nhiệm vụ thôi nào',54,5,38,'TASK_SCRIPTS','1','Tốt lắm, bây giờ con đã chính thức trở thành đệ tử của ta\nTa sẽ dạy con 1 tuyệt chiêu đặc biệt của ta\nBây giờ con hãy đi kết bạn với những người xung quanh đây đi, thêm 1 người bạn bớt 1 kẻ thù mà con\nMà lưu ý là tránh kết bạn với những người có bang hội nhé, họ không là kẻ thù cũng không nên là bạn'),
(11,'Đạt 550k sức mạnh',1,'Gia nhập bang hội',-1,-1,39,'TASK_SCRIPTS','1',NULL),
(11,'Đạt 600k sức mạnh',1,'Báo cáo nhiệm vụ thôi nào',-1,-1,40,'TASK_SCRIPTS','1',NULL),
(11,'Đi về nhà ông %2',1,'Thể hiện tính đoàn kết nào',105,42,41,'GO_TO_MAP','-1',NULL),
(12,'Đi về nhà %2',1,'Tiếp tục nào',-1,-2,42,'GO_TO_MAP','-1','Giờ đây xã giao của con đã tiến bộ hơn rất nhiều rồi\nBây giờ con hãy về nhà xin ông %2 rằng con sẽ vào bang hội nhé\nTa sợ lão ấy không đồng ý lại quay sang trách móc cái thân già này..\nĐi đi con, nói khéo lão ấy nhé.'),
(12,'Nói chuyện - xin phép gia nhập bang hội',1,'Tiếp tục nào',-2,-2,43,'TALK_NPC','-1',NULL),
(12,'Báo cáo lại cho %10',1,'Chúc mừng bạn đã hoàn thành xuất xác nhiệm vụ\nBây giờ đi báo cáo với %10 nào',-5,-9,44,'TALK_NPC','-1','Con muốn tham gia vào bang hội á? Haizz, cái lão già này lại dạy hư cháu ông rồi\nCon muốn thì cũng được thôi, nhưng con phải biết lựa chọn được bang hội nào tốt đấy nhé..\n..xã hội này có nhiều thành phần lắm, cũng chỉ vì an nguy của con nên ông chỉ biết dặn dò vậy\nChúc con may mắn trên con đường con chọn, mà luôn nhớ rằng con phải là 1 công dân tốt đấy nhé..'),
(13,'Tạo hoặc gia nhập bang hội có 2 thành viên',1,'Lên đường cùng bang hội tiêu diệt lũ quái vật nào',13,5,45,'TASK_SCRIPTS','-1','Cuối cùng lão ấy cũng đồng ý rồi à? Tốt lắm\nBây giờ con hãy cùng những người bạn con vừa kết bạn tạo thành 1 bang àội đi nhé\nKhi nào đủ 5 thành viên bang hãy tới đây ta s iao nhiệm vụ cho tất cả các con'),
(13,'Báo cáo cho %10',1,'Tiếp tục đến Namếc nào',-5,-9,46,'TALK_NPC','-1',NULL),
(14,'Tiêu diệt 50 con heo rừng',50,'Tiếp tục đến Xayda nào',-1,27,47,'KILL_MOB','16','Tốt lắm, con đã có những người đồng đội kề vai sát cánh rồi\nBây giờ con và 3 người họ hãy thể hiện tinh thần đoàn kết đi nào\nCách phối hợp nhau làm nhiệm vụ, cách cư xử với nhau đó là hiện thân của tâm tính mỗi người\nCác con hãy đối nhân xử thế với nhau, hãy cùng hợp sức tiêu diệt lũ quái vật nhé'),
(14,'Tiêu diệt 50 con heo da xanh',50,'Xong, quay về báo cáo với %10 nào',-1,31,48,'KILL_MOB','17',NULL),
(14,'Tiêu diệt 50 con heo xayda',50,'Lên đường tiêu diệt lũ quái vật',-1,35,49,'KILL_MOB','18',NULL),
(14,'Quay về %11 báo cáo nhiệm vụ',1,'Tới Namếc nào',-5,-9,50,'TALK_NPC','-1',NULL),
(15,'Tiêu diệt 50 bulon',50,'Tới Xayda nào',-1,30,51,'KILL_MOB','22','Giỏi lắm các con!\n...Hiện tại có vài chủng quái vật mới đổ bộ lên hành tinh chúng ta\nCon hãy cùng 3 người trong bang lên đường tiêu diệt chúng nhé\nDân chúng đặt niềm tin vào các con hết đấy..\nĐi đi...'),
(15,'Tiêu diệt 50 ukulele',50,'Xong, quay về báo cáo với %10 nào',-1,34,52,'KILL_MOB','-1',NULL),
(15,'Tiêu diệt 50 quỷ mập',50,'Lên đường',-1,38,53,'KILL_MOB','-1',NULL),
(15,'Quay về báo cáo %10',1,'Tiếp tục nhiệm vụ',-5,-9,54,'TALK_NPC','-1',NULL),
(16,'Tiêu diệt 100 tháp lơ',100,'Chúc mừng bạn đã hoàn thành nhiệm vụ',-1,28,55,'KILL_MOB','21','Giỏi lắm các con\nCòn 1 vài con quái vật đầu sỏ nữa\nCon hãy tiêu diệt nốt chúng đi nhé..'),
(16,'Tiêu diệt 100 phi long',100,'Chúc mừng bạn đã hoàn thành nhiệm vụ',-1,32,56,'KILL_MOB','-1',NULL),
(16,'Tiêu diệt 100 quỷ bay',100,'Quay về báo cáo %10 nào',-1,36,57,'KILL_MOB','-1',NULL),
(16,'Quay về báo cáo %10',1,'Hãy đi theo sự chỉ dẫn',-5,-9,58,'TALK_NPC','-1',NULL),
(17,'Gặp gỡ Cui',1,'Tiếp tục tiêu diệt lũ tay sai Fide nào',12,92,59,'TALK_NPC','-1','Con thực sự làm ta ngạc nhiên đấy, không uổng công ta truyền dạy võ công\nBên ngoài còn rất nhiều kẻ thù nguy hiểm, nên con phải không ngừng luyện tập nhé\nLại có chuyện xảy ra rồi, Cui - một người họ hàng xa của họ hàng ta - đang gặp chuyện\nCon hãy tới thành phố Vegeta hỏi thăm tình hình cậu ta nhé! Đi đi con..'),
(17,'Báo cáo nhiệm vụ',1,'Tiêu diệt các mục tiêu',12,92,60,'TALK_NPC','-1',NULL),
(18,'Tiêu diệt 20 lính Fide cấp 1',20,'Chúc mừng bạn đã hoàn thành nhiệm vụ',-1,92,61,'KILL_MOB','-1','Chào cậu, cậu là đệ tử của %10 phải không\nBọn người ngoài hành tinh cầm đầu bởi tên Fide đã và đang đổ bộ vào quê hương của tôi..\n..chúng tàn sát hết dân lành và hủy hoại quê hương chúng tôi\nCậu hãy giúp tôi 1 tay tiêu diệt bọn chúng nhé'),
(18,'Tiêu diệt 20 lính Fide cấp 2',20,'Chúc mừng bạn đã hoàn thành nhiệm vụ',-1,93,62,'KILL_MOB','-1',NULL),
(18,'Tiêu diệt 20 lính Fide cấp 3',20,'Chúc mừng bạn đã hoàn thành nhiệm vụ',-1,94,63,'KILL_MOB','-1',NULL),
(18,'Tiêu diệt 20 lính Fide cấp 4',20,'Cùng đồng đội tiêu diệt kẻ ác nào',-1,96,64,'KILL_MOB','-1',NULL),
(18,'Tiêu diệt 20 lính Fide cấp 5',20,'Chúc mừng bạn đã hoàn thành nhiệm vụ',-1,97,65,'KILL_MOB','-1',NULL),
(18,'Báo cáo nhiệm vụ cho Cui',1,'Phát huy tính thần đồng đội bảo vệ dân lành nào',12,92,66,'TALK_NPC','-1',NULL),
(19,'Tiêu diệt Appule',1,'Hạ gục Raspberry',-1,106,67,'KILL_MOB','-1','Cảm ơn cậu đã hỗ trợ tôi tiêu diệt bọn lính tay sai Fide\n3 tên cầm đầu chúng đang tức giận lắm, tôi thì không đủ mạnh để chống lại bọn chúng\n...'),
(19,'Tiêu diệt Raspberry',1,'Dấn thân vào nguy hiểm hạ gục đối thủ',-1,107,68,'KILL_MOB','-1',NULL),
(19,'Tiêu diệt Thanks',1,'Hành hiệp trượng nghĩa tiêu diệt kẻ ác',-1,108,69,'KILL_MOB','-1',NULL),
(19,'Báo cáo nhiệm vụ cho Cui',1,'Kẻ thù đang ở phía trước, cùng bang hội tiêu diệt chúng nào',12,92,70,'TALK_NPC','-1',NULL),
(20,'Tiêu diệt Guldo',1,'Hạ gục Recoome',-1,109,71,'KILL_MOB','-1','Cảm ơn cậu đã tiêu diệt giúp tôi lũ đệ tử của Fide\nDưới trướng Fide còn có 1 đội gồm 5 thành viên được chúng gọi là Tiều Đội Sát Thủ\nChúng rất mạnh và rất trung thành với tên Fide\nBọn chúng vừa được cử tới đi trả thù cho 3 tên đệ tử cậu vừa tiêu diệt\nHãy chống lại bọn chúng giúp tôi nhé....'),
(20,'Tiêu diệt Recoome',1,'Tiêu diệt Burter',-1,110,72,'KILL_MOB','-1',NULL),
(20,'Tiêu diệt Burter',1,'Tiêu diệt Jeice',-1,109,73,'KILL_MOB','-1',NULL),
(20,'Tiêu diệt Jeice',1,'Tiêu diệt Ginyu',-1,110,74,'KILL_MOB','-1',NULL),
(20,'Tiêu diệt Ginyu',1,'Hạ gục Fide cấp 1',-1,106,75,'KILL_MOB','-1',NULL),
(21,'Đạt 2 tỷ sức mạnh',1,'',-1,-1,78,'TASK_SCRIPTS','-1','TanTaiPro'),
(21,'Tiêu diệt Fide cấp 1',1,'Hãy đợi bọn Rôbốt sát thủ xuất hiện',-1,-1,79,'KILL_BOSS','-1',NULL),
(21,'Tiêu diệt Fide cấp 2',1,'Tiếp tục với lão Dr.Korê',-1,-1,80,'KILL_BOSS','-1',NULL),
(21,'Tiêu diệt Fide cấp 3',1,'Chúc mừng bạn đã hoàn thành nhiệm vụ. Bây giờ hãy đi trả nhiệm vụ cho Bunma nào',-1,-1,81,'KILL_BOSS','-1',NULL),
(21,'Báo cáo với %10',1,'',-5,-1,82,'TALK_NPC','-1',NULL),
(22,'Báo cáo với %2',1,'Hãy đợi bọn Rôbốt sát thủ xuất hiện',-2,-2,83,'TALK_NPC','-1','TanTaiPro\nTanTaiPro\nTanTaiPro'),
(22,'Đi tìm vị khách lạ',1,'Tiếp tục với Pic nào',38,-1,84,'TASK_SCRIPTS','-1','Ngon'),
(22,'Đưa thuốc trợ tim cho Quy Lão',1,'Tiếp tục với King Kong nào',13,5,85,'USE_ITEM','-1','20 năm trước bọn Android sát thủ đã đánh bại nhóm bảo vệ trái đất của Sôngoku và Cađíc, Pôcôlô ...\nRiêng Sôngoku vì bệnh tim nên đã chết trước đó nên không thể tham gia trận đánh...\nTừ đó đến nay bọn chúng tàn phá Trái Đất không hề thương tiếc\nCháu và mẹ may mắn sống sót nhờ lẩn trốn tại tần hầm của công ty Capsule...\nCháu tuy cũng là siêu xayda nhưng cũng không thể làm gì được bọn Android sát thủ...\nChỉ có Sôngoku mới có thể đánh bại bọn chúng\nmẹ cháu đã chế tạo thành công cỗ máy thời gian\nvà cháu quay về quá khứ để cứu Sôngoku...\nBệnh của Gôku ở quá khứ là nan y, nhưng với trình độ y học tương lai chỉ cần uống thuốc là khỏi...\nHãy đi theo cháu đến tương lai giúp nhóm của Gôku đánh bạn bọn Android sát thủ\nKhi nào chú cần sự giúp đỡ của cháu hãy đến đây nhé'),
(22,'Đến tương lai gặp Bunma',1,'Chúc mừng bạn đã hoàn thành nhiệm vụ. Bây giờ hãy đi trả nhiệm vụ cho Bunma nào',37,102,86,'TALK_NPC','-1','I a cờ bú'),
(22,'Diệt 1000 xên con cấp 1',1000,'',-1,-1,87,'KILL_MOB','-1','Mau đi tiêu diệt 1000 xên cấp 1 đi em'),
(22,'Báo với Bunma tương lai',1,'Chờ xem nỗi hiểm họa đó là gì',37,102,88,'TALK_NPC','-1',NULL),
(23,'Đến điểm hẹn tìm Rôbốt Sát Thủ',1,'Xên bọ hung hấp thụ Pic tiến hóa rồi kìa',-1,97,89,'GO_TO_MAP','-1','Đến Thành Phố Phía Đông tiêu diệt Tokuda à nhầm Dr.Kore và đàn em của hắn.'),
(23,'Tiêu diệt Số 2 (Android 19)',1,'Xên bọ hung hấp thụ Poc tiến hóa rồi kìa',-1,-1,90,'KILL_BOSS','-1',NULL),
(23,'Tiêu diệt Số 1 (Android 20)',1,'Cảm ơn bạn đã giải cứu thị trấn này. Bây giờ hãy quay lại trả nhiệm vụ cho Bunma nào',-1,97,91,'KILL_BOSS','-1',NULL),
(23,'Diệt 1500 xên con cấp 3',1500,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,92,'KILL_MOB','-1',NULL),
(23,'Báo với Bunma tương lai',1,'Tiếp tục với nhiệm vụ tiếp theo!',37,102,93,'TALK_NPC','-1',NULL),
(24,'Đến sân sau siêu thị',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,104,94,'GO_TO_MAP','-1','Bọn Android đã xuất hiện tại sân sau siêu thị mau đi trừ khử chúng'),
(24,'Tiêu diệt Android 15',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,95,'KILL_BOSS','-1',NULL),
(24,'Tiêu diệt Android 14',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,96,'KILL_BOSS','-1',NULL),
(24,'Tiêu diệt Android 13',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,97,'KILL_BOSS','-1',NULL),
(24,'Báo với Bunma tương lai',1,'Tiếp tục với nhiệm vụ tiếp theo!',37,102,98,'TALK_NPC','-1',NULL),
(25,'Đi tìm Píc Póc',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,99,'TASK_SCRIPTS','-1','Quá ghê gớm =))'),
(25,'Tiêu diệt Póc',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,100,'KILL_BOSS','-1',NULL),
(25,'Tiêu diệt Píc',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,101,'KILL_BOSS','-1',NULL),
(25,'Tiêu Diệt Kinh Kong',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,102,'KILL_BOSS','-1',NULL),
(25,'Diệt 2000 xên con cấp 5',2000,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,103,'KILL_MOB','-1',NULL),
(25,'Báo với Bunma tương lai',1,'Tiếp tục với nhiệm vụ tiếp theo!',37,102,104,'TALK_NPC','-1',NULL),
(26,'Đến thị trấn Ginder',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,105,'GO_TO_MAP','-1','Cũng ra gì đấy! Khét đấy nhề!'),
(26,'Tiêu diệt Xên Bọ Hung cấp 1',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,106,'KILL_BOSS','-1',NULL),
(26,'Tiêu diệt Xên Bọ Hung cấp 2',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,107,'KILL_BOSS','-1',NULL),
(26,'Tiêu diệt Xên Bọ Hung hoàn thiện',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,108,'KILL_BOSS','-1',NULL),
(26,'Diệt 2500 xên con cấp 8',2500,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,109,'KILL_MOB','-1',NULL),
(26,'Báo với Bunma tương lai',1,'Tiếp tục với nhiệm vụ tiếp theo!',37,102,110,'TALK_NPC','-1',NULL),
(27,'Nâng sức đánh gốc lên 35K',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,111,'TASK_SCRIPTS','-1','Hãy đến võ đài xên bọ hung và tiêu diệt 7 đứa con của nó'),
(27,'Thu thập Capsule kì bí',500,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,112,'PICK_ITEM','-1','Tốt lắm! Bây giờ con hãy tìm cho ta 500 viên capsulue kì bí'),
(27,'Đến võ đài xên bọ hung',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,113,'GO_TO_MAP','-1',NULL),
(27,'Tiêu diệt 7 đứa con của xên',7,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,114,'KILL_BOSS','-1',NULL),
(27,'Tiêu diệt Siêu Bọ Hung',1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,115,'KILL_BOSS','-1',NULL),
(27,'Báo với Bunma tương lai',1,'Tiếp tục với nhiệm vụ tiếp theo!',37,102,116,'TALK_NPC','-1',NULL),
(28,'Đi theo Ôsin',1,'Tiếp tục với nhiệm vụ tiếp theo!',44,-1,117,'TASK_SCRIPTS','-1', 'Vào lúc 12h trưa các ngày, bạn đến gặp NPC Ô sin tại map Đại hội võ thuật. sau đó bạn đến các tầng của map để tiêu diệt các mục tiêu:\nHạ 25 Drabura\nHạ 25 Bui Bui\nHạ 25 Bui Bui lần 2\nHạ 25 Yacon\nHạ 25 Drabura lần 2\nHạ 50 Mabư'),
(28,'Hạ vua địa ngục Drabura',25,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,118,'KILL_BOSS','-1',NULL),
(28,'Hạ Pui Pui',25,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,119,'KILL_BOSS','-1',NULL),
(28,'Hạ Pui Pui lần 2',25,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,120,'KILL_BOSS','-1',NULL),
(28,'Hạ Yacôn',25,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,121,'KILL_BOSS','-1',NULL),
(28,'Hạ Drabura lần 2',25,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,122,'KILL_BOSS','-1',NULL),
(28,'Hạ Mabư',50,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,123,'TASK_SCRIPTS','-1',NULL),
(28,'Báo cáo với Ôsin',1,'Tiếp tục với nhiệm vụ tiếp theo!',44,-1,124,'TALK_NPC','-1',NULL),
(29,'bạn đã xong nhiệm vụ rồi',-1,'Tiếp tục với nhiệm vụ tiếp theo!',-1,-1,125,'TASK_SCRIPTS','-1',NULL);
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

-- Dump completed on 2026-02-09 15:43:14
