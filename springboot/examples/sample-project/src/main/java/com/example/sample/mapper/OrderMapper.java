package com.example.sample.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.sample.common.OrderStatus;
import com.example.sample.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 订单 Mapper：自定义方法以 SQL 动词开头（select/count/insert/update/delete），条件收口在 mapper 包。 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

  /** 按状态分页查询（IPage 必须为第一个参数；列显式，禁 SELECT *；逻辑删除条件显式带上）。 */
  @Select({
    "<script>",
    "SELECT id, order_no, status, amount_fen, version, deleted, create_time, update_time",
    "FROM t_order WHERE deleted = 0",
    "<if test='status != null'> AND status = #{status}</if>",
    "ORDER BY id DESC",
    "</script>"
  })
  IPage<Order> selectOrderPage(IPage<Order> page, @Param("status") OrderStatus status);
}
