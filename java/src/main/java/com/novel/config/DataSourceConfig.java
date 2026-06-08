package com.novel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.io.File;
import java.sql.Statement;

@Configuration
public class DataSourceConfig {

    @Value("${app.datadir:data}")
    private String dataDir;

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
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS novel_entity (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "content TEXT," +
                "synopsis TEXT," +
                "outline TEXT," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP)"
            );
            // 兼容旧表：尝试加列，已存在则忽略
            try { stmt.executeUpdate("ALTER TABLE novel_entity ADD COLUMN synopsis TEXT"); } catch (Exception ignored) {}
            try { stmt.executeUpdate("ALTER TABLE novel_entity ADD COLUMN outline TEXT"); } catch (Exception ignored) {};

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS chapter (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "novel_id INTEGER NOT NULL," +
                "chapter_number INTEGER NOT NULL," +
                "title TEXT DEFAULT ''," +
                "summary TEXT DEFAULT ''," +
                "content TEXT DEFAULT ''," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP)"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS model_config (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "provider TEXT NOT NULL," +
                "base_url TEXT," +
                "model_name TEXT NOT NULL," +
                "api_key TEXT," +
                "options TEXT," +
                "enabled INTEGER DEFAULT 1," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP)"
            );
        } catch (Exception e) {
            throw new RuntimeException("Database initialization failed", e);
        }

        return ds;
    }
}