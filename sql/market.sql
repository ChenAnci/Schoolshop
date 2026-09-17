-- ----------------------------
-- 校园市场系统业务表（ruoyi_vue 库）
-- 说明：所有表使用 market_ 前缀，避免与 sys_ 系统表冲突
-- 字符集统一 utf8mb4，时间字段遵循 RuoYi 规范
-- ----------------------------

-- ----------------------------
-- 前置：sys_user 扩展商家入驻申请字段（已执行 ALTER）
-- 状态：merchant_apply_status 0未申请 1待审核 2已通过 3已驳回
-- ALTER TABLE sys_user ADD COLUMN merchant_apply_status char(1) DEFAULT '0' COMMENT '商家入驻申请状态(0未申请 1待审核 2已通过 3已驳回)';
-- ALTER TABLE sys_user ADD COLUMN merchant_apply_remark varchar(500) DEFAULT NULL COMMENT '商家入驻审核意见';
-- ----------------------------

-- ----------------------------
-- 1. 店铺表 market_shop
-- ----------------------------
DROP TABLE IF EXISTS `market_shop`;
CREATE TABLE `market_shop` (
  `shop_id`     bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
  `user_id`     bigint(20)    DEFAULT NULL            COMMENT '归属商家用户ID',
  `shop_name`   varchar(100)  NOT NULL                COMMENT '店铺名称',
  `shop_logo`   varchar(255)  DEFAULT NULL            COMMENT '店铺Logo',
  `shop_desc`   varchar(500)  DEFAULT NULL            COMMENT '店铺简介',
  `contact_phone` varchar(20) DEFAULT NULL            COMMENT '联系电话',
  `status`      char(1)       DEFAULT '0'             COMMENT '状态（0正常 1停用）',
  `create_by`   varchar(64)   DEFAULT ''              COMMENT '创建者',
  `create_time` datetime      DEFAULT NULL            COMMENT '创建时间',
  `update_by`   varchar(64)   DEFAULT ''              COMMENT '更新者',
  `update_time` datetime      DEFAULT NULL            COMMENT '更新时间',
  `remark`      varchar(500)  DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='店铺信息表';

-- ----------------------------
-- 2. 商品分类表 market_category
-- ----------------------------
DROP TABLE IF EXISTS `market_category`;
CREATE TABLE `market_category` (
  `category_id`   bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id`     bigint(20)   DEFAULT 0               COMMENT '父分类ID（0表示顶级）',
  `category_name` varchar(100) NOT NULL                COMMENT '分类名称',
  `sort`          int(11)      DEFAULT 0               COMMENT '显示顺序',
  `status`        char(1)      DEFAULT '0'             COMMENT '状态（0正常 1停用）',
  `create_by`     varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ----------------------------
-- 3. 商品表 market_product
-- ----------------------------
DROP TABLE IF EXISTS `market_product`;
CREATE TABLE `market_product` (
  `product_id`    bigint(20)     NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `shop_id`       bigint(20)     DEFAULT NULL            COMMENT '所属店铺ID',
  `category_id`   bigint(20)     NOT NULL                COMMENT '所属分类ID',
  `product_name`  varchar(100)   NOT NULL                COMMENT '商品名称',
  `product_desc`  varchar(1000)  DEFAULT NULL            COMMENT '商品描述',
  `product_image` varchar(255)   DEFAULT NULL            COMMENT '商品主图',
  `price`         decimal(10,2)  DEFAULT 0.00            COMMENT '价格（元）',
  `stock`         int(11)        DEFAULT 0               COMMENT '库存',
  `sales`         int(11)        DEFAULT 0               COMMENT '销量',
  `status`        char(1)        DEFAULT '0'             COMMENT '状态（0上架 1下架）',
  `sort`          int(11)        DEFAULT 0               COMMENT '显示顺序',
  `create_by`     varchar(64)    DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime       DEFAULT NULL            COMMENT '创建时间',
  `update_by`     varchar(64)    DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime       DEFAULT NULL            COMMENT '更新时间',
  `remark`        varchar(500)   DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`product_id`),
  KEY `idx_product_category` (`category_id`),
  KEY `idx_product_shop` (`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';

-- ----------------------------
-- 4. 购物车表 market_cart
-- ----------------------------
DROP TABLE IF EXISTS `market_cart`;
CREATE TABLE `market_cart` (
  `cart_id`       bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id`       bigint(20)    NOT NULL                COMMENT '用户ID',
  `product_id`    bigint(20)    NOT NULL                COMMENT '商品ID',
  `product_name`  varchar(100)  DEFAULT NULL            COMMENT '商品名称（快照）',
  `product_image` varchar(255)  DEFAULT NULL            COMMENT '商品主图（快照）',
  `price`         decimal(10,2) DEFAULT 0.00            COMMENT '单价（快照）',
  `quantity`      int(11)       DEFAULT 1               COMMENT '数量',
  `checked`       char(1)       DEFAULT '1'             COMMENT '是否选中（1选中 0未选中）',
  `create_time`   datetime      DEFAULT NULL            COMMENT '加入时间',
  `update_time`   datetime      DEFAULT NULL            COMMENT '更新时间',
  PRIMARY KEY (`cart_id`),
  KEY `idx_cart_user` (`user_id`),
  KEY `idx_cart_product` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ----------------------------
-- 5. 订单表 market_orders
-- ----------------------------
DROP TABLE IF EXISTS `market_orders`;
CREATE TABLE `market_orders` (
  `order_id`        bigint(20)     NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`        varchar(32)    NOT NULL                COMMENT '订单编号',
  `user_id`         bigint(20)     NOT NULL                COMMENT '下单用户ID',
  `shop_id`         bigint(20)     DEFAULT NULL            COMMENT '店铺ID',
  `total_amount`    decimal(10,2)  DEFAULT 0.00            COMMENT '订单总金额（元）',
  `pay_amount`      decimal(10,2)  DEFAULT 0.00            COMMENT '实付金额（元）',
  `status`          char(1)        DEFAULT '0'             COMMENT '状态（0待商家接单 1待自提 2已完成 3已取消 4审查中 5审查完成）',
  `audit_flag`      char(1)        DEFAULT '0'             COMMENT '审查位（0未审查 1审查中 2已仲裁）',
  `audit_remark`    varchar(500)   DEFAULT NULL            COMMENT '审查/仲裁意见',
  `receiver_name`   varchar(50)    DEFAULT NULL            COMMENT '取货人姓名',
  `receiver_phone`  varchar(20)    DEFAULT NULL            COMMENT '取货人电话',
  `receiver_address` varchar(255)  DEFAULT NULL            COMMENT '取货地址',
  `pay_time`        datetime       DEFAULT NULL            COMMENT '支付时间',
  `take_time`       datetime       DEFAULT NULL            COMMENT '取货时间',
  `finish_time`     datetime       DEFAULT NULL            COMMENT '完成时间',
  `cancel_time`     datetime       DEFAULT NULL            COMMENT '取消时间',
  `create_by`       varchar(64)    DEFAULT ''              COMMENT '创建者',
  `create_time`     datetime       DEFAULT NULL            COMMENT '下单时间',
  `update_by`       varchar(64)    DEFAULT ''              COMMENT '更新者',
  `update_time`     datetime       DEFAULT NULL            COMMENT '更新时间',
  `remark`          varchar(500)   DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_user` (`user_id`),
  KEY `idx_order_shop` (`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ----------------------------
-- 6. 订单明细表 market_order_item
-- ----------------------------
DROP TABLE IF EXISTS `market_order_item`;
CREATE TABLE `market_order_item` (
  `item_id`       bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id`      bigint(20)    NOT NULL                COMMENT '订单ID',
  `order_no`      varchar(32)   DEFAULT NULL            COMMENT '订单编号（冗余）',
  `product_id`    bigint(20)    NOT NULL                COMMENT '商品ID',
  `product_name`  varchar(100)  DEFAULT NULL            COMMENT '商品名称（快照）',
  `product_image` varchar(255)  DEFAULT NULL            COMMENT '商品主图（快照）',
  `price`         decimal(10,2) DEFAULT 0.00            COMMENT '成交单价（快照）',
  `quantity`      int(11)       DEFAULT 1               COMMENT '购买数量',
  `subtotal`      decimal(10,2) DEFAULT 0.00            COMMENT '小计金额',
  `create_time`   datetime      DEFAULT NULL            COMMENT '创建时间',
  PRIMARY KEY (`item_id`),
  KEY `idx_item_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ----------------------------
-- 7. 商品评价表 market_review
-- ----------------------------
DROP TABLE IF EXISTS `market_review`;
CREATE TABLE `market_review` (
  `review_id`     bigint(20)     NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `order_id`      bigint(20)     NOT NULL                COMMENT '订单ID',
  `order_item_id` bigint(20)     NOT NULL                COMMENT '订单明细ID（一条明细仅可评价一次）',
  `user_id`       bigint(20)     NOT NULL                COMMENT '评价用户ID',
  `product_id`    bigint(20)     NOT NULL                COMMENT '商品ID',
  `shop_id`       bigint(20)     DEFAULT NULL            COMMENT '店铺ID',
  `rating`        int(11)        DEFAULT 5               COMMENT '评分（1-5星）',
  `content`       varchar(1000)  DEFAULT NULL            COMMENT '评价内容',
  `create_time`   datetime       DEFAULT NULL            COMMENT '评价时间',
  PRIMARY KEY (`review_id`),
  UNIQUE KEY `uk_review_item` (`order_item_id`),
  KEY `idx_review_product` (`product_id`),
  KEY `idx_review_shop` (`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- ----------------------------
-- 初始化数据
-- ----------------------------

-- 商品分类
INSERT INTO `market_category` (`category_id`, `parent_id`, `category_name`, `sort`, `status`, `create_by`, `create_time`, `remark`) VALUES
(1, 0, '二手闲置', 1, '0', 'admin', NOW(), '教材、生活用品等二手交易'),
(2, 0, '数码电子', 2, '0', 'admin', NOW(), '手机、电脑配件、耳机等'),
(3, 0, '学习资料', 3, '0', 'admin', NOW(), '教材、笔记、复习资料'),
(4, 0, '校园生活', 4, '0', 'admin', NOW(), '日用品、零食、运动器材');

-- 店铺（归属商家用户 user_id=100，账号 merchant 密码 admin123，见"商家测试账号"说明）
INSERT INTO `market_shop` (`shop_id`, `user_id`, `shop_name`, `shop_desc`, `status`, `create_by`, `create_time`, `remark`) VALUES
(1, 100, '校园官方二手店', '学生自主交易平台示范店铺', '0', 'admin', NOW(), '默认店铺');

-- ----------------------------
-- 商家测试账号（商家端店铺管理联调用）
-- 账号：merchant  密码：admin123  角色：普通角色(common)
-- 说明：以下操作须在 ruoyi_vue 库执行；若需重建，请先删除 sys_user_role 中该用户记录
-- INSERT INTO sys_user (dept_id,user_name,nick_name,user_type,email,phonenumber,sex,avatar,password,status,del_flag,login_ip,login_date,create_by,create_time,remark) VALUES (100,'merchant','测试商家','00','','','0','','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1',NOW(),'admin',NOW(),'商家测试账号');
-- SET @uid = LAST_INSERT_ID(); INSERT INTO sys_user_role (user_id,role_id) VALUES (@uid,2); UPDATE market_shop SET user_id=@uid WHERE shop_id=1;

-- 商品
INSERT INTO `market_product` (`product_id`, `shop_id`, `category_id`, `product_name`, `product_desc`, `price`, `stock`, `sales`, `status`, `sort`, `create_by`, `create_time`, `remark`) VALUES
(1, 1, 1, '高等数学教材（第7版）', '九成新，无笔记涂写，考研可用', 25.00, 10, 3, '0', 1, 'admin', NOW(), '示例商品'),
(2, 1, 2, '无线蓝牙耳机', '国行在保，使用一个月，音质良好', 120.00, 5, 2, '0', 2, 'admin', NOW(), '示例商品'),
(3, 1, 3, '英语四六级真题资料', '含近5年真题+解析，附赠网课', 15.00, 20, 8, '0', 3, 'admin', NOW(), '示例商品');
