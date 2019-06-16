/*
SQLyog Ultimate v13.1.1 (64 bit)
MySQL - 5.7.23-0ubuntu0.16.04.1 : Database - fish_dev
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`fish_dev` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `fish_dev`;

/*Table structure for table `can` */

DROP TABLE IF EXISTS `can`;

CREATE TABLE `can` (
  `id` varchar(255) CHARACTER SET utf8 NOT NULL,
  `title` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `uid` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `content` text CHARACTER SET utf8 NOT NULL,
  `type` int(11) DEFAULT '0' COMMENT '罐头类型：0 普通匿名罐头 1 铁皮罐头',
  `date` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `outTime` varchar(255) CHARACTER SET utf8 DEFAULT '259200' COMMENT '默认三天',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
