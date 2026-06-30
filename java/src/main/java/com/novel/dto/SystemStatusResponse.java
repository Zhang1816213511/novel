package com.novel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusResponse {
    private boolean configured;
    private String workspaceRoot;
}
