package com.novel.service.impl;

import com.novel.config.AgentScopeConfig;
import com.novel.service.AgentPipelineService;
import com.novel.service.PromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.pipeline.SequentialPipeline;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentPipelineServiceImpl implements AgentPipelineService {

    private static final int MAX_RETRIES = 3;

    private final AgentScopeConfig agentScopeConfig;
    private final PromptLoader promptLoader;

    @Override
    public String generateWithReview(String modelName,
                                     String writerSysPrompt,
                                     String reviewerSysPrompt,
                                     String userPrompt) {
        Model model = agentScopeConfig.buildModel(modelName);
        String currentPrompt = userPrompt;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            ReActAgent writer = buildWriter(model, writerSysPrompt);
            ReActAgent reviewer = buildReviewer(model, reviewerSysPrompt);

            SequentialPipeline pipeline = SequentialPipeline.builder()
                    .addAgent(writer)
                    .addAgent(reviewer)
                    .build();

            Msg pipelineResult = pipeline.execute(
                    Msg.builder().textContent(currentPrompt).build()
            ).block();

            String reviewerOutput = pipelineResult != null ? pipelineResult.getTextContent() : "";

            if (reviewerOutput != null && reviewerOutput.contains("合格")) {
                String content = extractContent(reviewerOutput);
                if (content != null && !content.isBlank()) {
                    log.info("Content passed review on attempt {}", attempt + 1);
                    return content;
                }
            }

            log.warn("Review failed attempt {}/{}: {}", attempt + 1, MAX_RETRIES,
                    reviewerOutput != null ? reviewerOutput.substring(0, Math.min(100, reviewerOutput.length())) : "null");

            if (attempt < MAX_RETRIES - 1) {
                String retrySuffix = promptLoader.get("retry-modification",
                        Map.of("reviewerOutput", reviewerOutput != null ? reviewerOutput : ""));
                currentPrompt = userPrompt + retrySuffix;
            }
        }

        log.warn("Max retries reached, generating final result without review");
        String fallback = promptLoader.get("writer-fallback");
        ReActAgent writer = buildWriter(model, writerSysPrompt + "\n\n" + fallback);
        Msg finalResult = writer.call(Msg.builder().textContent(currentPrompt).build()).block();
        return finalResult != null ? finalResult.getTextContent() : "";
    }

    private ReActAgent buildWriter(Model model, String sysPrompt) {
        return ReActAgent.builder()
                .name("writer")
                .sysPrompt(sysPrompt)
                .model(model)
                .memory(new InMemoryMemory())
                .build();
    }

    private ReActAgent buildReviewer(Model model, String sysPrompt) {
        return ReActAgent.builder()
                .name("reviewer")
                .sysPrompt(sysPrompt)
                .model(model)
                .memory(new InMemoryMemory())
                .build();
    }

    private String extractContent(String reviewerOutput) {
        if (reviewerOutput == null) return null;
        int idx = reviewerOutput.indexOf("合格");
        if (idx < 0) return null;
        String after = reviewerOutput.substring(idx + 2).trim();
        return after.isEmpty() ? null : after;
    }
}
