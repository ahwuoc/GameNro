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
-- Table structure for table `boss_template`
--

DROP TABLE IF EXISTS `boss_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `boss_template` (
  `id` varchar(100) NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(50) NOT NULL COMMENT 'solo, group, sequence, scripted',
  `gender` tinyint(4) NOT NULL DEFAULT 0,
  `map_join` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`map_join`)),
  `seconds_rest` int(11) NOT NULL DEFAULT 300,
  `stages` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`stages`)),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `boss_template`
--

LOCK TABLES `boss_template` WRITE;
/*!40000 ALTER TABLE `boss_template` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `boss_template` VALUES
('android_19','Số 19','solo',2,'[92, 93, 94]',600,'[{\"hp\":800000,\"mp\":100000,\"dame\":12000,\"outfit\":[247,248,249,-1,-1,-1],\"skills\":[[1,7,1000],[4,5,2000]],\"chat\":{\"s\":[\"|-1|Năng lượng của các ngươi sẽ thuộc về ta\",\"|-1|Tính toán cho thấy khả năng thắng của ngươi là 0%\"],\"e\":[\"|-1|Pin... yếu... quá...\"]}}]'),
('boss_broly','Broly','scripts',2,'[5, 13, 20]',1800,'[{\"hp\":10000000,\"mp\":5000000,\"dame\":50000,\"outfit\":[291,292,293,-1,-1,-1],\"skills\":[[1,7,500]],\"chat\":{\"s\":[\"|-1|Kakarot... KAKAROT!!!!\"],\"m\":[\"|-1|Sức mạnh của ta là vô hạn!!\"]}}]'),
('boss_burter','So 2','solo',2,'[68, 69, 70]',600,'[{\"hp\":1200000,\"mp\":300000,\"dame\":16000,\"outfit\":[177,178,179,-1,-1,-1],\"skills\":[[1,7,1000]],\"chat\":{\"m\":[\"|-1|Ta là người nhanh nhất thiên hà!!\"]}}]'),
('boss_cell_1','Xên Bọ Hung','sequence',2,'[100]',1200,'[{\"hp\":1500000,\"mp\":200000,\"dame\":18000,\"outfit\":[174,175,176,-1,-1,-1],\"skills\":[[0,7,1000],[1,7,1000]],\"chat\":{\"s\":[\"|-1|Thế giới này sắp thuộc về ta\"],\"e\":[\"|-1|Không thể nào...\"]},\"together\":[\"boss_cell_2\",\"boss_cell_3\"]}]'),
('boss_cell_2','Xên Bọ Hung 2','scripts',2,'[92, 93, 94]',600,'[{\"hp\":5000000,\"mp\":1000000,\"dame\":35000,\"outfit\":[231,232,233,-1,-1,-1],\"skills\":[[1,7,1000],[2,7,2000]],\"chat\":{\"s\":[\"|-1|Sức mạnh thật tuyệt vời!\"],\"e\":[\"|-1|Ta sẽ đạt đến trạng thái hoàn hảo!\"]}}]'),
('boss_cell_3','Xên Bọ Hung 3','scripts',2,'[30]',300,'[{\"hp\":15000000,\"mp\":5000000,\"dame\":60000,\"outfit\":[234,235,236,-1,-1,-1],\"skills\":[[1,7,1000],[2,7,2000],[3,7,3000]],\"chat\":{\"s\":[\"|-1|Giờ thì không ai cản nổi ta!\"],\"m\":[\"|-1|Dáng đứng của ta đẹp không?\"]}}]'),
('boss_ginyu','Ginyu','group',2,'[68, 69, 70]',900,'[{\"hp\":2000000,\"mp\":500000,\"dame\":20000,\"outfit\":[174,175,176,-1,-1,-1],\"skills\":[[1,7,1000]],\"chat\":{\"s\":[\"|-1|Tiểu đội sát thủ... XUẤT QUÂN!!\"]},\"together\":[\"boss_recoome\",\"boss_burter\"]}]'),
('boss_kuku','Kuku','solo',2,'[68, 69, 70, 71, 72]',600,'[{\"hp\":500000,\"mp\":100000,\"dame\":9000,\"outfit\":[159,160,161,-1,-1,-1],\"skills\":[[1,3,1000],[4,7,1000]],\"chat\":{\"s\":[\"|-1|Tao đã có lệnh của đại ca Fide rồi\",\"|-1|Mày yếu đi đó, nhìn máy đo đi\"],\"m\":[\"|-1|Tao đã có lệnh của đại ca Fide rồi\",\"|-1|Mày yếu đi đó, nhìn máy đo đi\"],\"e\":[\"|-1|Được lắm, quân tử trả thù 10 năm chưa muộn\"]}}]'),
('boss_recoome','So 1','solo',2,'[68, 69, 70]',600,'[{\"hp\":1000000,\"mp\":200000,\"dame\":15000,\"outfit\":[171,172,173,-1,-1,-1],\"skills\":[[1,7,1000]],\"chat\":{\"s\":[\"|-1|Chuẩn bị ăn đòn đi nhóc\"]}}]');
/*!40000 ALTER TABLE `boss_template` ENABLE KEYS */;
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

-- Dump completed on 2026-02-05 22:53:07
