package com.example.crossborder.service;
import com.example.crossborder.model.AdminSettings;
import com.example.crossborder.repository.AdminSettingsRepository;
import org.springframework.stereotype.Service;
@Service
public class AdminSettingsService {
    private final AdminSettingsRepository repository;
    public AdminSettingsService(AdminSettingsRepository repository) { this.repository = repository; }
    public AdminSettings get() { return repository.get(); }
    public AdminSettings save(AdminSettings settings) { return repository.save(settings); }
}
