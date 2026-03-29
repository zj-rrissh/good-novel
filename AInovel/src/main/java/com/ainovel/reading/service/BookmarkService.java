package com.ainovel.reading.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.reading.dto.AddBookmarkRequest;
import com.ainovel.reading.vo.BookmarkVO;

public interface BookmarkService {

    BookmarkVO addBookmark(Long userId, AddBookmarkRequest request);

    void removeBookmark(Long userId, Long bookmarkId);

    PageResponse<BookmarkVO> listBookmarks(Long userId, Long novelId, int page, int size);
}
