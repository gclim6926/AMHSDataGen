package demo.amhsdatagen.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "stations", indexes = {
        @Index(name = "idx_stations_name", columnList = "name")
})
public class Station {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String type;
    private String port;
    @Embedded
    private Position pos;

    public Station() {}

    public Station(long id, String name, String type, String port, Position pos) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.port = port;
        this.pos = pos;
    }

    public Station(String name, String type, String port, Position pos) {
        this.name = name;
        this.type = type;
        this.port = port;
        this.pos = pos;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }
    public Position getPos() { return pos; }
    public void setPos(Position pos) { this.pos = pos; }
}


