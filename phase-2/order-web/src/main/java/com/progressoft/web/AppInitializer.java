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
        // Create DataSource once
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:~/orders;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(10);
        HikariDataSource ds = new HikariDataSource(config);
        dataSource = ds;
        // Store in ServletContext and in ServiceFactory
        sce.getServletContext().setAttribute("dataSource", ds);
        ServiceFactory.setDataSource(ds);

        // Init JPA EMF
        EntityManagerFactoryProvider.getEntityManagerFactory();

        System.out.println("AppInitializer: DataSource and EntityManagerFactory created.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
                System.out.println("AppInitializer: DataSource closed.");
            } catch (Exception e) {
                System.err.println("Error closing DataSource: " + e.getMessage());
            }
        }
        EntityManagerFactoryProvider.shutdown();
        ServiceFactory.shutdown();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}