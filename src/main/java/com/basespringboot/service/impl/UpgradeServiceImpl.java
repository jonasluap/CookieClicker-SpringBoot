package com.basespringboot.service.impl;

import com.basespringboot.dto.UpgradeRequest;
import com.basespringboot.dto.UpgradeResponse;
import com.basespringboot.entity.Upgrade;
import com.basespringboot.exception.ConflictException;
import com.basespringboot.exception.ResourceNotFoundException;
import com.basespringboot.repository.PurchaseRepository;
import com.basespringboot.repository.UpgradeRepository;
import com.basespringboot.service.UpgradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UpgradeServiceImpl implements UpgradeService {

    private final UpgradeRepository upgradeRepository;
    private final PurchaseRepository purchaseRepository;

    @Override
    public UpgradeResponse create(UpgradeRequest request) {
        validateBonus(request);
        String name = request.name().trim();
        if (upgradeRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Upgrade name already exists");
        }
        Upgrade upgrade = new Upgrade();
        applyRequest(upgrade, request, name);
        return toResponse(upgradeRepository.save(upgrade));
    }

    @Override
    @Transactional(readOnly = true)
    public UpgradeResponse findById(Long id) {
        return toResponse(requireUpgrade(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpgradeResponse> findAll() {
        return upgradeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public UpgradeResponse update(Long id, UpgradeRequest request) {
        validateBonus(request);
        Upgrade upgrade = requireUpgrade(id);
        String name = request.name().trim();
        if (!upgrade.getName().equalsIgnoreCase(name)
                && upgradeRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Upgrade name already exists");
        }
        applyRequest(upgrade, request, name);
        return toResponse(upgrade);
    }

    @Override
    public void delete(Long id) {
        Upgrade upgrade = requireUpgrade(id);
        if (purchaseRepository.existsByUpgradeId(id)) {
            throw new ConflictException("Upgrade has purchase history and cannot be deleted");
        }
        upgradeRepository.delete(upgrade);
    }

    private void validateBonus(UpgradeRequest request) {
        if (request.clickBonus() == 0 && request.passiveBonus() == 0) {
            throw new IllegalArgumentException("At least one bonus must be greater than zero");
        }
    }

    private void applyRequest(Upgrade upgrade, UpgradeRequest request, String name) {
        upgrade.setName(name);
        upgrade.setDescription(request.description());
        upgrade.setPrice(request.price());
        upgrade.setClickBonus(request.clickBonus());
        upgrade.setPassiveBonus(request.passiveBonus());
    }

    private Upgrade requireUpgrade(Long id) {
        return upgradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Upgrade not found: " + id));
    }

    private UpgradeResponse toResponse(Upgrade upgrade) {
        return new UpgradeResponse(upgrade.getId(), upgrade.getName(), upgrade.getDescription(),
                upgrade.getPrice(), upgrade.getClickBonus(), upgrade.getPassiveBonus());
    }
}
