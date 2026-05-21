package com.novel.service;

import com.novel.config.AgentScopeConfig;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.pipeline.SequentialPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentPipelineService {

    private static final int MAX_RETRIES = 3;

    @Autowired
    private AgentScopeConfig agentScopeConfig;

    /**
     * 使用 Writer + Reviewer 双智能体管道生成内容
     *
     * @param modelName         模型配置名称
     * @param writerSysPrompt   创作助手系统提示词
     * @param reviewerSysPrompt 编辑审查系统提示词
     * @param userPrompt        用户请求
     * @return 审核通过的内容
     */
    public String generateWithReview(String modelName,
                                     String writerSysPrompt,
                                     String reviewerSysPrompt,
                                     String userPrompt) {
        Model model = agentScopeConfig.buildModel(modelName);
        String currentPrompt = userPrompt;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            // 每次重建 agents，避免上下文干扰
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
                currentPrompt = userPrompt + "\n\n【修改意见】\n" + reviewerOutput + "\n请根据上述修改意见重新生成内容。";
            }
        }

        // 最大重试次数后直接生成
        log.warn("Max retries reached, generating final result without review");
        ReActAgent writer = buildWriter(model, writerSysPrompt + "\n\n请直接输出内容，不要额外说明。");
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

    /**
     * Reviewer 审查通过时第一行回复「合格」并输出全部原文，
     * 提取「合格」之后的部分即为原始生成内容。
     */
    private String extractContent(String reviewerOutput) {
        if (reviewerOutput == null) return null;
        int idx = reviewerOutput.indexOf("合格");
        if (idx < 0) return null;
        String after = reviewerOutput.substring(idx + 2).trim();
        return after.isEmpty() ? null : after;
    }
}
