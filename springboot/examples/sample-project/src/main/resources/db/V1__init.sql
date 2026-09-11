-- V1__init.sql —— 初始表结构（只增不改：一经应用不得改写，后续变更追加新脚本）
-- 约定：表名 t_ 前缀；审计字段 create_time/update_time；逻辑删除 deleted；乐观锁 version；
--       主键统一雪花（IdType.ASSIGN_ID，应用侧生成，故 DDL 不用 AUTO_INCREMENT）；字符集 utf8mb4。

CREATE TABLE IF NOT EXISTS t_order
(
    id          BIGINT       NOT NULL COMMENT '主键（雪花，MyBatis-Plus ASSIGN_ID）',
    order_no    VARCHAR(64)  NOT NULL COMMENT '外部订单号',
    status      VARCHAR(32)  NOT NULL COMMENT '订单状态 code（OrderStatus 枚举，不落字典表）',
    amount_fen  BIGINT       NOT NULL COMMENT '订单金额（分）',
    version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删',
    create_time DATETIME(3)  NOT NULL COMMENT '创建时间（应用自动填充）',
    update_time DATETIME(3)  NOT NULL COMMENT '更新时间（应用自动填充）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='订单';

CREATE TABLE IF NOT EXISTS t_dict_type
(
    id          BIGINT       NOT NULL COMMENT '主键（雪花）',
    type_code   VARCHAR(64)  NOT NULL COMMENT '类型编码（UPPER_SNAKE_CASE，唯一）',
    type_name   VARCHAR(64)  NOT NULL COMMENT '类型名称（中文）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '启停：1=启用 0=停用（只停用不删除）',
    sort_no     INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删',
    create_time DATETIME(3)  NOT NULL COMMENT '创建时间',
    update_time DATETIME(3)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type_code (type_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='字典类型';

CREATE TABLE IF NOT EXISTS t_dict_item
(
    id          BIGINT       NOT NULL COMMENT '主键（雪花）',
    type_code   VARCHAR(64)  NOT NULL COMMENT '类型编码（关联 t_dict_type.type_code）',
    item_code   VARCHAR(64)  NOT NULL COMMENT '项机器值（UPPER_SNAKE_CASE）',
    item_label  VARCHAR(128) NOT NULL COMMENT '项中文展示名',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '启停：1=启用 0=停用（只停用不删除）',
    sort_no     INT          NOT NULL DEFAULT 0 COMMENT '排序号（查询 ORDER BY sort_no, id）',
    remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常 1=已删',
    create_time DATETIME(3)  NOT NULL COMMENT '创建时间',
    update_time DATETIME(3)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_item_type_item (type_code, item_code),
    KEY idx_dict_item_type (type_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='字典项';

-- 初始字典数据（幂等：重复执行不报错）。字典 type_code/item_code 一经使用只停用不删除。
INSERT INTO t_dict_type (id, type_code, type_name, status, sort_no, remark, version, deleted,
                         create_time, update_time)
VALUES (1, 'CHANNEL', '下单渠道', 1, 1, '初始化脚本内置', 0, 0, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE type_name = VALUES(type_name);

INSERT INTO t_dict_item (id, type_code, item_code, item_label, status, sort_no, remark, version,
                         deleted, create_time, update_time)
VALUES (1, 'CHANNEL', 'APP', '应用商店', 1, 1, '初始化脚本内置', 0, 0, NOW(3), NOW(3)),
       (2, 'CHANNEL', 'WEB', '网页端', 1, 2, '初始化脚本内置', 0, 0, NOW(3), NOW(3)),
       (3, 'CHANNEL', 'MINI_PROGRAM', '小程序', 1, 3, '初始化脚本内置', 0, 0, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);
