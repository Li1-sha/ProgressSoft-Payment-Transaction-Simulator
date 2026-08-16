package com.progressoft.web;

import com.progressoft.repository.jpa.EntityManagerFactoryProvider;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class ShutdownListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Nothing to do here – startup is handled by AppInitializer.
        // But the method must be present to satisfy the interface.
        System.out.println("ShutdownListener: contextInitialized (no-op)");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Close EntityManagerFactory
        EntityManagerFactoryProvider.shutdown();

        // Close DataSource (via ServiceFactory)
        ServiceFactory.shutdown();

        System.out.println("ShutdownListener: All resources closed.");
    }
}