package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import demo.amhsdatagen.model.Address;
import demo.amhsdatagen.model.Line;
import demo.amhsdatagen.model.Position;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LineEndpointService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConfigService configService;

    public LineEndpointService(ConfigService configService) {
        this.configService = configService;
    }

    public Result runAddLines() throws IOException {
        JsonNode root = configService.loadOutputFromDb().orElseThrow(() -> new IOException("output not found in DB. Run generate first."));
        List<Address> addresses = readAddresses(root.path("addresses"));
        List<Line> lines = readLines(root.path("lines"));

        AtomicLong nextLineId = new AtomicLong(Math.max(200000L, lines.stream().mapToLong(Line::getId).max().orElse(200000L)) + 1);

        int addedPhase1 = connectUnusedAddresses(addresses, lines, nextLineId::getAndIncrement);

        // Save intermediate to DB
        ObjectNode first = buildOutput(addresses, lines);
        configService.saveOutputToDb(first);

        // Recompute endpoint addresses based on updated lines
        int addedPhase2 = connectEndpointAddresses(addresses, lines, nextLineId::getAndIncrement);

        ObjectNode finalOut = buildOutput(addresses, lines);
        configService.saveOutputToDb(finalOut);

        Result r = new Result();
        r.addedPhase1 = addedPhase1;
        r.addedPhase2 = addedPhase2;
        r.totalLines = lines.size();
        r.outputPath = "db://AMHS_data:layout_seed.output";
        return r;
    }

    private int connectUnusedAddresses(List<Address> addresses, List<Line> lines, IdSupplier idSupplier) {
        Set<Long> used = collectUsedAddressIds(lines);
        List<Address> unused = new ArrayList<>();
        for (Address a : addresses) {
            if (!used.contains(a.getId())) unused.add(a);
        }

        int added = 0;
        for (Address a : unused) {
            List<AddressDistance> nearest = findNearest(addresses, a, 2, Collections.emptySet());
            for (int i = 0; i < nearest.size(); i++) {
                Address b = nearest.get(i).address;
                if (!lineExists(lines, a.getId(), b.getId())) {
                    lines.add(new Line(idSupplier.nextId(),
                            "LINE_" + a.getId() + "_" + b.getId(),
                            a.getId(), b.getId(), a.getPos(), b.getPos(), false));
                    added++;
                }
            }
        }
        return added;
    }

    private int connectEndpointAddresses(List<Address> addresses, List<Line> lines, IdSupplier idSupplier) {
        Map<Long, Integer> counts = usageCounts(lines);
        List<Address> endpoints = new ArrayList<>();
        for (Address a : addresses) {
            int c = counts.getOrDefault(a.getId(), 0);
            if (c == 1) endpoints.add(a);
        }

        int added = 0;
        for (Address a : endpoints) {
            Set<Long> connectedIds = connectedAddressIds(lines, a.getId());
            connectedIds.add(a.getId());
            List<AddressDistance> nearestList = findNearest(addresses, a, addresses.size(), connectedIds);
            if (!nearestList.isEmpty()) {
                Address b = nearestList.get(0).address;
                if (!lineExists(lines, a.getId(), b.getId())) {
                    lines.add(new Line(idSupplier.nextId(),
                            "LINE_" + a.getId() + "_" + b.getId(),
                            a.getId(), b.getId(), a.getPos(), b.getPos(), false));
                    added++;
                }
            }
        }
        return added;
    }

    private Set<Long> connectedAddressIds(List<Line> lines, long sourceId) {
        Set<Long> set = new HashSet<>();
        for (Line l : lines) {
            if (l.getFromAddress() == sourceId) set.add(l.getToAddress());
            if (l.getToAddress() == sourceId) set.add(l.getFromAddress());
        }
        return set;
    }

    private Map<Long, Integer> usageCounts(List<Line> lines) {
        Map<Long, Integer> counts = new HashMap<>();
        for (Line l : lines) {
            counts.put(l.getFromAddress(), counts.getOrDefault(l.getFromAddress(), 0) + 1);
            counts.put(l.getToAddress(), counts.getOrDefault(l.getToAddress(), 0) + 1);
        }
        return counts;
    }

    private boolean lineExists(List<Line> lines, long a, long b) {
        for (Line l : lines) {
            if ((l.getFromAddress() == a && l.getToAddress() == b) || (l.getFromAddress() == b && l.getToAddress() == a)) {
                return true;
            }
        }
        return false;
    }

    private List<AddressDistance> findNearest(List<Address> all, Address src, int count, Set<Long> exclude) {
        List<AddressDistance> list = new ArrayList<>();
        for (Address a : all) {
            if (a.getId() == src.getId()) continue;
            if (exclude.contains(a.getId())) continue;
            list.add(new AddressDistance(a, distance(src.getPos(), a.getPos())));
        }
        list.sort(Comparator.comparingDouble(ad -> ad.distance));
        if (count >= list.size()) return list;
        return list.subList(0, count);
        
    }

    private double distance(Position p1, Position p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double dz = p2.getZ() - p1.getZ();
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    private List<Address> readAddresses(JsonNode node) {
        List<Address> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                long id = n.path("id").asLong();
                long address = n.path("address").asLong(id);
                String name = n.path("name").asText("ADDR_" + id);
                JsonNode pos = n.path("pos");
                Position position = new Position(pos.path("x").asDouble(), pos.path("y").asDouble(), pos.path("z").asDouble());
                list.add(new Address(id, address, name, position));
            }
        }
        return list;
    }

    private List<Line> readLines(JsonNode node) {
        List<Line> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                long id = n.path("id").asLong();
                String name = n.path("name").asText();
                long from = n.path("fromAddress").asLong();
                long to = n.path("toAddress").asLong();
                JsonNode fp = n.path("fromPos");
                JsonNode tp = n.path("toPos");
                Position fromPos = new Position(fp.path("x").asDouble(), fp.path("y").asDouble(), fp.path("z").asDouble());
                Position toPos = new Position(tp.path("x").asDouble(), tp.path("y").asDouble(), tp.path("z").asDouble());
                boolean curve = n.path("curve").asBoolean(false);
                list.add(new Line(id, name, from, to, fromPos, toPos, curve));
            }
        }
        return list;
    }

    private ObjectNode buildOutput(List<Address> addresses, List<Line> lines) {
        ObjectNode out = objectMapper.createObjectNode();
        ArrayNode aArr = out.putArray("addresses");
        for (Address a : addresses) {
            ObjectNode aNode = aArr.addObject();
            aNode.put("id", a.getId());
            aNode.put("address", a.getAddress());
            aNode.put("name", a.getName());
            ObjectNode p = aNode.putObject("pos");
            p.put("x", a.getPos().getX());
            p.put("y", a.getPos().getY());
            p.put("z", a.getPos().getZ());
        }
        ArrayNode lArr = out.putArray("lines");
        for (Line l : lines) {
            ObjectNode lNode = lArr.addObject();
            lNode.put("id", l.getId());
            lNode.put("name", l.getName());
            lNode.put("fromAddress", l.getFromAddress());
            lNode.put("toAddress", l.getToAddress());
            ObjectNode fp = lNode.putObject("fromPos");
            fp.put("x", l.getFromPos().getX());
            fp.put("y", l.getFromPos().getY());
            fp.put("z", l.getFromPos().getZ());
            ObjectNode tp = lNode.putObject("toPos");
            tp.put("x", l.getToPos().getX());
            tp.put("y", l.getToPos().getY());
            tp.put("z", l.getToPos().getZ());
            lNode.put("curve", l.isCurve());
        }
        return out;
    }

    private Set<Long> collectUsedAddressIds(List<Line> lines) {
        Set<Long> used = new HashSet<>();
        for (Line l : lines) {
            used.add(l.getFromAddress());
            used.add(l.getToAddress());
        }
        return used;
    }

    private static class AddressDistance {
        Address address;
        double distance;
        AddressDistance(Address address, double distance) {
            this.address = address; this.distance = distance;
        }
    }

    @FunctionalInterface
    private interface IdSupplier {
        long nextId();
    }

    public static class Result {
        public int addedPhase1;
        public int addedPhase2;
        public int totalLines;
        public String outputPath;
    }
}


