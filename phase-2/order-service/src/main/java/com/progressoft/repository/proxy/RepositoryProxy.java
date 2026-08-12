package com.progressoft.repository.proxy;

import com.progressoft.repository.Identifiable;
import com.progressoft.repository.Repository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class RepositoryProxy {

    @SuppressWarnings("unchecked")
    public static <T extends Identifiable<ID>, ID> Repository<T, ID> loggingProxy(Repository<T, ID> target) {
        return (Repository<T, ID>) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                new Class[]{Repository.class},
                new LoggingHandler(target)
        );
    }

    private static class LoggingHandler implements InvocationHandler {
        private final Repository<?, ?> target;

        LoggingHandler(Repository<?, ?> target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            // Use Order.toString() via args (if args are Orders, they have nice toString)
            String argsString = args != null ? Arrays.toString(args) : "[]";

            System.out.println(">>> " + target.getClass().getSimpleName() + "." + methodName
                    + "(" + argsString + ")");

            long start = System.nanoTime();
            try {
                Object result = method.invoke(target, args);
                long duration = System.nanoTime() - start;
                System.out.println("<<< " + methodName + " returned " + result
                        + " in " + duration / 1_000_000 + "ms");
                return result;
            } catch (Throwable t) {
                long duration = System.nanoTime() - start;
                System.out.println("<<< " + methodName + " threw " + t.getCause()
                        + " in " + duration / 1_000_000 + "ms");
                throw t.getCause(); // unwrap InvocationTargetException
            }
        }
    }
}