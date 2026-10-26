package com.scicrop.biblia.service;

import com.scicrop.biblia.model.DatabaseStatus;
import com.scicrop.biblia.repository.StatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusService {

    private final StatusRepository statusRepository;

    public StatusService(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @Transactional(readOnly = true)
    public DatabaseStatus getDatabaseStatus() {
        return statusRepository.findStatus();
    }
}
