CREATE TABLE `t_sms` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `code` int unsigned DEFAULT '0' COMMENT '验证码',
  `phone` varchar(200) DEFAULT '' COMMENT '手机号',
  `send_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_phone_send_time` (`phone`, `send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
