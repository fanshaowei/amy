package com.amy.sunpalaceartspace.config;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import com.alibaba.fastjson2.JSON;

/**
 * 列表字符串 JSON ↔ List&lt;String&gt; 字段映射处理器
 *
 * <p>用于把 {@code List<String>} 字段以 JSON 数组字符串形式存入 VARCHAR/TEXT 列，
 * 读取时自动反序列化为 {@code List<String>}。常用于 {@code @TableField} 字段级 typeHandler，
 * 例如 {@code biz_spas_reservation_order.comp_users}（预约随行人身份信息）。</p>
 *
 * <p>写入：{@code List<String>} → JSON 字符串 → {@code PreparedStatement.setString}。<br>
 * 读取：{@code ResultSet.getString} → JSON 字符串 → {@code JSON.parseArray(String, String.class)}。</p>
 *
 * @author fantasyfan
 * @date 2026-09-13
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class StringListJsonTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, JSON.toJSONString(parameter));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<String> parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseArray(json, String.class);
    }
}