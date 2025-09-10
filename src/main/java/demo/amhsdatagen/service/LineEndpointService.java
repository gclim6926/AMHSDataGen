package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    public Result runAddLines(String userId) throws IOException {
        System.out.println("🚀 Starting Line Generation Process...");
        System.out.println("📋 User ID: " + userId);
        System.out.println("⏰ Process started at: " + java.time.LocalDateTime.now());
        
        System.out.println("📂 Loading existing data from database...");
        JsonNode root = configService.loadOutputFromDb(userId).orElseThrow(() -> new IOException("output not found in DB. Run generate first."));
        List<AddressData> addresses = readAddresses(root.path("addresses"));
        List<LineData> lines = readLines(root.path("lines"));
        System.out.println("✅ Data loaded successfully");
        System.out.println("📊 Initial data: " + addresses.size() + " addresses, " + lines.size() + " lines");

        AtomicLong nextLineId = new AtomicLong(Math.max(200000L, lines.stream().mapToLong(LineData::getId).max().orElse(200000L)) + 1);
        System.out.println("🔢 Next available line ID: " + nextLineId.get());

        System.out.println("🔄 Phase 1: Connecting unused addresses...");
        int addedPhase1 = connectUnusedAddresses(addresses, lines, nextLineId::getAndIncrement);
        System.out.println("✅ Phase 1 completed - Added " + addedPhase1 + " lines");

        System.out.println("💾 Saving intermediate results to database...");
        // Save intermediate to DB
        ObjectNode first = buildOutput(addresses, lines);
        configService.saveOutputToDb(userId, first);
        System.out.println("✅ Intermediate data saved");

        System.out.println("🔄 Phase 2: Connecting endpoint addresses...");
        // Recompute endpoint addresses based on updated lines
        int addedPhase2 = connectEndpointAddresses(addresses, lines, nextLineId::getAndIncrement);
        System.out.println("✅ Phase 2 completed - Added " + addedPhase2 + " lines");

        System.out.println("💾 Saving final results to database...");
        ObjectNode finalOut = buildOutput(addresses, lines);
        configService.saveOutputToDb(userId, finalOut);
        
        System.out.println("📊 Final data summary:");
        System.out.println("   - Total Addresses: " + addresses.size());
        System.out.println("   - Total Lines: " + lines.size());
        System.out.println("   - Phase 1 Added: " + addedPhase1 + " lines");
        System.out.println("   - Phase 2 Added: " + addedPhase2 + " lines");
        System.out.println("   - Database Key: layout_seed.output");
        System.out.println("✅ Data successfully saved to database");
        System.out.println("🎉 Line Generation Process completed successfully!");
        System.out.println("⏰ Process finished at: " + java.time.LocalDateTime.now());

        Result r = new Result();
        r.addedPhase1 = addedPhase1;
        r.addedPhase2 = addedPhase2;
        r.totalLines = lines.size();
        r.outputPath = "db://" + userId + "_amhs_data:layout_seed.output";
        return r;
    }

    private int connectUnusedAddresses(List<AddressData> addresses, List<LineData> lines, IdSupplier idSupplier) {
        Set<Long> used = collectUsedAddressIds(lines);
        List<AddressData> unused = new ArrayList<>();
        for (AddressData a : addresses) {
            if (!used.contains(a.getId())) unused.add(a);
        }

        int added = 0;
        for (AddressData a : unused) {
            List<AddressDistance> nearest = findNearest(addresses, a, 2, Collections.emptySet());
            for (int i = 0; i < nearest.size(); i++) {
                AddressData b = nearest.get(i).address;
                if (!lineExists(lines, a.getId(), b.getId())) {
                    lines.add(new LineData(idSupplier.nextId(),
                            "LINE_" + a.getId() + "_" + b.getId(),
                            a.getId(), b.getId(), a.getPos(), b.getPos(), false));
                    added++;
                }
            }
        }
        return added;
    }

    private int connectEndpointAddresses(List<AddressData> addresses, List<LineData> lines, IdSupplier idSupplier) {
        Map<Long, Integer> counts = usageCounts(lines);
        List<AddressData> endpoints = new ArrayList<>();
        for (AddressData a : addresses) {
            int c = counts.getOrDefault(a.getId(), 0);
            if (c == 1) endpoints.add(a);
        }

        int added = 0;
        for (AddressData a : endpoints) {
            Set<Long> connectedIds = connectedAddressIds(lines, a.getId());
            connectedIds.add(a.getId());
            List<AddressDistance> nearestList = findNearest(addresses, a, addresses.size(), connectedIds);
            if (!nearestList.isEmpty()) {
                AddressData b = nearestList.get(0).address;
                if (!lineExists(lines, a.getId(), b.getId())) {
                    lines.add(new LineData(idSupplier.nextId(),
                            "LINE_" + a.getId() + "_" + b.getId(),
                            a.getId(), b.getId(), a.getPos(), b.getPos(), false));
                    added++;
                }
            }
        }
        return added;
    }

    private Set<Long> connectedAddressIds(List<LineData> lines, long sourceId) {
        Set<Long> set = new HashSet<>();
        for (LineData l : lines) {
            if (l.getFromAddress() == sourceId) set.add(l.getToAddress());
            if (l.getToAddress() == sourceId) set.add(l.getFromAddress());
        }
        return set;
    }

    private Map<Long, Integer> usageCounts(List<LineData> lines) {
        Map<Long, Integer> counts = new HashMap<>();
        for (LineData l : lines) {
            counts.put(l.getFromAddress(), counts.getOrDefault(l.getFromAddress(), 0) + 1);
            counts.put(l.getToAddress(), counts.getOrDefault(l.getToAddress(), 0) + 1);
        }
        return counts;
    }

    private boolean lineExists(List<LineData> lines, long a, long b) {
        for (LineData l : lines) {
            if ((l.getFromAddress() == a && l.getToAddress() == b) || (l.getFromAddress() == b && l.getToAddress() == a)) {
                return true;
            }
        }
        return false;
    }

    private List<AddressDistance> findNearest(List<AddressData> all, AddressData src, int count, Set<Long> exclude) {
        List<AddressDistance> list = new ArrayList<>();
        for (AddressData a : all) {
            if (a.getId() == src.getId()) continue;
            if (exclude.contains(a.getId())) continue;
            list.add(new AddressDistance(a, distance(src.getPos(), a.getPos())));
        }
        list.sort(Comparator.comparingDouble(ad -> ad.distance));
        if (count >= list.size()) return list;
        return list.subList(0, count);
    }

    private double distance(PositionData p1, PositionData p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double dz = p2.getZ() - p1.getZ();
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    private List<AddressData> readAddresses(JsonNode node) {
        List<AddressData> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                long id = n.path("id").asLong();
                long address = n.path("address").asLong(id);
                String name = n.path("name").asText("ADDR_" + id);
                JsonNode pos = n.path("pos");
                PositionData position = new PositionData(pos.path("x").asDouble(), pos.path("y").asDouble(), pos.path("z").asDouble());
                list.add(new AddressData(id, address, name, position));
            }
        }
        return list;
    }

    private List<LineData> readLines(JsonNode node) {
        List<LineData> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                long id = n.path("id").asLong();
                String name = n.path("name").asText();
                long from = n.path("fromAddress").asLong();
                long to = n.path("toAddress").asLong();
                JsonNode fp = n.path("fromPos");
                JsonNode tp = n.path("toPos");
                PositionData fromPos = new PositionData(fp.path("x").asDouble(), fp.path("y").asDouble(), fp.path("z").asDouble());
                PositionData toPos = new PositionData(tp.path("x").asDouble(), tp.path("y").asDouble(), tp.path("z").asDouble());
                boolean curve = n.path("curve").asBoolean(false);
                list.add(new LineData(id, name, from, to, fromPos, toPos, curve));
            }
        }
        return list;
    }

    private ObjectNode buildOutput(List<AddressData> addresses, List<LineData> lines) {
        ObjectNode out = objectMapper.createObjectNode();
        ArrayNode aArr = out.putArray("addresses");
        for (AddressData a : addresses) {
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
        for (LineData l : lines) {
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

    private Set<Long> collectUsedAddressIds(List<LineData> lines) {
        Set<Long> used = new HashSet<>();
        for (LineData l : lines) {
            used.add(l.getFromAddress());
            used.add(l.getToAddress());
        }
        return used;
    }

    // Inner classes for data transfer
    private static class AddressData {
        private final long id;
        private final long address;
        private final String name;
        private final PositionData pos;

        public AddressData(long id, long address, String name, PositionData pos) {
            this.id = id;
            this.address = address;
            this.name = name;
            this.pos = pos;
        }

        public long getId() { return id; }
        public long getAddress() { return address; }
        public String getName() { return name; }
        public PositionData getPos() { return pos; }
    }

    private static class PositionData {
        private final double x, y, z;

        public PositionData(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }

    private static class LineData {
        private final long id;
        private final String name;
        private final long fromAddress;
        private final long toAddress;
        private final PositionData fromPos;
        private final PositionData toPos;
        private final boolean curve;

        public LineData(long id, String name, long fromAddress, long toAddress, PositionData fromPos, PositionData toPos, boolean curve) {
            this.id = id;
            this.name = name;
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
            this.fromPos = fromPos;
            this.toPos = toPos;
            this.curve = curve;
        }

        public long getId() { return id; }
        public String getName() { return name; }
        public long getFromAddress() { return fromAddress; }
        public long getToAddress() { return toAddress; }
        public PositionData getFromPos() { return fromPos; }
        public PositionData getToPos() { return toPos; }
        public boolean isCurve() { return curve; }
    }

    private static class AddressDistance {
        AddressData address;
        double distance;
        AddressDistance(AddressData address, double distance) {
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