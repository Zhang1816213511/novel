package com.novel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Statement;
import java.util.stream.Collectors;

@Configuration
public class DataSourceConfig {

    @Value("${app.datadir:data}")
    private String dataDir;

    /**
     * 从 classpath:schema.sql 加载 DDL 并逐条执行
     */
    private String loadSchemaSql() {
        try {
            ClassPathResource resource = new ClassPathResource("schema.sql");
            String sql = new BufferedReader(new InputStreamReader(resource.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            // 过滤注释和空行，按分号分段
            StringBuilder clean = new StringBuilder();
            for (String line : sql.split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("--") && !trimmed.isEmpty()) {
                    clean.append(line).append("\n");
                }
            }
            return clean.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load schema.sql", e);
        }
    }

    @Bean
    public DataSource dataSource() {
        // 确保数据目录存在
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String url = "jdbc:sqlite:" + dataDir + "/novel.db";
        org.sqlite.SQLiteDataSource ds = new org.sqlite.SQLiteDataSource();
        ds.setUrl(url);

        try (var conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            String schema = loadSchemaSql();
            for (String statement : schema.split(";")) {
                String s = statement.trim();
                if (!s.isEmpty()) {
                    stmt.executeUpdate(s);
                }
            }

            // ─── 兼容旧表：尝试加列，已存在则忽略 ───
            try { stmt.executeUpdate("ALTER TABLE novel_entity ADD COLUMN workspace_dir TEXT"); } catch (Exception ignored) {}
            try { stmt.executeUpdate("ALTER TABLE chapter ADD COLUMN md_dir TEXT"); } catch (Exception ignored) {}
        } catch (Exception e) {
            throw new RuntimeException("Database initialization failed", e);
        }

        return ds;
    }
}