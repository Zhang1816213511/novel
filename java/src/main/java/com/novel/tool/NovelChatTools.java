package com.novel.tool;

import com.novel.entity.Chapter;
import com.novel.entity.Faction;
import com.novel.entity.Novel;
import com.novel.entity.NovelCharacter;
import com.novel.mapper.FactionMapper;
import com.novel.service.ChapterService;
import com.novel.service.MarkdownStorageService;
import com.novel.service.NovelCharacterService;
import com.novel.service.NovelService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NovelChatTools {

    private final Long novelId;
    private final String workspaceDir;
    private final NovelService novelService;
    private final ChapterService chapterService;
    private final MarkdownStorageService markdownStorage;
    private final NovelCharacterService characterService;
    private final FactionMapper factionMapper;

    public NovelChatTools(Long novelId, String workspaceDir,
                          NovelService novelService, ChapterService chapterService,
                          MarkdownStorageService markdownStorage,
                          NovelCharacterService characterService,
                          FactionMapper factionMapper) {
        this.novelId = novelId;
        this.workspaceDir = workspaceDir;
        this.novelService = novelService;
        this.chapterService = chapterService;
        this.markdownStorage = markdownStorage;
        this.characterService = characterService;
        this.factionMapper = factionMapper;
    }

    // ═══════════════════════════════════════════
    // 读取工具：返回带行号的内容，方便定位修改位置
    // ═══════════════════════════════════════════

    @Tool(name = "read_synopsis", description = "读取作品简介，每行前带行号（如'3| 这是第三行'）")
    public String readSynopsis() {
        return numbered(markdownStorage.readSynopsis(workspaceDir), "暂无简介");
    }

    @Tool(name = "read_outline", description = "读取创作大纲，每行前带行号")
    public String readOutline() {
        return numbered(markdownStorage.readOutline(workspaceDir), "暂无大纲");
    }

    @Tool(name = "read_chapter_list", description = "获取所有章节的编号和标题")
    public String readChapterList() {
        List<Chapter> chapters = chapterService.listByNovelId(novelId);
        if (chapters.isEmpty()) return "暂无章节";
        return chapters.stream()
                .map(c -> "第" + c.getChapterNumber() + "章：" + c.getTitle())
                .collect(Collectors.joining("\n"));
    }

    @Tool(name = "read_chapter_summary", description = "读取指定章节的梗概，每行前带行号")
    public String readChapterSummary(
            @ToolParam(name = "chapterNumber", description = "章节编号") int chapterNumber) {
        Chapter ch = getChapter(chapterNumber);
        if (ch == null) return "章节不存在：" + chapterNumber;
        if (ch.getMdDir() == null) return "章节目录未初始化";
        return numbered(markdownStorage.readChapterSummary(workspaceDir, ch.getMdDir()), "暂无梗概");
    }

    @Tool(name = "read_chapter_content", description = "读取指定章节的正文，每行前带行号")
    public String readChapterContent(
            @ToolParam(name = "chapterNumber", description = "章节编号") int chapterNumber) {
        Chapter ch = getChapter(chapterNumber);
        if (ch == null) return "章节不存在：" + chapterNumber;
        if (ch.getMdDir() == null) return "章节目录未初始化";
        return numbered(markdownStorage.readChapterContent(workspaceDir, ch.getMdDir()), "暂无正文");
    }

    @Tool(name = "read_characters", description = "获取所有角色信息（名称、势力、类型、性格、外貌、背景、描述）")
    public String readCharacters() {
        List<NovelCharacter> chars = characterService.listByNovelId(novelId);
        if (chars.isEmpty()) return "暂无角色";
        return chars.stream()
                .map(c -> String.format(
                        "- %s（ID:%d） 势力：%s 类型：%s\n  别名：%s\n  性格：%s\n  外貌：%s\n  背景：%s\n  描述：%s",
                        c.getName(), c.getId(),
                        nvl(c.getFactionName()), nvl(c.getRoleType()),
                        nvl(c.getAlias()), nvl(c.getPersonality()),
                        nvl(c.getAppearance()), nvl(c.getBackground()),
                        nvl(c.getDescription())))
                .collect(Collectors.joining("\n\n"));
    }

    @Tool(name = "read_factions", description = "获取所有势力信息（名称、ID、描述）")
    public String readFactions() {
        List<Faction> factions = factionMapper.listByNovelId(novelId);
        if (factions.isEmpty()) return "暂无势力，请先用 create_faction 创建";
        return factions.stream()
                .map(f -> String.format("- %s（ID:%d）\n  描述：%s",
                        f.getName(), f.getId(), nvl(f.getDescription())))
                .collect(Collectors.joining("\n\n"));
    }

    @Tool(name = "create_faction", description = "创建新势力")
    public String createFaction(
            @ToolParam(name = "name", description = "势力名称") String name,
            @ToolParam(name = "description", description = "势力简介") String description) {
        Faction f = new Faction();
        f.setNovelId(novelId);
        f.setName(name);
        f.setDescription(description);
        factionMapper.insert(f);
        f.setId(factionMapper.getLastInsertId());
        return "势力「" + name + "」已创建（ID:" + f.getId() + "）";
    }

    // ═══════════════════════════════════════════
    // 行级编辑：替换、插入、删除指定行范围
    // ═══════════════════════════════════════════

    @Tool(name = "replace_lines", description = "替换指定目标中 startLine 到 endLine 之间的行（含首尾）。target 取值为 synopsis / outline / chapter_summary / chapter_content。chapterNumber 仅当 target 为 chapter_summary 或 chapter_content 时需要")
    public String replaceLines(
            @ToolParam(name = "target", description = "目标：synopsis / outline / chapter_summary / chapter_content") String target,
            @ToolParam(name = "chapterNumber", description = "章节编号（仅 chapter_summary / chapter_content 需要，其他传 0）") int chapterNumber,
            @ToolParam(name = "startLine", description = "起始行号（从 1 开始）") int startLine,
            @ToolParam(name = "endLine", description = "结束行号（含）") int endLine,
            @ToolParam(name = "newContent", description = "替换后的新内容（可多行）") String newContent) {
        String old = readTarget(target, chapterNumber);
        if (old == null) return "目标不存在或读取失败";
        String result = replaceLinesInText(old, startLine, endLine, newContent);
        if (result == null) return "行号超出范围：startLine=" + startLine + ", endLine=" + endLine;
        writeTarget(target, chapterNumber, result);
        return targetLabel(target, chapterNumber) + " 第" + startLine + "-" + endLine + "行已替换";
    }

    @Tool(name = "insert_after", description = "在指定目标的第 lineNumber 行之后插入新内容。target 取值同上")
    public String insertAfter(
            @ToolParam(name = "target", description = "目标：synopsis / outline / chapter_summary / chapter_content") String target,
            @ToolParam(name = "chapterNumber", description = "章节编号（仅 chapter_summary / chapter_content 需要，其他传 0）") int chapterNumber,
            @ToolParam(name = "lineNumber", description = "在第几行之后插入（0 表示在开头插入）") int lineNumber,
            @ToolParam(name = "content", description = "要插入的内容（可多行）") String content) {
        String old = readTarget(target, chapterNumber);
        if (old == null) return "目标不存在或读取失败";
        String result = insertAfterLine(old, lineNumber, content);
        if (result == null) return "行号超出范围：lineNumber=" + lineNumber;
        writeTarget(target, chapterNumber, result);
        return targetLabel(target, chapterNumber) + " 第" + lineNumber + "行后已插入";
    }

    @Tool(name = "delete_lines", description = "删除指定目标中 startLine 到 endLine 之间的行（含首尾）。target 取值同上")
    public String deleteLines(
            @ToolParam(name = "target", description = "目标：synopsis / outline / chapter_summary / chapter_content") String target,
            @ToolParam(name = "chapterNumber", description = "章节编号（仅 chapter_summary / chapter_content 需要，其他传 0）") int chapterNumber,
            @ToolParam(name = "startLine", description = "起始行号（从 1 开始）") int startLine,
            @ToolParam(name = "endLine", description = "结束行号（含）") int endLine) {
        String old = readTarget(target, chapterNumber);
        if (old == null) return "目标不存在或读取失败";
        String result = replaceLinesInText(old, startLine, endLine, "");
        if (result == null) return "行号超出范围";
        writeTarget(target, chapterNumber, result);
        return targetLabel(target, chapterNumber) + " 第" + startLine + "-" + endLine + "行已删除";
    }

    // ═══════════════════════════════════════════
    // 全局编辑：整体替换
    // ═══════════════════════════════════════════

    @Tool(name = "update_synopsis", description = "整体替换作品简介")
    public String updateSynopsis(
            @ToolParam(name = "content", description = "新简介全文") String content) {
        markdownStorage.writeSynopsis(workspaceDir, content);
        Novel novel = novelService.getById(novelId);
        novel.setSynopsis(content);
        novelService.updateNovel(novelId, novel);
        return "简介已整体替换";
    }

    @Tool(name = "update_outline", description = "整体替换创作大纲")
    public String updateOutline(
            @ToolParam(name = "content", description = "新大纲全文") String content) {
        markdownStorage.writeOutline(workspaceDir, content);
        Novel novel = novelService.getById(novelId);
        novel.setOutline(content);
        novelService.updateNovel(novelId, novel);
        return "大纲已整体替换";
    }

    @Tool(name = "update_chapter_name", description = "修改指定章节的标题")
    public String updateChapterName(
            @ToolParam(name = "chapterNumber", description = "章节编号") int chapterNumber,
            @ToolParam(name = "name", description = "新标题") String name) {
        Chapter ch = getChapter(chapterNumber);
        if (ch == null) return "章节不存在：" + chapterNumber;
        ch.setTitle(name);
        chapterService.updateChapter(ch.getId(), ch);
        return "第" + chapterNumber + "章标题已改为：" + name;
    }

    @Tool(name = "update_chapter_summary", description = "整体替换指定章节的梗概")
    public String updateChapterSummary(
            @ToolParam(name = "chapterNumber", description = "章节编号") int chapterNumber,
            @ToolParam(name = "content", description = "新梗概全文") String content) {
        Chapter ch = getChapter(chapterNumber);
        if (ch == null) return "章节不存在：" + chapterNumber;
        if (ch.getMdDir() == null) return "章节目录未初始化";
        markdownStorage.writeChapterSummary(workspaceDir, ch.getMdDir(), content);
        return "第" + chapterNumber + "章梗概已整体替换";
    }

    @Tool(name = "update_chapter_content", description = "整体替换指定章节的正文")
    public String updateChapterContent(
            @ToolParam(name = "chapterNumber", description = "章节编号") int chapterNumber,
            @ToolParam(name = "content", description = "新正文全文") String content) {
        Chapter ch = getChapter(chapterNumber);
        if (ch == null) return "章节不存在：" + chapterNumber;
        if (ch.getMdDir() == null) return "章节目录未初始化";
        markdownStorage.writeChapterContent(workspaceDir, ch.getMdDir(), content);
        return "第" + chapterNumber + "章正文已整体替换";
    }

    // ═══════════════════════════════════════════
    // 角色管理
    // ═══════════════════════════════════════════

    @Tool(name = "create_character", description = "新增角色。factionId 可通过 read_factions 获取")
    public String createCharacter(
            @ToolParam(name = "name", description = "角色名称") String name,
            @ToolParam(name = "description", description = "角色描述") String description,
            @ToolParam(name = "personality", description = "性格（可选）") String personality,
            @ToolParam(name = "appearance", description = "外貌（可选）") String appearance,
            @ToolParam(name = "background", description = "背景（可选）") String background,
            @ToolParam(name = "factionId", description = "所属势力 ID（可选，通过 read_factions 获取）") Long factionId,
            @ToolParam(name = "roleType", description = "角色类型：主角/配角/NPC（可选）") String roleType) {
        NovelCharacter c = new NovelCharacter();
        c.setNovelId(novelId);
        c.setName(name);
        c.setDescription(description);
        if (personality != null) c.setPersonality(personality);
        if (appearance != null) c.setAppearance(appearance);
        if (background != null) c.setBackground(background);
        if (factionId != null) c.setFactionId(factionId);
        if (roleType != null) c.setRoleType(roleType);
        characterService.createCharacter(c);
        return "角色「" + name + "」已创建（ID:" + c.getId() + "）";
    }

    @Tool(name = "update_character", description = "修改角色信息（按名称查找，只传要修改的字段）")
    public String updateCharacter(
            @ToolParam(name = "name", description = "角色名称（用于查找）") String name,
            @ToolParam(name = "description", description = "新描述（可选，不修改则不传）") String description,
            @ToolParam(name = "personality", description = "新性格（可选）") String personality,
            @ToolParam(name = "appearance", description = "新外貌（可选）") String appearance,
            @ToolParam(name = "background", description = "新背景（可选）") String background,
            @ToolParam(name = "factionId", description = "新势力 ID（可选，通过 read_factions 获取）") Long factionId,
            @ToolParam(name = "roleType", description = "新角色类型（可选）：主角/配角/NPC") String roleType) {
        NovelCharacter target = findCharacterByName(name);
        if (target == null) return "角色不存在：" + name;
        if (description != null) target.setDescription(description);
        if (personality != null) target.setPersonality(personality);
        if (appearance != null) target.setAppearance(appearance);
        if (background != null) target.setBackground(background);
        if (factionId != null) target.setFactionId(factionId);
        if (roleType != null) target.setRoleType(roleType);
        characterService.updateCharacter(target.getId(), target);
        return "角色「" + name + "」已更新";
    }

    @Tool(name = "update_character_field", description = "修改角色的单个字段（更精确）")
    public String updateCharacterField(
            @ToolParam(name = "name", description = "角色名称") String name,
            @ToolParam(name = "field", description = "字段名：alias / personality / appearance / background / description / factionId / roleType") String field,
            @ToolParam(name = "value", description = "新值（factionId 需传数字）") String value) {
        NovelCharacter target = findCharacterByName(name);
        if (target == null) return "角色不存在：" + name;
        switch (field) {
            case "alias": target.setAlias(value); break;
            case "personality": target.setPersonality(value); break;
            case "appearance": target.setAppearance(value); break;
            case "background": target.setBackground(value); break;
            case "description": target.setDescription(value); break;
            case "factionId":
                try { target.setFactionId(Long.parseLong(value)); } catch (NumberFormatException e) { return "factionId 需要是数字"; }
                break;
            case "roleType": target.setRoleType(value); break;
            default: return "未知字段：" + field + "，可选：alias/personality/appearance/background/description/factionId/roleType";
        }
        characterService.updateCharacter(target.getId(), target);
        return "角色「" + name + "」的 " + field + " 已更新";
    }

    @Tool(name = "delete_character", description = "删除角色（按名称）")
    public String deleteCharacter(
            @ToolParam(name = "name", description = "角色名称") String name) {
        NovelCharacter target = findCharacterByName(name);
        if (target == null) return "角色不存在：" + name;
        characterService.deleteCharacter(target.getId());
        return "角色「" + name + "」已删除";
    }

    // ═══════════════════════════════════════════
    // 内部辅助方法
    // ═══════════════════════════════════════════

    private Chapter getChapter(int chapterNumber) {
        Chapter ch = chapterService.getChapterByNumber(novelId, chapterNumber);
        if (ch == null) return null;
        return ch;
    }

    private NovelCharacter findCharacterByName(String name) {
        return characterService.listByNovelId(novelId).stream()
                .filter(c -> c.getName().equals(name))
                .findFirst().orElse(null);
    }

    private String readTarget(String target, int chapterNumber) {
        switch (target) {
            case "synopsis": return markdownStorage.readSynopsis(workspaceDir);
            case "outline": return markdownStorage.readOutline(workspaceDir);
            case "chapter_summary": {
                Chapter ch = getChapter(chapterNumber);
                if (ch == null || ch.getMdDir() == null) return null;
                return markdownStorage.readChapterSummary(workspaceDir, ch.getMdDir());
            }
            case "chapter_content": {
                Chapter ch = getChapter(chapterNumber);
                if (ch == null || ch.getMdDir() == null) return null;
                return markdownStorage.readChapterContent(workspaceDir, ch.getMdDir());
            }
            default: return null;
        }
    }

    private void writeTarget(String target, int chapterNumber, String content) {
        switch (target) {
            case "synopsis": {
                markdownStorage.writeSynopsis(workspaceDir, content);
                Novel novel = novelService.getById(novelId);
                novel.setSynopsis(content);
                novelService.updateNovel(novelId, novel);
                break;
            }
            case "outline": {
                markdownStorage.writeOutline(workspaceDir, content);
                Novel novel = novelService.getById(novelId);
                novel.setOutline(content);
                novelService.updateNovel(novelId, novel);
                break;
            }
            case "chapter_summary": {
                Chapter ch = getChapter(chapterNumber);
                if (ch != null && ch.getMdDir() != null)
                    markdownStorage.writeChapterSummary(workspaceDir, ch.getMdDir(), content);
                break;
            }
            case "chapter_content": {
                Chapter ch = getChapter(chapterNumber);
                if (ch != null && ch.getMdDir() != null)
                    markdownStorage.writeChapterContent(workspaceDir, ch.getMdDir(), content);
                break;
            }
        }
    }

    private String targetLabel(String target, int chapterNumber) {
        switch (target) {
            case "synopsis": return "简介";
            case "outline": return "大纲";
            case "chapter_summary": return "第" + chapterNumber + "章梗概";
            case "chapter_content": return "第" + chapterNumber + "章正文";
            default: return target;
        }
    }

    /** 给文本每行加上行号前缀 */
    private String numbered(String text, String emptyHint) {
        if (text == null || text.isBlank()) return emptyHint;
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(String.format("%4d| %s", i + 1, lines[i]));
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    /** 替换文本中第 startLine 到 endLine 行（1-based，含首尾） */
    private String replaceLinesInText(String text, int startLine, int endLine, String newContent) {
        String[] lines = text.split("\n", -1);
        if (startLine < 1 || endLine > lines.length || startLine > endLine) return null;
        List<String> list = new ArrayList<>(List.of(lines));
        // 移除旧行
        for (int i = endLine - 1; i >= startLine - 1; i--) {
            list.remove(i);
        }
        if (newContent != null && !newContent.isEmpty()) {
            String[] newLines = newContent.split("\n", -1);
            int insertPos = startLine - 1;
            for (int i = newLines.length - 1; i >= 0; i--) {
                list.add(insertPos, newLines[i]);
            }
        }
        return String.join("\n", list);
    }

    /** 在第 lineNumber 行后插入内容（0 表示开头） */
    private String insertAfterLine(String text, int lineNumber, String content) {
        if (content == null || content.isEmpty()) return text;
        String[] lines = text.split("\n", -1);
        if (lineNumber < 0 || lineNumber > lines.length) return null;
        List<String> list = new ArrayList<>(List.of(lines));
        String[] newLines = content.split("\n", -1);
        if (lineNumber == 0) {
            for (int i = 0; i < newLines.length; i++) {
                list.add(i, newLines[i]);
            }
        } else {
            for (int i = newLines.length - 1; i >= 0; i--) {
                list.add(lineNumber, newLines[i]);
            }
        }
        return String.join("\n", list);
    }

    private static String nvl(String s) {
        return s != null && !s.isBlank() ? s : "无";
    }
}
