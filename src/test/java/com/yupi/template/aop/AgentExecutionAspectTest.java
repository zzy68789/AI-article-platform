package com.yupi.template.aop;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentExecutionAspectTest {

    @Test
    void extractsTaskIdFromOverAllState() {
        AgentExecutionAspect aspect = new AgentExecutionAspect();
        OverAllState state = new OverAllState(Map.of("taskId", "task-123"));
        ProceedingJoinPoint joinPoint = (ProceedingJoinPoint) Proxy.newProxyInstance(
                ProceedingJoinPoint.class.getClassLoader(),
                new Class[]{ProceedingJoinPoint.class},
                (proxy, method, args) -> "getArgs".equals(method.getName()) ? new Object[]{state} : null
        );

        String taskId = ReflectionTestUtils.invokeMethod(aspect, "extractTaskId", joinPoint);

        assertEquals("task-123", taskId);
    }
}
