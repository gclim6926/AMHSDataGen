package demo.amhsdatagen.repository;

import demo.amhsdatagen.model.ConfigEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigEntryRepository extends JpaRepository<ConfigEntry, Long> {
    Optional<ConfigEntry> findByConfigKey(String configKey);
}


