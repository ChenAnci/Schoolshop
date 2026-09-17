-- =============================================================
-- admin 端业务菜单持久化初始化
-- 目标：把管理端侧边栏/路由由"前端静态"切换为"后端 sys_menu 驱动"
-- 说明：
--   1) 清洗 RuoYi 默认无关菜单（部门/岗位/字典/参数/通知/日志/监控/工具/官网等），
--      仅保留 系统管理 及其 用户/角色/菜单 及对应按钮。
--   2) 新增 市场管理、店铺管理 两个业务菜单目录与叶子菜单。
--   3) 给 admin 角色授予全部菜单权限。
--   4) admin 为超管角色，getRouters 实际会返回全部菜单，授权主要用于角色编辑界面的权限树回填。
-- 执行方式：Navicat / DBeaver / IDEA Database 中执行本脚本，或 `mysql -uroot ruoyi_vue < 本文件`
-- =============================================================

USE ruoyi_vue;

-- 1）删除 sys_menu 中非业务菜单（仅保留 系统管理1 与 用户100/角色101/菜单102 及 按钮1000-1015）
DELETE FROM sys_menu
WHERE menu_id NOT IN (1, 100, 101, 102,
                      1000,1001,1002,1003,1004,1005,1006,
                      1007,1008,1009,1010,1011,
                      1012,1013,1014,1015);

-- 2）新增 市场管理 目录与叶子菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2000, '市场管理', 0, 2, 'market', NULL, '', 'Market', 1, 0, 'M', '0', '0', '', 'Shop', 'admin', sysdate(), '', NULL, '市场管理目录');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2001, '分类管理', 2000, 1, 'category', 'market/category/index', '', 'MarketCategory', 1, 0, 'C', '0', '0', 'market:category:list', 'Collection', 'admin', sysdate(), '', NULL, '分类管理菜单'),
(2002, '商品管理', 2000, 2, 'product',   'market/product/index',   '', 'MarketProduct',   1, 0, 'C', '0', '0', 'market:product:list', 'Goods', 'admin', sysdate(), '', NULL, '商品管理菜单'),
(2003, '订单审查', 2000, 3, 'order',     'market/order/index',     '', 'MarketOrder',     1, 0, 'C', '0', '0', 'market:order:list',   'Tickets', 'admin', sysdate(), '', NULL, '订单审查菜单');

-- 市场管理 按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2004, '分类查询', 2001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'market:category:query', '#', 'admin', sysdate(), '', NULL, ''),
(2005, '分类新增', 2001, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'market:category:add',   '#', 'admin', sysdate(), '', NULL, ''),
(2006, '分类修改', 2001, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'market:category:edit',  '#', 'admin', sysdate(), '', NULL, ''),
(2007, '分类删除', 2001, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'market:category:remove','#', 'admin', sysdate(), '', NULL, ''),
(2010, '商品查询', 2002, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'market:product:query',  '#', 'admin', sysdate(), '', NULL, ''),
(2011, '商品新增', 2002, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'market:product:add',    '#', 'admin', sysdate(), '', NULL, ''),
(2012, '商品修改', 2002, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'market:product:edit',   '#', 'admin', sysdate(), '', NULL, ''),
(2013, '商品删除', 2002, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'market:product:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2016, '订单查询', 2003, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'market:order:query',    '#', 'admin', sysdate(), '', NULL, ''),
(2017, '订单审查', 2003, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'market:order:edit',     '#', 'admin', sysdate(), '', NULL, '');

-- 3）新增 店铺管理 目录与叶子菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3000, '店铺管理', 0, 3, 'shop', NULL, '', 'Shop', 1, 0, 'M', '0', '0', '', 'OfficeBuilding', 'admin', sysdate(), '', NULL, '店铺管理目录');

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3001, '店铺列表', 3000, 1, 'list', 'shop/list/index', '', 'ShopList', 1, 0, 'C', '0', '0', 'market:shop:list', 'OfficeBuilding', 'admin', sysdate(), '', NULL, '店铺列表菜单');

-- 店铺管理 按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(3010, '店铺查询', 3001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'market:shop:query',  '#', 'admin', sysdate(), '', NULL, ''),
(3011, '店铺新增', 3001, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'market:shop:add',    '#', 'admin', sysdate(), '', NULL, ''),
(3012, '店铺修改', 3001, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'market:shop:edit',   '#', 'admin', sysdate(), '', NULL, ''),
(3013, '店铺删除', 3001, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'market:shop:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 4）重建 admin 角色的菜单授权（清空后按 sys_menu 全量授权）
DELETE FROM sys_role_menu;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_key = 'admin';

-- 5）抬高自增起点，避免后续在菜单管理里"新增"时与手动分配的 menu_id 冲突
ALTER TABLE sys_menu AUTO_INCREMENT = 9000;