package com.novel.service;

import com.novel.entity.ModelConfig;

import java.util.List;

public interface ModelConfigService {

    List<ModelConfig> listAll();

    List<ModelConfig> listEnabled();

    ModelConfig getById(Long id);

    ModelConfig saveModel(ModelConfig modelConfig);

    ModelConfig updateModel(Long id, ModelConfig modelConfig);

    void deleteById(Long id);

    ModelConfig toggleEnabled(Long id);
}
