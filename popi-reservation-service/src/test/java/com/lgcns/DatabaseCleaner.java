package com.lgcns;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner implements InitializingBean {

    @PersistenceContext private EntityManager entityManager;

    private List<String> tableNames;

    @Override
    public void afterPropertiesSet() {
        entityManager.unwrap(Session.class).doWork(this::extractTableNames);
    }

    private void extractTableNames(Connection conn) {
        tableNames =
                entityManager.getMetamodel().getEntities().stream()
                        .filter(e -> e.getJavaType().getAnnotation(Entity.class) != null)
                        .map(e -> toSnakeCase(e.getName()))
                        .collect(Collectors.toList());
    }

    public void execute() {
        entityManager.unwrap(Session.class).doWork(this::cleanTables);
    }

    private void cleanTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");

            for (String table : tableNames) {
                stmt.executeUpdate("TRUNCATE TABLE " + table);
                stmt.executeUpdate(
                        "ALTER TABLE " + table + " ALTER COLUMN " + table + "_id RESTART WITH 1");
            }

            stmt.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
