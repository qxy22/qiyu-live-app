USE `qiyu_live_user`;

DROP PROCEDURE IF EXISTS create_t_user_phone_100;

DELIMITER $$

CREATE PROCEDURE create_t_user_phone_100()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE table_name VARCHAR(32);
    DECLARE sql_text VARCHAR(3000);

    WHILE i < 100 DO
        SET table_name = CONCAT('t_user_phone_', LPAD(i, 2, '0'));
        SET sql_text = CONCAT(
            'CREATE TABLE IF NOT EXISTS `', table_name, '` (',
            '`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT ''主键id'',',
            '`user_id` bigint NOT NULL DEFAULT ''-1'' COMMENT ''用户id'',',
            '`phone` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT ''手机号'',',
            '`status` tinyint NOT NULL DEFAULT ''1'' COMMENT ''状态(0无效,1有效)'',',
            '`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',',
            '`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'',',
            'PRIMARY KEY (`id`),',
            'UNIQUE KEY `udx_phone` (`phone`),',
            'KEY `idx_user_id` (`user_id`)',
            ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin'
        );
        SET @sql = sql_text;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

CALL create_t_user_phone_100();

DROP PROCEDURE IF EXISTS create_t_user_phone_100;
