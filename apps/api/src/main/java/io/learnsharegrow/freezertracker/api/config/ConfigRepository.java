package io.learnsharegrow.freezertracker.api.config;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRepository extends JpaRepository<ConfigEntry, String> {}
