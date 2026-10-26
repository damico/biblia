package com.scicrop.biblia.service;

import com.scicrop.biblia.model.SourceRecord;
import com.scicrop.biblia.repository.SourceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SourceRecordService {

    private final SourceRecordRepository sourceRecordRepository;

    public SourceRecordService(SourceRecordRepository sourceRecordRepository) {
        this.sourceRecordRepository = sourceRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<SourceRecord> listAll() {
        return sourceRecordRepository.findAll();
    }
}
