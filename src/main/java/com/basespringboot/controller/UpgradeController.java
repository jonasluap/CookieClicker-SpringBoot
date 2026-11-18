package com.basespringboot.controller;

import com.basespringboot.dto.UpgradeRequest;
import com.basespringboot.dto.UpgradeResponse;
import com.basespringboot.service.UpgradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/upgrades")
@RequiredArgsConstructor
public class UpgradeController {

    private final UpgradeService upgradeService;

    @PostMapping
    public ResponseEntity<UpgradeResponse> create(@Valid @RequestBody UpgradeRequest request) {
        UpgradeResponse upgrade = upgradeService.create(request);
        return ResponseEntity.created(URI.create("/api/upgrades/" + upgrade.id())).body(upgrade);
    }

    @GetMapping
    public List<UpgradeResponse> findAll() {
        return upgradeService.findAll();
    }

    @GetMapping("/{id}")
    public UpgradeResponse findById(@PathVariable Long id) {
        return upgradeService.findById(id);
    }

    @PutMapping("/{id}")
    public UpgradeResponse update(@PathVariable Long id, @Valid @RequestBody UpgradeRequest request) {
        return upgradeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        upgradeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
