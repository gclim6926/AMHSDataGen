package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class Visualize3DService {

    private final ConfigService configService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Visualize3DService(ConfigService configService) {
        this.configService = configService;
    }

    public Result run(String userId, JsonNode filters) throws IOException {
        // DB에서 output 데이터 로드
        JsonNode output = configService.loadOutputFromDb(userId).orElseThrow(() -> new IOException("output not found in DB"));
        
        // 필터링된 데이터 생성
        JsonNode filteredData = applyFilters(userId, output, filters);
        
        Result r = new Result();
        r.status = "OK";
        r.data = filteredData;
        r.info = String.format("3D visualization data loaded for userId: %s, addresses: %d, lines: %d", 
                              userId, 
                              output.path("addresses").size(),
                              output.path("lines").size());
        return r;
    }
    
    private JsonNode applyFilters(String userId, JsonNode output, JsonNode filters) {
        if (filters == null || filters.isNull()) {
            return output;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        
        // Layer Filter 처리
        JsonNode layers = filters.path("layers");
        boolean showZ6022 = layers.isArray() && layers.size() > 0 && 
                           layers.toString().contains("z6022");
        boolean showZ4822 = layers.isArray() && layers.size() > 0 && 
                           layers.toString().contains("z4822");
        boolean showOverlap = layers.isArray() && layers.size() > 0 && 
                             layers.toString().contains("Overlap");
        
        // Overlap이 체크되면 모든 레이어를 하나의 창에 표시
        if (showOverlap) {
            showZ6022 = true;
            showZ4822 = true;
        }
        
        // Component Filter 처리
        JsonNode components = filters.path("components");
        boolean showAddresses = components.isArray() && components.size() > 0 && 
                               components.toString().contains("addresses");
        boolean showLines = components.isArray() && components.size() > 0 && 
                           components.toString().contains("lines");
        boolean showStations = components.isArray() && components.size() > 0 && 
                              components.toString().contains("stations");
        boolean showOhts = components.isArray() && components.size() > 0 && 
                          components.toString().contains("ohts");
        
        // 필터링된 addresses 추가
        if (showAddresses) {
            ArrayNode filteredAddresses = mapper.createArrayNode();
            JsonNode addresses = output.path("addresses");
            if (addresses.isArray()) {
                for (JsonNode addr : addresses) {
                    double z = addr.path("pos").path("z").asDouble();
                    boolean include = false;
                    String layer = "";
                    
                    if (showZ6022 && Math.abs(z - 6022.0) < 0.1) {
                        include = true;
                        layer = "z6022";
                    }
                    if (showZ4822 && Math.abs(z - 4822.0) < 0.1) {
                        include = true;
                        layer = "z4822";
                    }
                    if (showOverlap && Math.abs(z - 3000.0) < 0.1) {
                        include = true;
                        layer = "z3000";
                    }
                    
                    if (include) {
                        ObjectNode addrWithLayer = addr.deepCopy();
                        addrWithLayer.put("layer", layer);
                        filteredAddresses.add(addrWithLayer);
                    }
                }
            }
            result.set("addresses", filteredAddresses);
        }
        
        // 필터링된 lines 추가
        if (showLines) {
            ArrayNode filteredLines = mapper.createArrayNode();
            JsonNode lines = output.path("lines");
            if (lines.isArray()) {
                for (JsonNode line : lines) {
                    // lines는 start_address와 end_address를 가지고 있음
                    int startAddr = line.path("start_address").asInt();
                    int endAddr = line.path("end_address").asInt();
                    
                    // addresses가 선택된 경우에만 연결성 확인
                    if (showAddresses) {
                        // 필터링된 addresses에서 해당 address들이 포함되는지 확인
                        boolean startIncluded = false;
                        boolean endIncluded = false;
                        
                        JsonNode filteredAddresses = result.path("addresses");
                        if (filteredAddresses.isArray()) {
                            for (JsonNode addr : filteredAddresses) {
                                int addrId = addr.path("id").asInt();
                                if (addrId == startAddr) startIncluded = true;
                                if (addrId == endAddr) endIncluded = true;
                            }
                        }
                        
                        // start_address와 end_address가 모두 필터링된 addresses에 포함되면 lines도 포함
                        if (startIncluded && endIncluded) {
                            filteredLines.add(line);
                        }
                    } else {
                        // addresses가 선택되지 않은 경우 모든 lines 포함
                        filteredLines.add(line);
                    }
                }
            }
            result.set("lines", filteredLines);
        }
        
        // 필터링된 stations 추가
        if (showStations) {
            ArrayNode filteredStations = mapper.createArrayNode();
            JsonNode stations = output.path("stations");
            if (stations.isArray()) {
                for (JsonNode station : stations) {
                    double z = station.path("pos").path("z").asDouble();
                    boolean include = false;
                    String layer = "";
                    
                    if (showZ6022 && Math.abs(z - 6022.0) < 0.1) {
                        include = true;
                        layer = "z6022";
                    }
                    if (showZ4822 && Math.abs(z - 4822.0) < 0.1) {
                        include = true;
                        layer = "z4822";
                    }
                    if (showOverlap && Math.abs(z - 3000.0) < 0.1) {
                        include = true;
                        layer = "z3000";
                    }
                    
                    if (include) {
                        ObjectNode stationWithLayer = station.deepCopy();
                        stationWithLayer.put("layer", layer);
                        filteredStations.add(stationWithLayer);
                    }
                }
            }
            result.set("stations", filteredStations);
        }
        
        // OHT tracks 추가 (oht_track.datalog에서 로드)
        if (showOhts) {
            try {
                String ohtLog = configService.loadOhtLogFromDb(userId).orElse("");
                result.put("oht_tracks", ohtLog);
            } catch (Exception e) {
                // OHT 로그가 없으면 빈 문자열
                result.put("oht_tracks", "");
            }
        }
        
        // 필터 정보 추가
        result.put("overlap_mode", showOverlap);
        result.put("selected_layers", layers);
        result.put("selected_components", components);
        
        return result;
    }

    public static class Result {
        public String status;
        public String info;
        public JsonNode data;
    }
}


