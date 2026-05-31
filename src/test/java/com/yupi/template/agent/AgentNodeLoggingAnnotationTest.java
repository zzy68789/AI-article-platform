package com.yupi.template.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.yupi.template.agent.agents.ContentGeneratorAgent;
import com.yupi.template.agent.agents.ContentMergerAgent;
import com.yupi.template.agent.agents.ImageAnalyzerAgent;
import com.yupi.template.agent.agents.OutlineGeneratorAgent;
import com.yupi.template.agent.agents.TitleGeneratorAgent;
import com.yupi.template.agent.parallel.ParallelImageGenerator;
import com.yupi.template.annotation.AgentExecution;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentNodeLoggingAnnotationTest {

    @Test
    void graphNodeApplyMethodsDeclareAgentExecutionAnnotations() throws NoSuchMethodException {
        Map<Class<?>, String> expectedAgents = Map.of(
                TitleGeneratorAgent.class, "agent1_generate_titles",
                OutlineGeneratorAgent.class, "agent2_generate_outline",
                ContentGeneratorAgent.class, "agent3_generate_content",
                ImageAnalyzerAgent.class, "agent4_analyze_image_requirements",
                ParallelImageGenerator.class, "agent5_generate_images",
                ContentMergerAgent.class, "agent6_merge_content"
        );

        for (Map.Entry<Class<?>, String> entry : expectedAgents.entrySet()) {
            Method apply = entry.getKey().getMethod("apply", OverAllState.class);
            AgentExecution annotation = apply.getAnnotation(AgentExecution.class);

            assertNotNull(annotation, entry.getKey().getSimpleName() + ".apply should be logged");
            assertEquals(entry.getValue(), annotation.value());
        }
    }
}
