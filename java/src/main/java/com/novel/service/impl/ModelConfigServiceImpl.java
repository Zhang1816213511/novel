package com.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.novel.entity.ModelConfig;
import com.novel.mapper.ModelConfigMapper;
import com.novel.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;

    @Override
    public ModelConfig getById(Long id) {
        return super.getById(id);
    }

    @Override
    public List<ModelConfig> listAll() {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ModelConfig::getCreateTime);
        return modelConfigMapper.selectList(wrapper);
    }

    @Override
    public List<ModelConfig> listEnabled() {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getEnabled, 1);
        return modelConfigMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig saveModel(ModelConfig modelConfig) {
        modelConfigMapper.insert(modelConfig);
        modelConfig.setId(modelConfigMapper.getLastInsertId());
        return modelConfig;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig updateModel(Long id, ModelConfig modelConfig) {
        modelConfig.setId(id);
        modelConfigMapper.updateById(modelConfig);
        return modelConfig;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        modelConfigMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ModelConfig toggleEnabled(Long id) {
        ModelConfig mc = modelConfigMapper.selectById(id);
        if (mc != null) {
            mc.setEnabled(mc.getEnabled() == 1 ? 0 : 1);
            modelConfigMapper.updateById(mc);
        }
        return mc;
    }
}
