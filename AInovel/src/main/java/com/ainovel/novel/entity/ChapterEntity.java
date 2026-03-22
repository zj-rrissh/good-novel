package com.ainovel.novel.entity;

import com.ainovel.novel.domain.ChapterStatus;
import java.time.LocalDateTime;

public class ChapterEntity {

    private Long id;
    private Long novelId;
    private Integer chapterNo;
    private String title;
    private String content;
    private ChapterStatus status;
    private String auditTaskId;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public Integer getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(Integer chapterNo) {
        this.chapterNo = chapterNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ChapterStatus getStatus() {
        return status;
    }

    public void setStatus(ChapterStatus status) {
        this.status = status;
    }

    public String getAuditTaskId() {
        return auditTaskId;
    }

    public void setAuditTaskId(String auditTaskId) {
        this.auditTaskId = auditTaskId;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
