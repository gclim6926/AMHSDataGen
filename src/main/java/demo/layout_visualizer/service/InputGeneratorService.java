package demo.layout_visualizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import demo.layout_visualizer.model.Address;
import demo.layout_visualizer.model.Line;
import demo.layout_visualizer.model.Position;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Service
public class InputGeneratorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private final DataFileService dataFileService;

    public InputGeneratorService(DataFileService dataFileService) {
        this.dataFileService = dataFileService;
    }

    public Result runGenerate() throws IOException {
        dataFileService.ensureInputExistsFromSample();
        Path inputPath = dataFileService.getInputPath();
        Path outputPath = dataFileService.getOutputPath();
        JsonNode root = dataFileService.readInput();

        long addressId = GenerationConfig.ADDRESS_ID_START;
        long lineId = GenerationConfig.LINE_ID_START;
        List<Address> addresses = new ArrayList<>();
        List<Line> lines = new ArrayList<>();

        // Layer crossover connections (z0-4822, z4822-6022) first
        addressId = processLayerCrossover(root.path("layer_crossover"), addresses, lines, addressId, lineId);
        lineId = GenerationConfig.LINE_ID_START + lines.size();

        // Minimal parity: process z6022/z4822 central/local loops with 2D points
        addressId = processLayer(root.path("z6022"), 6022.0, addresses, lines, addressId, lineId);
        lineId += lines.size();
        addressId = processLayer(root.path("z4822"), 4822.0, addresses, lines, addressId, lineId);
        lineId = GenerationConfig.LINE_ID_START + lines.size();

        // Save output
        ObjectNode output = objectMapper.createObjectNode();
        ArrayNode addressesNode = output.putArray("addresses");
        for (Address a : addresses) {
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
        for (Line l : lines) {
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

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), output);

        Result res = new Result();
        res.addressCount = addresses.size();
        res.lineCount = lines.size();
        res.outputPath = outputPath.toString();
        return res;
    }

    private long processLayerCrossover(JsonNode lcNode,
                                       List<Address> addresses,
                                       List<Line> lines,
                                       long currentAddressId,
                                       long currentLineIdStart) {
        if (lcNode == null || lcNode.isMissingNode()) return currentAddressId;
        currentAddressId = processConnectionArray(lcNode.path("z0-4822"), addresses, lines, currentAddressId);
        currentAddressId = processConnectionArray(lcNode.path("z4822-6022"), addresses, lines, currentAddressId);
        return currentAddressId;
    }

    private long processConnectionArray(JsonNode arr,
                                        List<Address> addresses,
                                        List<Line> lines,
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
                    Address aStart = new Address(startId, startId, "ADDR_" + startId, new Position(round1(sx), round1(sy), sz));
                    addresses.add(aStart);
                    currentAddressId++;
                    long endId = currentAddressId;
                    Address aEnd = new Address(endId, endId, "ADDR_" + endId, new Position(round1(tx), round1(ty), tz));
                    addresses.add(aEnd);
                    currentAddressId++;

                    long newLineId = GenerationConfig.LINE_ID_START + lines.size();
                    Line l = new Line(newLineId, "LINE_" + newLineId, startId, endId,
                            aStart.getPos(), aEnd.getPos(), false);
                    lines.add(l);
                }
            }
        }
        return currentAddressId;
    }

    private long processLayer(JsonNode layerNode, double zValue,
                              List<Address> addresses, List<Line> lines,
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
                                   List<Address> addresses, List<Line> lines,
                                   long currentAddressId, long currentLineId) {
        if (!linesArrayNode.isArray()) return currentAddressId;
        Iterator<JsonNode> it = linesArrayNode.elements();
        while (it.hasNext()) {
            JsonNode lineNode = it.next();
            if (lineNode.isArray() && lineNode.size() == 2) {
                double[] start = readPoint2D(lineNode.get(0));
                double[] end = readPoint2D(lineNode.get(1));
                List<Address> generated = generateAddressesOnLine(start, end, zValue, currentAddressId);
                addresses.addAll(generated);
                if (generated.size() > 1) {
                    List<Line> genLines = generateLinesFromAddresses(generated, currentLineId);
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

    private List<Address> generateAddressesOnLine(double[] start, double[] end, double zValue, long startId) {
        List<Address> list = new ArrayList<>();
        double x1 = start[0], y1 = start[1];
        double x2 = end[0], y2 = end[1];
        double dx = x2 - x1, dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);

        long id = startId;
        // start
        list.add(new Address(id, id, "ADDR_" + id, new Position(round1(x1), round1(y1), zValue)));
        id++;

        double current = 0.0;
        while (current < length) {
            double step = pickInterval();
            current += step;
            if (current < length) {
                double ratio = current / length;
                double x = x1 + ratio * dx;
                double y = y1 + ratio * dy;
                list.add(new Address(id, id, "ADDR_" + id, new Position(round1(x), round1(y), zValue)));
                id++;
            }
        }
        // end
        list.add(new Address(id, id, "ADDR_" + id, new Position(round1(x2), round1(y2), zValue)));

        return list;
    }

    private List<Line> generateLinesFromAddresses(List<Address> addresses, long startLineId) {
        List<Line> list = new ArrayList<>();
        if (addresses.size() < 2) return list;
        long lineId = startLineId;
        for (int i = 0; i < addresses.size() - 1; i++) {
            Address a = addresses.get(i);
            Address b = addresses.get(i + 1);
            Line l = new Line(lineId, "LINE_" + a.getId() + "_" + b.getId(), a.getId(), b.getId(), a.getPos(), b.getPos(), false);
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

    public static class Result {
        public int addressCount;
        public int lineCount;
        public String outputPath;
    }
}


