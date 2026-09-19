-- Luojia Soundscape business schemas
--
-- Sanitized schema-only export
-- ------------------------------------------------------
-- Server version	8.0.29

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `luojia_soundscape_account`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `luojia_soundscape_account` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `luojia_soundscape_account`;

--
-- Table structure for table `recharge_info`
--

DROP TABLE IF EXISTS `recharge_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recharge_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户ID',
  `order_no` varchar(50) NOT NULL DEFAULT '' COMMENT '充值订单编号',
  `recharge_status` char(4) NOT NULL DEFAULT '0' COMMENT '充值状态：0901-未支付 0902-已支付 0903-已取消',
  `recharge_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '充值金额',
  `pay_way` char(4) NOT NULL DEFAULT '1' COMMENT '支付方式：1101-微信 1102-支付宝',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_trade_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='充值信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_account`
--

DROP TABLE IF EXISTS `user_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户id',
  `total_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '账户总金额',
  `lock_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '锁定金额',
  `available_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '可用金额',
  `total_income_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总收入',
  `total_pay_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总支出',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_account_detail`
--

DROP TABLE IF EXISTS `user_account_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_account_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `title` varchar(100) NOT NULL DEFAULT '' COMMENT '交易标题',
  `trade_type` varchar(10) NOT NULL DEFAULT '' COMMENT '交易类型：1201-充值 1202-锁定 1203-解锁 1204-消费',
  `amount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT '金额',
  `order_no` varchar(50) DEFAULT NULL COMMENT '订单编号',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` varchar(2) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账户明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `luojia_soundscape_album`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `luojia_soundscape_album` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `luojia_soundscape_album`;

--
-- Table structure for table `album_attribute_value`
--

DROP TABLE IF EXISTS `album_attribute_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `album_attribute_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `album_id` bigint NOT NULL DEFAULT '0' COMMENT '专辑id',
  `attribute_id` bigint NOT NULL DEFAULT '0' COMMENT '属性id',
  `value_id` bigint NOT NULL DEFAULT '0' COMMENT '属性值id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_album_id` (`album_id`),
  KEY `idx_value_id` (`value_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专辑属性值关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `album_info`
--

DROP TABLE IF EXISTS `album_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `album_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户id',
  `album_title` varchar(100) NOT NULL DEFAULT '' COMMENT '标题',
  `category3_id` bigint NOT NULL DEFAULT '0' COMMENT '三级分类id',
  `album_intro` varchar(200) NOT NULL DEFAULT '0' COMMENT '专辑简介',
  `cover_url` varchar(200) NOT NULL DEFAULT '' COMMENT '专辑封面原图，尺寸不固定，最大尺寸为960*960（像素）',
  `include_track_count` int unsigned DEFAULT '0' COMMENT '专辑包含声音总数',
  `is_finished` char(1) NOT NULL DEFAULT '0' COMMENT '专辑是否完结：0-否；1-完结；',
  `estimated_track_count` int unsigned NOT NULL DEFAULT '0' COMMENT '预计更新多少集',
  `album_rich_intro` text COMMENT '专辑简介，富文本',
  `quality_score` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '专辑评分',
  `pay_type` char(4) NOT NULL DEFAULT '0101' COMMENT '付费类型: 0101-免费、0102-vip免费、0103-付费',
  `price_type` char(4) DEFAULT NULL COMMENT '价格类型： 0201-单集 0202-整专辑 【声音购买不支持折扣】',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '原价',
  `discount` decimal(2,1) NOT NULL DEFAULT '-1.0' COMMENT '0.1-9.9  不打折 -1',
  `vip_discount` decimal(2,1) NOT NULL DEFAULT '-1.0' COMMENT '0.1-9.9 不打折 -1',
  `tracks_for_free` int NOT NULL DEFAULT '0' COMMENT '免费试听集数',
  `seconds_for_free` int NOT NULL DEFAULT '0' COMMENT '每集免费试听秒数',
  `buy_notes` text COMMENT '购买须知，富文本',
  `selling_point` text COMMENT '专辑卖点，富文本',
  `is_open` char(1) NOT NULL DEFAULT '1' COMMENT '是否开放',
  `status` char(4) DEFAULT NULL COMMENT '专辑状态 0301-审核通过 0302-审核不通过',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_category3_id` (`category3_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专辑信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `album_stat`
--

DROP TABLE IF EXISTS `album_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `album_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `album_id` bigint DEFAULT '0' COMMENT '专辑id',
  `stat_type` varchar(10) NOT NULL DEFAULT '0' COMMENT '统计类型：0401-播放量 0402-订阅量 0403-购买量 0403-评论数',
  `stat_num` int unsigned NOT NULL DEFAULT '0' COMMENT '统计数目',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_album_id` (`album_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专辑统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `base_attribute`
--

DROP TABLE IF EXISTS `base_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_attribute` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `category1_id` bigint NOT NULL DEFAULT '0' COMMENT '三级分类id',
  `attribute_name` varchar(200) NOT NULL DEFAULT '' COMMENT '属性显示名称',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_category3_id` (`category1_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='属性表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `base_attribute_value`
--

DROP TABLE IF EXISTS `base_attribute_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_attribute_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `attribute_id` bigint NOT NULL DEFAULT '0' COMMENT '属性id',
  `value_name` varchar(100) NOT NULL DEFAULT '' COMMENT '属性值名称',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_attribute_id` (`attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='属性值表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `base_category1`
--

DROP TABLE IF EXISTS `base_category1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_category1` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(10) NOT NULL COMMENT '分类名称',
  `order_num` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一级分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `base_category2`
--

DROP TABLE IF EXISTS `base_category2`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_category2` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '二级分类名称',
  `category1_id` bigint NOT NULL DEFAULT '0' COMMENT '一级分类编号',
  `order_num` int NOT NULL DEFAULT '0',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_category1_id` (`category1_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='二级分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `base_category3`
--

DROP TABLE IF EXISTS `base_category3`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_category3` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '三级分类名称',
  `category2_id` bigint NOT NULL DEFAULT '0' COMMENT '二级分类编号',
  `order_num` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_top` tinyint NOT NULL DEFAULT '0',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_category2_id` (`category2_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='三级分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `base_category_view`
--

DROP TABLE IF EXISTS `base_category_view`;
/*!50001 DROP VIEW IF EXISTS `base_category_view`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `base_category_view` AS SELECT
 1 AS `id`,
 1 AS `category1_id`,
 1 AS `category1_name`,
 1 AS `category2_id`,
 1 AS `category2_name`,
 1 AS `category3_id`,
 1 AS `category3_name`,
 1 AS `create_time`,
 1 AS `update_time`,
 1 AS `is_deleted`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `track_info`
--

DROP TABLE IF EXISTS `track_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `track_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` varchar(255) DEFAULT NULL COMMENT '用户id',
  `album_id` bigint NOT NULL DEFAULT '0' COMMENT '专辑id',
  `track_title` varchar(200) NOT NULL DEFAULT '' COMMENT '声音标题',
  `order_num` int NOT NULL DEFAULT '0' COMMENT '声音在专辑中的排序值，从1开始依次递增，值越小排序越前',
  `track_intro` varchar(255) DEFAULT NULL COMMENT '声音简介',
  `track_rich_intro` text COMMENT '声音简介，富文本',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '声音封面图url',
  `media_duration` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '声音媒体时长，单位秒',
  `media_file_id` varchar(30) DEFAULT NULL COMMENT '媒体文件的唯一标识',
  `media_url` varchar(200) DEFAULT '' COMMENT '媒体播放地址',
  `media_size` bigint NOT NULL DEFAULT '0' COMMENT '音频文件大小，单位字节',
  `media_type` varchar(10) NOT NULL DEFAULT '' COMMENT '声音媒体类型',
  `source` char(4) NOT NULL DEFAULT '1' COMMENT '声音来源：0601-用户原创 0602-上传',
  `is_open` char(1) NOT NULL DEFAULT '1' COMMENT '是否开放',
  `status` char(4) NOT NULL DEFAULT '0' COMMENT '声音状态 0501-审核通过 0502"-审核不通过',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_album_id` (`album_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='声音信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `track_stat`
--

DROP TABLE IF EXISTS `track_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `track_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `track_id` bigint NOT NULL DEFAULT '0' COMMENT '声音id',
  `stat_type` varchar(10) NOT NULL DEFAULT '0' COMMENT '统计类型：0701-播放量 0702-收藏量 0703-点赞量 0704-评论数',
  `stat_num` int NOT NULL DEFAULT '0' COMMENT '统计数目',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_track_id` (`track_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='声音统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `luojia_soundscape_dispatch`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `luojia_soundscape_dispatch` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `luojia_soundscape_dispatch`;

--
-- Table structure for table `xxl_job_config`
--

DROP TABLE IF EXISTS `xxl_job_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `xxl_job_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL DEFAULT '' COMMENT '调度任务标题',
  `executor_handler` varchar(30) NOT NULL DEFAULT '' COMMENT '调度执行handler',
  `executor_param` varchar(255) DEFAULT NULL COMMENT '调度任务参数',
  `cron` varchar(20) NOT NULL DEFAULT '' COMMENT '调度表达式',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `status` int NOT NULL DEFAULT '0' COMMENT '任务状态    0：失败    1：成功',
  `xxl_job_id` int DEFAULT NULL COMMENT 'xxl任务平台id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='xxl调度任务配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `xxl_job_log`
--

DROP TABLE IF EXISTS `xxl_job_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `xxl_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_config_id` bigint NOT NULL DEFAULT '0' COMMENT '任务配置id',
  `status` int NOT NULL DEFAULT '1' COMMENT '任务状态    0：失败    1：成功',
  `error` text COMMENT '失败信息',
  `times` int NOT NULL DEFAULT '0' COMMENT '耗时(单位：毫秒)',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`),
  KEY `idx_job_config_id` (`job_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `luojia_soundscape_live`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `luojia_soundscape_live` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `luojia_soundscape_live`;

--
-- Table structure for table `live_room`
--

DROP TABLE IF EXISTS `live_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `live_room` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户id',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '直播间封面',
  `live_title` varchar(100) DEFAULT NULL COMMENT '直播标题',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '当前直播场次的总访问人次',
  `status` varchar(255) DEFAULT NULL COMMENT '直播状态:1-直播正在进行，2-直播结束',
  `tag_id` varchar(255) DEFAULT NULL COMMENT '直播间标签id',
  `app_name` varchar(20) DEFAULT NULL COMMENT '直播应用名称',
  `stream_name` varchar(100) DEFAULT NULL COMMENT '直播流名称',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `push_url` varchar(200) DEFAULT NULL COMMENT '推流地址',
  `play_url` varchar(200) DEFAULT NULL COMMENT '播放地址',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `location` varchar(100) DEFAULT NULL COMMENT '位置',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='直播间';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `live_tag`
--

DROP TABLE IF EXISTS `live_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `live_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(20) NOT NULL DEFAULT '' COMMENT '标签名称',
  `icon_url` varchar(255) DEFAULT NULL COMMENT '标签图标url',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='直播标签';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `luojia_soundscape_order`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `luojia_soundscape_order` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `luojia_soundscape_order`;

--
-- Table structure for table `order_derate`
--

DROP TABLE IF EXISTS `order_derate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_derate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_id` varchar(10) NOT NULL COMMENT '订单id',
  `derate_type` char(4) NOT NULL DEFAULT '0' COMMENT '订单减免类型 1405-专辑折扣 1406-VIP服务折',
  `derate_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '减免金额',
  `remarks` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单减免表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_detail`
--

DROP TABLE IF EXISTS `order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_id` varchar(10) NOT NULL COMMENT '订单id',
  `item_id` bigint NOT NULL COMMENT '付费项目id',
  `item_name` varchar(200) DEFAULT NULL COMMENT '付费项目名称',
  `item_url` varchar(255) DEFAULT NULL COMMENT '付费项目图片url',
  `item_price` decimal(10,2) DEFAULT NULL COMMENT '付费项目价格',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_info`
--

DROP TABLE IF EXISTS `order_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户ID',
  `order_title` varchar(100) NOT NULL DEFAULT '' COMMENT '订单标题',
  `order_no` varchar(50) NOT NULL DEFAULT '' COMMENT '订单号',
  `order_status` char(4) NOT NULL DEFAULT '0901' COMMENT '订单状态：0901-未支付 0902-已支付 0903-已取消',
  `original_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单原始金额',
  `derate_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '减免总金额',
  `order_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单总价',
  `item_type` char(4) NOT NULL DEFAULT '1' COMMENT '付款项目类型: 1001-专辑 1002-声音 1003-vip会员',
  `pay_way` char(4) NOT NULL DEFAULT '1' COMMENT '支付方式：1101-微信 1102-支付宝 1103-账户余额',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `luojia_soundscape_payment`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `luojia_soundscape_payment` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `luojia_soundscape_payment`;

--
-- Table structure for table `payment_info`
--

DROP TABLE IF EXISTS `payment_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_info` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `payment_type` char(4) NOT NULL DEFAULT '' COMMENT '支付类型：1301-订单 1302-充值',
  `order_no` varchar(50) NOT NULL DEFAULT '' COMMENT '订单号',
  `pay_way` char(4) NOT NULL DEFAULT '' COMMENT '付款方式：1101-微信 1102-支付宝',
  `out_trade_no` varchar(50) DEFAULT NULL COMMENT '交易编号（微信或支付）',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '支付金额',
  `content` varchar(200) DEFAULT NULL COMMENT '交易内容',
  `payment_status` char(4) DEFAULT NULL COMMENT '支付状态：1401-未支付 1402-已支付',
  `callback_time` datetime DEFAULT NULL COMMENT '回调时间',
  `callback_content` text COMMENT '回调信息',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `uniq_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='付款信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `luojia_soundscape_system`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `luojia_soundscape_system` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `luojia_soundscape_system`;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '部门名称',
  `parent_id` bigint DEFAULT '0' COMMENT '上级部门id',
  `tree_path` varchar(255) DEFAULT ',' COMMENT '树结构',
  `sort_value` int DEFAULT '1' COMMENT '排序',
  `leader` varchar(20) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) DEFAULT NULL COMMENT '电话',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态（1正常 0停用）',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=COMPACT COMMENT='组织机构';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_login_log`
--

DROP TABLE IF EXISTS `sys_login_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `username` varchar(50) DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `status` tinyint(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示信息',
  `access_time` datetime DEFAULT NULL COMMENT '访问时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '所属上级',
  `name` varchar(20) NOT NULL DEFAULT '' COMMENT '名称',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '类型(0:目录,1:菜单,2:按钮)',
  `path` varchar(100) DEFAULT NULL COMMENT '路由地址',
  `component` varchar(100) DEFAULT NULL COMMENT '组件路径',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `sort_value` int DEFAULT NULL COMMENT '排序',
  `active_menu` varchar(255) DEFAULT NULL COMMENT '高亮的 path',
  `is_hide` tinyint(1) NOT NULL DEFAULT '0',
  `status` tinyint DEFAULT NULL COMMENT '状态(0:禁止,1:正常)',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_menu_1`
--

DROP TABLE IF EXISTS `sys_menu_1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu_1` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '所属上级',
  `name` varchar(20) NOT NULL DEFAULT '' COMMENT '名称',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '类型(0:目录,1:菜单,2:按钮)',
  `path` varchar(100) DEFAULT NULL COMMENT '路由地址',
  `component` varchar(100) DEFAULT NULL COMMENT '组件路径',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `sort_value` int DEFAULT NULL COMMENT '排序',
  `active_menu` varchar(255) DEFAULT NULL COMMENT '高亮的 path',
  `is_hide` tinyint(1) NOT NULL DEFAULT '0',
  `status` tinyint DEFAULT NULL COMMENT '状态(0:禁止,1:正常)',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` varchar(20) DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `operator_type` varchar(20) DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) NOT NULL COMMENT '岗位编码',
  `name` varchar(50) NOT NULL DEFAULT '' COMMENT '岗位名称',
  `description` varchar(255) NOT NULL DEFAULT '' COMMENT '描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态（1正常 0停用）',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色id',
  `role_name` varchar(20) NOT NULL DEFAULT '' COMMENT '角色名称',
  `role_code` varchar(20) DEFAULT NULL COMMENT '角色编码',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='角色';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL DEFAULT '0',
  `menu_id` bigint NOT NULL DEFAULT '0',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='角色菜单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员id',
  `username` varchar(20) NOT NULL DEFAULT '' COMMENT '用户名',
  `password` varchar(32) NOT NULL DEFAULT '' COMMENT '密码',
  `name` varchar(50) DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机',
  `head_url` varchar(200) DEFAULT NULL COMMENT '头像地址',
  `dept_id` bigint DEFAULT NULL COMMENT '部门id',
  `post_id` bigint DEFAULT NULL COMMENT '岗位id',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `status` tinyint DEFAULT NULL COMMENT '状态（1：正常 0：停用）',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `role_id` bigint NOT NULL DEFAULT '0' COMMENT '角色id',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_admin_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='用户角色';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `luojia_soundscape_user`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `luojia_soundscape_user` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `luojia_soundscape_user`;

--
-- Table structure for table `user_certification`
--

DROP TABLE IF EXISTS `user_certification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_certification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户id',
  `id_card1_url` varchar(255) DEFAULT NULL COMMENT '身份证地址1',
  `id_card2_url` varchar(255) DEFAULT NULL COMMENT '身份证地址2',
  `face_url` varchar(255) DEFAULT NULL COMMENT '人脸图片地址',
  `result_data` text COMMENT '比对结果数据',
  `operate_user_id` bigint DEFAULT NULL COMMENT '日志操作用户',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户认证';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_collect`
--

DROP TABLE IF EXISTS `user_collect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_collect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `track_id` bigint NOT NULL DEFAULT '0' COMMENT '声音ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_info`
--

DROP TABLE IF EXISTS `user_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机',
  `password` varchar(50) DEFAULT NULL COMMENT '密码',
  `wx_open_id` varchar(50) NOT NULL DEFAULT '' COMMENT '微信openId',
  `nickname` varchar(100) DEFAULT '' COMMENT 'nickname',
  `avatar_url` varchar(500) DEFAULT '' COMMENT '主播用户头像图片',
  `is_vip` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '用户是否为VIP会员 0:普通用户  1:VIP会员',
  `vip_expire_time` date DEFAULT NULL COMMENT '当前VIP到期时间，即失效时间',
  `gender` tinyint DEFAULT NULL COMMENT '性别',
  `birthday` date DEFAULT NULL COMMENT '出生年月',
  `intro` varchar(255) DEFAULT NULL COMMENT '简介',
  `certification_type` tinyint DEFAULT NULL COMMENT '主播认证类型',
  `certification_status` tinyint DEFAULT NULL COMMENT '认证状态',
  `status` char(4) NOT NULL DEFAULT '0' COMMENT '状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_wx_open_id` (`wx_open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_listen_process`
--

DROP TABLE IF EXISTS `user_listen_process`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_listen_process` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户id',
  `album_id` bigint NOT NULL DEFAULT '0' COMMENT '专辑id',
  `track_id` bigint NOT NULL DEFAULT '0' COMMENT '声音id，声音id为0时，浏览的是专辑',
  `break_second` decimal(10,2) DEFAULT NULL COMMENT '相对于音频开始位置的播放跳出位置，单位为秒。比如当前音频总时长60s，本次播放到音频第25s处就退出或者切到下一首，那么break_second就是25',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户播放进度表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_paid_album`
--

DROP TABLE IF EXISTS `user_paid_album`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_paid_album` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_no` varchar(50) NOT NULL DEFAULT '0' COMMENT '订单号',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户ID',
  `album_id` bigint NOT NULL DEFAULT '0' COMMENT '专辑id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户已付款专辑';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_paid_track`
--

DROP TABLE IF EXISTS `user_paid_track`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_paid_track` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_no` varchar(50) NOT NULL DEFAULT '0' COMMENT '订单号',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户ID',
  `album_id` bigint DEFAULT NULL COMMENT '专辑id',
  `track_id` bigint NOT NULL DEFAULT '0' COMMENT '声音id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户已付款声音';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_stat`
--

DROP TABLE IF EXISTS `user_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户id',
  `stat_type` int NOT NULL DEFAULT '0' COMMENT '统计类型',
  `stat_num` int NOT NULL DEFAULT '0' COMMENT '统计数目',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_track_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户统计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_subscribe`
--

DROP TABLE IF EXISTS `user_subscribe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_subscribe` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `album_id` bigint NOT NULL DEFAULT '0' COMMENT '专辑ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记（0:不可用 1:可用）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_album_id` (`album_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户订阅表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_vip_service`
--

DROP TABLE IF EXISTS `user_vip_service`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_vip_service` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `order_no` varchar(50) NOT NULL DEFAULT '' COMMENT '订单号',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户id',
  `start_time` datetime DEFAULT NULL COMMENT '开始生效日期',
  `expire_time` datetime DEFAULT NULL COMMENT '到期时间',
  `is_auto_renew` tinyint NOT NULL DEFAULT '0' COMMENT '是否自动续费',
  `next_renew_time` datetime DEFAULT NULL COMMENT '下次自动续费时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户vip服务记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vip_service_config`
--

DROP TABLE IF EXISTS `vip_service_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vip_service_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(20) NOT NULL DEFAULT '' COMMENT '服务名称',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '原价，单位元，用于营销展示',
  `discount_price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '折后价，单位元，即实际价格',
  `intro` varchar(50) DEFAULT NULL COMMENT '优惠简介',
  `rich_intro` varchar(300) DEFAULT NULL COMMENT '服务简介，富文本',
  `service_month` int DEFAULT NULL COMMENT '服务月数',
  `image_url` varchar(100) NOT NULL DEFAULT '0' COMMENT '服务图片url',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='vip服务配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Current Database: `luojia_soundscape_account`
--

USE `luojia_soundscape_account`;

--
-- Current Database: `luojia_soundscape_album`
--

USE `luojia_soundscape_album`;

--
-- Final view structure for view `base_category_view`
--

/*!50001 DROP VIEW IF EXISTS `base_category_view`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `base_category_view` AS select `c3`.`id` AS `id`,`c1`.`id` AS `category1_id`,`c1`.`name` AS `category1_name`,`c2`.`id` AS `category2_id`,`c2`.`name` AS `category2_name`,`c3`.`id` AS `category3_id`,`c3`.`name` AS `category3_name`,`c3`.`create_time` AS `create_time`,`c3`.`update_time` AS `update_time`,`c3`.`is_deleted` AS `is_deleted` from ((`base_category1` `c1` join `base_category2` `c2` on((`c2`.`category1_id` = `c1`.`id`))) join `base_category3` `c3` on((`c3`.`category2_id` = `c2`.`id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Current Database: `luojia_soundscape_dispatch`
--

USE `luojia_soundscape_dispatch`;

--
-- Current Database: `luojia_soundscape_live`
--

USE `luojia_soundscape_live`;

--
-- Current Database: `luojia_soundscape_order`
--

USE `luojia_soundscape_order`;

--
-- Current Database: `luojia_soundscape_payment`
--

USE `luojia_soundscape_payment`;

--
-- Current Database: `luojia_soundscape_system`
--

USE `luojia_soundscape_system`;

--
-- Current Database: `luojia_soundscape_user`
--

USE `luojia_soundscape_user`;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2023-10-13 21:44:04
