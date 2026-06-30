package com.novel.service;

public interface AgentPipelineService {

    String generateWithReview(String modelName,
                              String writerSysPrompt,
                              String reviewerSysPrompt,
                              String userPrompt);
}
