package com.novel.service;

import io.agentscope.core.tool.Toolkit;

public interface AgentPipelineService {

    /** 直接生成（writer only，无 reviewer） */
    String generate(String modelName, String sysPrompt, String userPrompt);

    /** 带工具的直接生成 */
    String generate(String modelName, String sysPrompt, String userPrompt, Toolkit toolkit);

    /** Writer + Reviewer 双智能体管道 */
    String generateWithReview(String modelName,
                              String writerSysPrompt,
                              String reviewerSysPrompt,
                              String userPrompt);
}
