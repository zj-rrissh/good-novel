package com.ainovel.reading.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.common.api.StandardErrorCode;
import com.ainovel.infrastructure.exception.BusinessException;
import com.ainovel.novel.mapper.ChapterMapper;
import com.ainovel.reading.dto.AddBookmarkRequest;
import com.ainovel.reading.entity.BookmarkEntity;
import com.ainovel.reading.mapper.BookmarkMapper;
import com.ainovel.reading.vo.BookmarkVO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkMapper bookmarkMapper;
    private final ChapterMapper chapterMapper;

    public BookmarkServiceImpl(BookmarkMapper bookmarkMapper, ChapterMapper chapterMapper) {
        this.bookmarkMapper = bookmarkMapper;
        this.chapterMapper = chapterMapper;
    }

    @Override
    @Transactional
    public BookmarkVO addBookmark(Long userId, AddBookmarkRequest request) {
        var chapter = chapterMapper.findById(request.chapterId());
        if (chapter == null || !chapter.getNovelId().equals(request.novelId())) {
            throw new BusinessException(StandardErrorCode.INVALID_REQUEST, "chapter does not belong to novel");
        }
        BookmarkEntity existing = bookmarkMapper.findByUserAndChapter(userId, request.chapterId());
        if (existing != null) {
            return toVO(existing);
        }
        BookmarkEntity entity = new BookmarkEntity();
        entity.setUserId(userId);
        entity.setNovelId(request.novelId());
        entity.setChapterId(request.chapterId());
        entity.setPageOffset(Optional.ofNullable(request.pageOffset()).orElse(0L));
        entity.setNote(request.note());
        bookmarkMapper.insert(entity);
        return toVO(bookmarkMapper.findByUserAndChapter(userId, request.chapterId()));
    }

    @Override
    @Transactional
    public void removeBookmark(Long userId, Long bookmarkId) {
        int rows = bookmarkMapper.deleteById(bookmarkId, userId);
        if (rows == 0) {
            throw new BusinessException(StandardErrorCode.BUSINESS_STATE_INVALID, "bookmark not found");
        }
    }

    @Override
    public PageResponse<BookmarkVO> listBookmarks(Long userId, Long novelId, int page, int size) {
        int offset = (page - 1) * size;
        List<BookmarkVO> records;
        long total;
        if (novelId != null) {
            records = bookmarkMapper.findByUserAndNovel(userId, novelId, offset, size)
                    .stream().map(this::toVO).toList();
            total = bookmarkMapper.countByUserAndNovel(userId, novelId);
        } else {
            records = bookmarkMapper.findAllByUser(userId, offset, size)
                    .stream().map(this::toVO).toList();
            total = bookmarkMapper.countAllByUser(userId);
        }
        return PageResponse.of(records, total, page, size);
    }

    private BookmarkVO toVO(BookmarkEntity entity) {
        return new BookmarkVO(
                entity.getId(),
                entity.getNovelId(),
                entity.getChapterId(),
                entity.getPageOffset(),
                entity.getNote(),
                entity.getCreatedAt());
    }
}
