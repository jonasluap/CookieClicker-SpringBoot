package com.basespringboot.service;

import com.basespringboot.dto.UpgradeRequest;
import com.basespringboot.dto.UpgradeResponse;

import java.util.List;

public interface UpgradeService {
    UpgradeResponse create(UpgradeRequest request);
    UpgradeResponse findById(Long id);
    List<UpgradeResponse> findAll();
    UpgradeResponse update(Long id, UpgradeRequest request);
    void delete(Long id);
}
