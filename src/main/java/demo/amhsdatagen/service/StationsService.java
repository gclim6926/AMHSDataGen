package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class StationsService {

    private final ConfigService configService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public StationsService(ConfigService configService) {
        this.configService = configService;
    }

    public Result runAddStations(String userId) throws IOException {
        System.out.println("🚀 Starting Station Generation Process...");
        System.out.println("📋 User ID: " + userId);
        System.out.println("⏰ Process started at: " + java.time.LocalDateTime.now());
        
        System.out.println("📂 Loading input and output data from database...");
        JsonNode input = configService.loadInputFromDb(userId).orElseThrow(() -> new IOException("input not found in DB"));
        JsonNode output = configService.loadOutputFromDb(userId).orElseThrow(() -> new IOException("output not found in DB"));
        System.out.println("✅ Data loaded successfully");

        // 1) IntraBay candidates from local_loop of z6022 and z4822
        System.out.println("🔍 Extracting station boundaries from local loops...");
        List<double[]> boundaries = new ArrayList<>(); // each as [x1,y1,z1,x2,y2,z2]
        extractBoundaries(input.path("z6022").path("local_loop"), 6022.0, boundaries);
        extractBoundaries(input.path("z4822").path("local_loop"), 4822.0, boundaries);
        System.out.println("✅ Found " + boundaries.size() + " potential station boundaries");

        // 2) Randomly select EQUIPMENTS boundaries
        System.out.println("🎲 Selecting " + GenerationConfig.EQUIPMENTS + " equipment boundaries...");
        if (boundaries.size() < GenerationConfig.EQUIPMENTS) {
            throw new IOException("Not enough station boundaries: " + boundaries.size() + " < EQUIPMENTS=" + GenerationConfig.EQUIPMENTS);
        }
        List<double[]> selected = new ArrayList<>(boundaries);
        Collections.shuffle(selected, random);
        selected = selected.subList(0, GenerationConfig.EQUIPMENTS);
        System.out.println("✅ Selected " + selected.size() + " boundaries for station generation");

        // 3) For each selected boundary create 3 stations by Y interval
        System.out.println("🏭 Generating stations for each selected boundary...");
        ArrayNode newStations = objectMapper.createArrayNode();
        long stationId = GenerationConfig.STATION_ID_START;
        int stationCount = 0;
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
                stationCount++;
            }
        }
        System.out.println("✅ Generated " + stationCount + " stations");

        System.out.println("💾 Saving stations to database...");
        // 4) output JSON에 stations 병합 후 DB 저장
        ObjectNode outputObj = (output instanceof ObjectNode) ? (ObjectNode) output : objectMapper.createObjectNode();
        outputObj.set("stations", newStations);
        configService.saveOutputToDb(userId, outputObj);
        
        System.out.println("📊 Final data summary:");
        System.out.println("   - Total Stations Generated: " + stationCount);
        System.out.println("   - Equipment Boundaries Used: " + selected.size());
        System.out.println("   - Database Key: layout_seed.output");
        System.out.println("✅ Data successfully saved to database");
        System.out.println("🎉 Station Generation Process completed successfully!");
        System.out.println("⏰ Process finished at: " + java.time.LocalDateTime.now());

        Result r = new Result();
        r.count = newStations.size();
        r.outputPath = "db://" + userId + "_amhs_data:layout_seed.output";
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


