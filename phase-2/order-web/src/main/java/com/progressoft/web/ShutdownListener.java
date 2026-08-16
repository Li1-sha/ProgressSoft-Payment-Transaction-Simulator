package com.progressoft.web;

import com.progressoft.repository.jpa.EntityManagerFactoryProvider;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class ShutdownListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Close EntityManagerFactory
        EntityManagerFactoryProvider.shutdown();

        // Close DataSource (via ServiceFactory)
        ServiceFactory.shutdown();

        System.out.println("ShutdownListener: All resources closed.");
    }
}