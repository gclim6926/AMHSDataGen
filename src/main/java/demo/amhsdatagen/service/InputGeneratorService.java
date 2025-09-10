package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Service
public class InputGeneratorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    private final ConfigService configService;

    public InputGeneratorService(ConfigService configService) {
        this.configService = configService;
    }

    public Result runGenerate(String userId) throws IOException {
        System.out.println("🚀 Starting Address Generation Process...");
        System.out.println("📋 User ID: " + userId);
        System.out.println("⏰ Process started at: " + java.time.LocalDateTime.now());
        
        // 입력 로드 정책: DB 우선, 없으면 샘플 사용
        System.out.println("📂 Loading input data from database...");
        JsonNode root = configService.loadInputFromDb(userId).orElseThrow(() -> new RuntimeException("Input data not found"));
        System.out.println("✅ Input data loaded successfully");
        List<String> keys = new ArrayList<>();
        root.fieldNames().forEachRemaining(keys::add);
        System.out.println("📊 Available data keys: " + String.join(", ", keys));

        long addressId = GenerationConfig.ADDRESS_ID_START;
        long lineId = GenerationConfig.LINE_ID_START;
        List<AddressData> addresses = new ArrayList<>();
        List<LineData> lines = new ArrayList<>();

        System.out.println("🔄 Processing layer crossover connections...");
        // Layer crossover connections (z0-4822, z4822-6022) first
        addressId = processLayerCrossover(root.path("layer_crossover"), addresses, lines, addressId, lineId);
        lineId = GenerationConfig.LINE_ID_START + lines.size();
        System.out.println("✅ Layer crossover processing completed - Addresses: " + addresses.size() + ", Lines: " + lines.size());

        System.out.println("🏭 Processing z6022 layer (top floor)...");
        // Process z6022/z4822 central/local loops with 2D points
        addressId = processLayer(root.path("z6022"), 6022.0, addresses, lines, addressId, lineId);
        lineId = GenerationConfig.LINE_ID_START + lines.size();
        System.out.println("✅ z6022 layer processing completed - Addresses: " + addresses.size() + ", Lines: " + lines.size());
        
        System.out.println("🏭 Processing z4822 layer (middle floor)...");
        addressId = processLayer(root.path("z4822"), 4822.0, addresses, lines, addressId, lineId);
        lineId = GenerationConfig.LINE_ID_START + lines.size();
        System.out.println("✅ z4822 layer processing completed - Addresses: " + addresses.size() + ", Lines: " + lines.size());

        // Save output (DB)
        ObjectNode output = objectMapper.createObjectNode();
        ArrayNode addressesNode = output.putArray("addresses");
        for (AddressData a : addresses) {
            ObjectNode aNode = addressesNode.addObject();
            aNode.put("id", a.getId());
            aNode.put("address", a.getAddress());
            aNode.put("name", a.getName());
            ObjectNode pos = aNode.putObject("pos");
            pos.put("x", round1(a.getPos().getX()));
            pos.put("y", round1(a.getPos().getY()));
            pos.put("z", a.getPos().getZ());
        }
        ArrayNode linesNode = output.putArray("lines");
        for (LineData l : lines) {
            ObjectNode lNode = linesNode.addObject();
            lNode.put("id", l.getId());
            lNode.put("name", l.getName());
            lNode.put("fromAddress", l.getFromAddress());
            lNode.put("toAddress", l.getToAddress());
            ObjectNode fromPos = lNode.putObject("fromPos");
            fromPos.put("x", round1(l.getFromPos().getX()));
            fromPos.put("y", round1(l.getFromPos().getY()));
            fromPos.put("z", l.getFromPos().getZ());
            ObjectNode toPos = lNode.putObject("toPos");
            toPos.put("x", round1(l.getToPos().getX()));
            toPos.put("y", round1(l.getToPos().getY()));
            toPos.put("z", l.getToPos().getZ());
            lNode.put("curve", l.isCurve());
        }

        System.out.println("💾 Saving data to database...");
        System.out.println("📊 Data summary:");
        System.out.println("   - Total Addresses: " + addresses.size());
        System.out.println("   - Total Lines: " + lines.size());
        System.out.println("   - Database Key: layout_seed.output");
        
        // Save output to DB
        configService.saveOutputToDb(userId, output);
        
        System.out.println("✅ Data successfully saved to database");
        System.out.println("🎉 Address Generation Process completed successfully!");
        System.out.println("⏰ Process finished at: " + java.time.LocalDateTime.now());

        Result res = new Result();
        res.addressCount = addresses.size();
        res.lineCount = lines.size();
        res.outputPath = "db://" + userId + "_amhs_data:layout_seed.output";
        return res;
    }
    
    private long processLayerCrossover(JsonNode lcNode,
                                       List<AddressData> addresses,
                                       List<LineData> lines,
                                       long currentAddressId,
                                       long currentLineIdStart) {
        if (lcNode == null || lcNode.isMissingNode()) return currentAddressId;
        currentAddressId = processConnectionArray(lcNode.path("z0-4822"), addresses, lines, currentAddressId);
        currentAddressId = processConnectionArray(lcNode.path("z4822-6022"), addresses, lines, currentAddressId);
        return currentAddressId;
    }

    private long processConnectionArray(JsonNode arr,
                                        List<AddressData> addresses,
                                        List<LineData> lines,
                                        long currentAddressId) {
        if (arr == null || !arr.isArray()) return currentAddressId;
        for (JsonNode pair : arr) {
            if (pair.isArray() && pair.size() == 2) {
                JsonNode s = pair.get(0);
                JsonNode t = pair.get(1);
                if (s.isArray() && s.size() >= 3 && t.isArray() && t.size() >= 3) {
                    double sx = s.get(0).asDouble();
                    double sy = s.get(1).asDouble();
                    double sz = s.get(2).asDouble();
                    double tx = t.get(0).asDouble();
                    double ty = t.get(1).asDouble();
                    double tz = t.get(2).asDouble();

                    long startId = currentAddressId;
                    AddressData aStart = new AddressData(startId, startId, "ADDR_" + startId, new PositionData(round1(sx), round1(sy), sz));
                    addresses.add(aStart);
                    currentAddressId++;
                    long endId = currentAddressId;
                    AddressData aEnd = new AddressData(endId, endId, "ADDR_" + endId, new PositionData(round1(tx), round1(ty), tz));
                    addresses.add(aEnd);
                    currentAddressId++;

                    long newLineId = GenerationConfig.LINE_ID_START + lines.size();
                    LineData l = new LineData(newLineId, "LINE_" + newLineId, startId, endId,
                            aStart.getPos(), aEnd.getPos(), false);
                    lines.add(l);
                }
            }
        }
        return currentAddressId;
    }

    private long processLayer(JsonNode layerNode, double zValue,
                              List<AddressData> addresses, List<LineData> lines,
                              long addressIdStart, long lineIdStart) {
        long currentAddressId = addressIdStart;
        long currentLineId = lineIdStart;

        // central_loop
        currentAddressId = processLinesArray(layerNode.path("central_loop"), zValue, addresses, lines, currentAddressId, currentLineId);
        currentLineId = lineIdStart + lines.size();

        // local_loop
        currentAddressId = processLinesArray(layerNode.path("local_loop"), zValue, addresses, lines, currentAddressId, currentLineId);

        // local_loop_for_layer
        currentAddressId = processLinesArray(layerNode.path("local_loop_for_layer"), zValue, addresses, lines, currentAddressId, currentLineId);

        return currentAddressId;
    }

    private long processLinesArray(JsonNode linesArrayNode, double zValue,
                                   List<AddressData> addresses, List<LineData> lines,
                                   long currentAddressId, long currentLineId) {
        if (!linesArrayNode.isArray()) return currentAddressId;
        Iterator<JsonNode> it = linesArrayNode.elements();
        while (it.hasNext()) {
            JsonNode lineNode = it.next();
            if (lineNode.isArray() && lineNode.size() == 2) {
                double[] start = readPoint2D(lineNode.get(0));
                double[] end = readPoint2D(lineNode.get(1));
                List<AddressData> generated = generateAddressesOnLine(start, end, zValue, currentAddressId);
                addresses.addAll(generated);
                if (generated.size() > 1) {
                    List<LineData> genLines = generateLinesFromAddresses(generated, currentLineId);
                    lines.addAll(genLines);
                    currentLineId += genLines.size();
                }
                currentAddressId += generated.size();
            }
        }
        return currentAddressId;
    }

    private double[] readPoint2D(JsonNode node) {
        // Accept [x, y] or [x, y, z]
        if (node.isArray() && node.size() >= 2) {
            return new double[] { node.get(0).asDouble(), node.get(1).asDouble() };
        }
        return new double[] {0.0, 0.0};
    }

    private List<AddressData> generateAddressesOnLine(double[] start, double[] end, double zValue, long startId) {
        List<AddressData> list = new ArrayList<>();
        double x1 = start[0], y1 = start[1];
        double x2 = end[0], y2 = end[1];
        double dx = x2 - x1, dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);

        long id = startId;
        // start
        list.add(new AddressData(id, id, "ADDR_" + id, new PositionData(round1(x1), round1(y1), zValue)));
        id++;

        double current = 0.0;
        while (current < length) {
            double step = pickInterval();
            current += step;
            if (current < length) {
                double ratio = current / length;
                double x = x1 + ratio * dx;
                double y = y1 + ratio * dy;
                list.add(new AddressData(id, id, "ADDR_" + id, new PositionData(round1(x), round1(y), zValue)));
                id++;
            }
        }
        // end
        list.add(new AddressData(id, id, "ADDR_" + id, new PositionData(round1(x2), round1(y2), zValue)));

        return list;
    }

    private List<LineData> generateLinesFromAddresses(List<AddressData> addresses, long startLineId) {
        List<LineData> list = new ArrayList<>();
        if (addresses.size() < 2) return list;
        long lineId = startLineId;
        for (int i = 0; i < addresses.size() - 1; i++) {
            AddressData a = addresses.get(i);
            AddressData b = addresses.get(i + 1);
            LineData l = new LineData(lineId, "LINE_" + a.getId() + "_" + b.getId(), a.getId(), b.getId(), a.getPos(), b.getPos(), false);
            list.add(l);
            lineId++;
        }
        return list;
    }

    private double pickInterval() {
        // randomly pick one of configured intervals
        int idx = random.nextBoolean() ? 0 : 1;
        return GenerationConfig.RANDOM_INTERVAL[idx];
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
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

    public static class Result {
        public int addressCount;
        public int lineCount;
        public String outputPath;
    }
}