package com.novel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelProperties {
    private String provider;
    private String modelName;
    private String baseUrl;
    private String apiKey;
    private String options;
}
