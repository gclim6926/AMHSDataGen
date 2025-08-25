package demo.layout_visualizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class StationsService {

    private final DataFileService dataFileService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public StationsService(DataFileService dataFileService) {
        this.dataFileService = dataFileService;
    }

    public Result runAddStations() throws IOException {
        JsonNode input = dataFileService.readInput();
        Path outputPath = dataFileService.getOutputPath();
        JsonNode output = objectMapper.readTree(outputPath.toFile());

        // 1) IntraBay candidates from local_loop of z6022 and z4822
        List<double[]> boundaries = new ArrayList<>(); // each as [x1,y1,z1,x2,y2,z2]
        extractBoundaries(input.path("z6022").path("local_loop"), 6022.0, boundaries);
        extractBoundaries(input.path("z4822").path("local_loop"), 4822.0, boundaries);

        // 2) Randomly select EQUIPMENTS boundaries
        if (boundaries.size() < GenerationConfig.EQUIPMENTS) {
            throw new IOException("Not enough station boundaries: " + boundaries.size() + " < EQUIPMENTS=" + GenerationConfig.EQUIPMENTS);
        }
        List<double[]> selected = new ArrayList<>(boundaries);
        Collections.shuffle(selected, random);
        selected = selected.subList(0, GenerationConfig.EQUIPMENTS);

        // 3) For each selected boundary create 3 stations by Y interval
        ArrayNode newStations = objectMapper.createArrayNode();
        long stationId = GenerationConfig.STATION_ID_START;
        for (double[] b : selected) {
            double x1 = b[0], y1 = b[1], z = b[2];
            double x2 = b[3], y2 = b[4];
            double centerX = (x1 + x2) / 2.0;
            double yMin = Math.min(y1, y2);
            double yMax = Math.max(y1, y2);
            for (int j = 0; j < 3; j++) {
                double yPos = yMin + (j + 1) * GenerationConfig.STATION_Y_INTERVAL;
                if (yPos > yMax) {
                    yPos = yMax - (2 - j) * GenerationConfig.STATION_Y_INTERVAL;
                }
                ObjectNode s = objectMapper.createObjectNode();
                s.put("id", stationId);
                s.put("name", stationName(stationId));
                s.put("type", "1");
                s.put("port", stationPort(stationId));
                ObjectNode pos = s.putObject("pos");
                pos.put("x", round1(centerX));
                pos.put("y", round1(yPos));
                pos.put("z", round1(z));
                newStations.add(s);
                stationId++;
            }
        }

        // 4) Append to output.json stations array
        ArrayNode stationsNode;
        if (output.has("stations") && output.path("stations").isArray()) {
            stationsNode = (ArrayNode) output.path("stations");
        } else {
            stationsNode = objectMapper.createArrayNode();
        }
        stationsNode.addAll(newStations);
        ((ObjectNode) output).set("stations", stationsNode);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), output);

        Result r = new Result();
        r.count = newStations.size();
        r.outputPath = outputPath.toString();
        return r;
    }

    private void extractBoundaries(JsonNode linesArray, double z, List<double[]> sink) {
        if (linesArray == null || !linesArray.isArray()) return;
        for (JsonNode line : linesArray) {
            if (line.isArray() && line.size() == 2) {
                double x1 = line.get(0).get(0).asDouble();
                double y1 = line.get(0).get(1).asDouble();
                double x2 = line.get(1).get(0).asDouble();
                double y2 = line.get(1).get(1).asDouble();
                // divide by Y interval 100
                double yDiff = Math.abs(y2 - y1);
                int divs = Math.max(1, (int) (yDiff / 100.0));
                for (int i = 0; i < divs; i++) {
                    double rs = i / (double) divs;
                    double re = (i + 1) / (double) divs;
                    double ys = y1 + rs * (y2 - y1);
                    double ye = y1 + re * (y2 - y1);
                    sink.add(new double[] {x1, ys, z, x2, ye, z});
                }
            }
        }
    }

    private static String stationName(long id) {
        String idStr = Long.toString(id);
        String suffix = idStr.length() >= 5 ? idStr.substring(idStr.length() - 5) : String.format("%05d", id);
        return "Station" + suffix;
    }

    private static String stationPort(long id) {
        long last5 = id % 100000L;
        long device = (last5 / 3L) + 1L;
        long port = (last5 % 3L) + 1L;
        return "DEVICE" + device + "_" + port;
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    public static class Result {
        public int count;
        public String outputPath;
    }
}


