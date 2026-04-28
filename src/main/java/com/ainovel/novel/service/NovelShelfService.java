package com.ainovel.novel.service;

public interface NovelShelfService {

    void onShelf(Long novelId, String reason);

    void offShelf(Long novelId, String reason);

    void ban(Long novelId, String reason);
}
