package com.soundmarketplace.observability;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

@Aspect
@Component
@Slf4j
public class TraceAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    @Pointcut("within(@org.springframework.stereotype.Service *) && !within(com.soundmarketplace.observability..*)")
    public void serviceMethods() {}

    @Pointcut("this(org.springframework.data.repository.Repository)")
    public void repositoryMethods() {}

    @Around("controllerMethods()")
    public Object traceController(ProceedingJoinPoint pjp) throws Throwable {
        return traceExecution("controller", pjp);
    }

    @Around("serviceMethods()")
    public Object traceService(ProceedingJoinPoint pjp) throws Throwable {
        return traceExecution("service", pjp);
    }

    @Around("repositoryMethods()")
    public Object traceRepository(ProceedingJoinPoint pjp) throws Throwable {
        return traceExecution("repository", pjp);
    }

    private Object traceExecution(String layer, ProceedingJoinPoint pjp) throws Throwable {
        String className = pjp.getSignature().getDeclaringType().getSimpleName();
        String methodName = pjp.getSignature().getName();
        String summarizedArgs = summarizeArgs(pjp);

        log.debug("TRACE_ENTER layer={} method={}.{} args=[{}]", layer, className, methodName, summarizedArgs);
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            long durationMs = System.currentTimeMillis() - start;
            String summarizedResult;
            try {
                summarizedResult = ArgumentSummarizer.summarize(result);
            } catch (Throwable t) {
                summarizedResult = "<unprintable>";
            }
            log.debug("TRACE_EXIT layer={} method={}.{} durationMs={} result={}", layer, className, methodName, durationMs, summarizedResult);
            return result;
        } catch (Throwable t) {
            long durationMs = System.currentTimeMillis() - start;
            String sanitizedMsg = sanitizeMessage(t.getMessage());
            log.debug("TRACE_ERROR layer={} method={}.{} durationMs={} exception={} message={}",
                    layer, className, methodName, durationMs, t.getClass().getSimpleName(), sanitizedMsg);
            throw t;
        }
    }

    private String summarizeArgs(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) {
            return "";
        }

        String[] paramNames = null;
        if (pjp.getSignature() instanceof MethodSignature methodSignature) {
            paramNames = methodSignature.getParameterNames();
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < args.length; i++) {
            String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : null;
            joiner.add(ArgumentSummarizer.summarize(paramName, args[i]));
        }
        return joiner.toString();
    }

    private String sanitizeMessage(String message) {
        if (message == null) return "none";
        if (ArgumentSummarizer.isSensitiveName(message)) {
            return "***";
        }
        return message.length() > 128 ? message.substring(0, 125) + "..." : message;
    }
}
