package demo.layoutviz.model;

public class Line {
    private long id;
    private String name;
    private long fromAddress;
    private long toAddress;
    private Position fromPos;
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


