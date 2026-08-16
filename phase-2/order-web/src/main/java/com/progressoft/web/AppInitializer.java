package com.progressoft.web;

import com.progressoft.repository.jpa.EntityManagerFactoryProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

@WebListener
public class AppInitializer implements ServletContextListener {

    private static DataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 1. Create DataSource once at startup
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:~/orders;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);

        // Store in servlet context so other components can access it
        sce.getServletContext().setAttribute("dataSource", dataSource);

        // 2. Initialize EntityManagerFactory (wires JPA)
        EntityManagerFactoryProvider.getEntityManagerFactory();

        // 3. Create schema (if needed) – but JPA's update will handle it

        System.out.println("AppInitializer: DataSource and EntityManagerFactory created.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 1. Close EntityManagerFactory
        EntityManagerFactoryProvider.shutdown();

        // 2. Close DataSource
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
                System.out.println("AppInitializer: DataSource closed.");
            } catch (Exception e) {
                System.err.println("Error closing DataSource: " + e.getMessage());
            }
        }
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}