package com.ainovel.reading.service;

import com.ainovel.common.api.PageResponse;
import com.ainovel.reading.entity.ReadingHistoryEntity;
import com.ainovel.reading.mapper.ReadingHistoryMapper;
import com.ainovel.reading.vo.ReadingHistoryVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadingHistoryServiceImpl implements ReadingHistoryService {

    private final ReadingHistoryMapper readingHistoryMapper;

    public ReadingHistoryServiceImpl(ReadingHistoryMapper readingHistoryMapper) {
        this.readingHistoryMapper = readingHistoryMapper;
    }

    @Override
    @Transactional
    public void recordHistory(Long userId, Long novelId, Long chapterId) {
        ReadingHistoryEntity existing = readingHistoryMapper.findByUserAndNovel(userId, novelId);
        if (existing == null) {
            ReadingHistoryEntity entity = new ReadingHistoryEntity();
            entity.setUserId(userId);
            entity.setNovelId(novelId);
            entity.setChapterId(chapterId);
            readingHistoryMapper.insert(entity);
        } else {
            existing.setChapterId(chapterId);
            readingHistoryMapper.update(existing);
        }
    }

    @Override
    @Transactional
    public void deleteHistory(Long userId, Long novelId) {
        readingHistoryMapper.deleteByUserAndNovel(userId, novelId);
    }

    @Override
    public PageResponse<ReadingHistoryVO> listHistory(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<ReadingHistoryVO> records = readingHistoryMapper.findRecentByUser(userId, offset, size)
                .stream().map(this::toVO).toList();
        long total = readingHistoryMapper.countByUser(userId);
        return PageResponse.of(records, total, page, size);
    }

    private ReadingHistoryVO toVO(ReadingHistoryEntity entity) {
        return new ReadingHistoryVO(
                entity.getNovelId(),
                entity.getChapterId(),
                entity.getLastReadAt());
    }
}
