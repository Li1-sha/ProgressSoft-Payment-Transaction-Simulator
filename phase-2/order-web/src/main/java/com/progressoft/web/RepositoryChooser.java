package com.progressoft.web;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.util.Set;

@HandlesTypes({})
public class RepositoryChooser implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) {
        String repoType = System.getProperty("repo.type", "jdbc");
        ctx.setAttribute("repoType", repoType);
        ServiceFactory.setRepoType(repoType);
        System.out.println("RepositoryChooser: Using " + repoType + " repository.");
    }
}