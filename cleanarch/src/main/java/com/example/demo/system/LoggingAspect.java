package com.example.demo.system;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.example.demo.adapter.out.gateway.*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        var key = String.format("[%s] %s", className, methodName);
        var logMessage = String.format("[%s]", Arrays.toString(args));

        LoggingContext.put(key + "input: ", logMessage);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            LoggingContext.put(
                    key + " duration: ", String.format("%.3f seconds", duration / 1000.0));

            LoggingContext.put(key + " result: ", result);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            LoggingContext.put(
                    key + " duration: ", String.format("%.3f seconds", duration / 1000.0));

            LoggingContext.put(key + " error: ", e.getMessage());
            throw e;
        }
    }
}
