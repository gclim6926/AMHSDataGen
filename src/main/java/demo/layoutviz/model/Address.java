package demo.layoutviz.model;

public class Address {
    private long id;
    private long address;
    private String name;
    private Position pos;

    public Address() {}

    public Address(long id, long address, String name, Position pos) {
        this.id = id;
        this.address = address;
        this.name = name;
        this.pos = pos;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAddress() {
        return address;
    }

    public void setAddress(long address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Position getPos() {
        return pos;
    }

    public void setPos(Position pos) {
        this.pos = pos;
    }
}


