package com.ai.agent.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * 数据源调试配置
 */
@Configuration
public class DataSourceDebugConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private DataSourceProperties dataSourceProperties;

    @PostConstruct
    public void debug() {
        System.out.println("\n========================================");
        System.out.println("数据源配置调试信息：");
        System.out.println("========================================");

        if (dataSourceProperties != null) {
            System.out.println("✓ DataSourceProperties 存在");
            System.out.println("  URL: " + dataSourceProperties.getUrl());
            System.out.println("  Username: " + dataSourceProperties.getUsername());
            System.out.println("  Driver: " + dataSourceProperties.getDriverClassName());
        } else {
            System.out.println("✗ DataSourceProperties 不存在");
        }

        System.out.println("DataSource Bean 存在: " + (dataSource != null));
        System.out.println("JdbcTemplate Bean 存在: " + (jdbcTemplate != null));

        if (jdbcTemplate != null) {
            try {
                String url = jdbcTemplate.execute((java.sql.Connection conn) ->
                    conn.getMetaData().getURL()
                );
                System.out.println("✓ 数据库连接测试成功");
                System.out.println("  实际连接: " + url);
            } catch (Exception e) {
                System.err.println("✗ 数据库连接测试失败: " + e.getMessage());
            }
        }

        System.out.println("========================================\n");
    }
}
