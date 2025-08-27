package demo.layoutviz.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "lines", indexes = {
        @Index(name = "idx_lines_name", columnList = "name"),
        @Index(name = "idx_lines_from_to", columnList = "fromAddress,toAddress")
})
public class Line {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private long fromAddress;
    private long toAddress;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "from_x")),
            @AttributeOverride(name = "y", column = @Column(name = "from_y")),
            @AttributeOverride(name = "z", column = @Column(name = "from_z"))
    })
    private Position fromPos;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "to_x")),
            @AttributeOverride(name = "y", column = @Column(name = "to_y")),
            @AttributeOverride(name = "z", column = @Column(name = "to_z"))
    })
    private Position toPos;
    private boolean curve;

    public Line() {}

    public Line(long id, String name, long fromAddress, long toAddress, Position fromPos, Position toPos, boolean curve) {
        this.id = id;
        this.name = name;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.fromPos = fromPos;
        this.toPos = toPos;
        this.curve = curve;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(long fromAddress) {
        this.fromAddress = fromAddress;
    }

    public long getToAddress() {
        return toAddress;
    }

    public void setToAddress(long toAddress) {
        this.toAddress = toAddress;
    }

    public Position getFromPos() {
        return fromPos;
    }

    public void setFromPos(Position fromPos) {
        this.fromPos = fromPos;
    }

    public Position getToPos() {
        return toPos;
    }

    public void setToPos(Position toPos) {
        this.toPos = toPos;
    }

    public boolean isCurve() {
        return curve;
    }

    public void setCurve(boolean curve) {
        this.curve = curve;
    }
}


