package demo.amhsdatagen.model;

import jakarta.persistence.*;

@Entity
@Table(name = "\"AMHS_data\"")
public class ConfigEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", unique = true, nullable = false, length = 128)
    private String configKey;

    @Lob
    @Column(name = "config_value", nullable = false)
    private String configValue;

    protected ConfigEntry() {}

    public ConfigEntry(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public Long getId() { return id; }

    public String getConfigKey() { return configKey; }

    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }

    public void setConfigValue(String configValue) { this.configValue = configValue; }
}


