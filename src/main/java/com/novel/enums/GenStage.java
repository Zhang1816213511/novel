package com.novel.enums;

public enum GenStage {
    SYNOPSIS("生成简介"),
    OUTLINE("生成大纲"),
    SUMMARIES("生成梗概"),
    CONTENT("生成正文"),
    DONE("已完成"),
    ERROR("出错");

    private final String display;

    GenStage(String display) { this.display = display; }

    public String getDisplay() { return display; }
}
