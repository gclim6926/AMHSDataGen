package demo.layoutviz.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.layoutviz.service.DataFileService;
import demo.layoutviz.service.InputGeneratorService;
import demo.layoutviz.service.LineEndpointService;
import demo.layoutviz.service.CheckService;
import demo.layoutviz.service.StationsService;
import demo.layoutviz.service.UdpGeneratorService;
import demo.layoutviz.service.Visualize2DService;
import demo.layoutviz.service.Visualize3DService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final InputGeneratorService generatorService;
    private final DataFileService dataFileService;
    private final LineEndpointService lineEndpointService;
    private final CheckService checkService;
    private final StationsService stationsService;
    private final UdpGeneratorService udpGeneratorService;
    private final Visualize2DService visualize2DService;
    private final Visualize3DService visualize3DService;

    public ApiController(InputGeneratorService generatorService, DataFileService dataFileService, LineEndpointService lineEndpointService,
                         CheckService checkService, StationsService stationsService, UdpGeneratorService udpGeneratorService,
                         Visualize2DService visualize2DService, Visualize3DService visualize3DService) {
        this.generatorService = generatorService;
        this.dataFileService = dataFileService;
        this.lineEndpointService = lineEndpointService;
        this.checkService = checkService;
        this.stationsService = stationsService;
        this.udpGeneratorService = udpGeneratorService;
        this.visualize2DService = visualize2DService;
        this.visualize3DService = visualize3DService;
    }

    @GetMapping("/check-status")
    public Map<String, Object> checkStatus() {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "OK");
        return res;
    }
    @PostMapping("/run-check")
    public ResponseEntity<Map<String, Object>> runCheck() {
        Map<String, Object> res = new HashMap<>();
        try {
            CheckService.Result r = checkService.runCheck();
            res.put("success", true);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", r.summary + "; log=" + r.logPath);
            res.put("execution_output", exec);
            Map<String, Object> configUpdated = new HashMap<>();
            configUpdated.put("selected_layers", new String[] {"z6022", "z4822"});
            configUpdated.put("method", "check");
            res.put("config_updated", configUpdated);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/run-stations")
    public ResponseEntity<Map<String, Object>> runStations() {
        Map<String, Object> res = new HashMap<>();
        try {
            StationsService.Result r = stationsService.runAddStations();
            res.put("success", true);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", "stations=" + r.count + "; output=" + r.outputPath);
            res.put("execution_output", exec);
            Map<String, Object> configUpdated = new HashMap<>();
            configUpdated.put("script", "add_stations.py");
            configUpdated.put("status", "OK");
            configUpdated.put("method", "add_stations");
            res.put("config_updated", configUpdated);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/run-udp-generator")
    public ResponseEntity<Map<String, Object>> runUdpGenerator(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            int start = ((Number) body.getOrDefault("start_address", 0)).intValue();
            int dest = ((Number) body.getOrDefault("destination_address", 0)).intValue();
            String ohtId = body.get("oht_id") != null ? String.valueOf(body.get("oht_id")) : null;
            UdpGeneratorService.Result r = udpGeneratorService.runGenerate(start, dest, ohtId);
            res.put("success", true);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", r.summary);
            res.put("execution_output", exec);
            Map<String, Object> configUpdated = new HashMap<>();
            configUpdated.put("script", "generate_udp_data.py");
            configUpdated.put("status", "OK");
            configUpdated.put("method", "udp_generator");
            configUpdated.put("start_address", start);
            configUpdated.put("destination_address", dest);
            res.put("config_updated", configUpdated);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/run-udp-generator-bulk")
    public ResponseEntity<Map<String, Object>> runUdpGeneratorBulk(@RequestBody java.util.List<UdpGeneratorService.Request> requests) {
        Map<String, Object> res = new HashMap<>();
        try {
            UdpGeneratorService.Result r = udpGeneratorService.runGenerateBulk(requests);
            res.put("success", true);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", r.summary);
            res.put("execution_output", exec);
            Map<String, Object> configUpdated = new HashMap<>();
            configUpdated.put("status", "OK");
            configUpdated.put("method", "udp_generator_bulk");
            res.put("config_updated", configUpdated);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/2d-viewer")
    public ResponseEntity<Map<String, Object>> visualize2D(@RequestBody(required = false) JsonNode filters) {
        Map<String, Object> res = new HashMap<>();
        try {
            Visualize2DService.Result r = visualize2DService.run(filters);
            res.put("success", true);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", r.info);
            res.put("execution_output", exec);
            res.put("config_updated", Map.of("selected_layers", new String[]{}));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/3d-viewer")
    public ResponseEntity<Map<String, Object>> visualize3D(@RequestBody(required = false) JsonNode filters) {
        Map<String, Object> res = new HashMap<>();
        try {
            Visualize3DService.Result r = visualize3DService.run(filters);
            res.put("success", true);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", r.info);
            res.put("execution_output", exec);
            res.put("config_updated", Map.of("selected_layers", new String[]{}));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @GetMapping("/get-output-json")
    public ResponseEntity<Map<String, Object>> getOutputJson() {
        Map<String, Object> res = new HashMap<>();
        try {
            var node = new ObjectMapper().readTree(dataFileService.getOutputPath().toFile());
            res.put("success", true);
            res.put("data", node);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @GetMapping("/get-udp-log")
    public ResponseEntity<Map<String, Object>> getUdpLog() {
        Map<String, Object> res = new HashMap<>();
        try {
            var path = dataFileService.getDataDir().resolve("output_oht_track_data.log");
            if (!java.nio.file.Files.exists(path)) {
                res.put("success", false);
                res.put("message", "output_oht_track_data.log not found");
                return ResponseEntity.ok(res);
            }
            var lines = java.nio.file.Files.readAllLines(path);
            res.put("success", true);
            res.put("lines", lines);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/run-generate")
    public ResponseEntity<Map<String, Object>> runGenerate() {
        Map<String, Object> res = new HashMap<>();
        try {
            InputGeneratorService.Result result = generatorService.runGenerate();
            res.put("success", true);
            Map<String, Object> configUpdated = new HashMap<>();
            configUpdated.put("selected_layers", new String[] {"z6022", "z4822"});
            configUpdated.put("visualization_mode", "overlap");
            res.put("config_updated", configUpdated);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", String.format("addresses=%d, lines=%d -> %s", result.addressCount, result.lineCount, result.outputPath));
            exec.put("stdout", "");
            exec.put("stderr", "");
            res.put("execution_output", exec);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @GetMapping("/get-input-data")
    public ResponseEntity<Map<String, Object>> getInputData() {
        Map<String, Object> res = new HashMap<>();
        try {
            JsonNode node = dataFileService.readInput();
            res.put("success", true);
            res.put("data", node);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/update-input-json")
    public ResponseEntity<Map<String, Object>> updateInputJson(@RequestBody JsonNode input) {
        Map<String, Object> res = new HashMap<>();
        try {
            dataFileService.writeInput(input);
            res.put("success", true);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/run-add-lines")
    public ResponseEntity<Map<String, Object>> runAddLines() {
        Map<String, Object> res = new HashMap<>();
        try {
            LineEndpointService.Result r = lineEndpointService.runAddLines();
            res.put("success", true);
            Map<String, Object> configUpdated = new HashMap<>();
            configUpdated.put("method", "add_lines_endpoint");
            res.put("config_updated", configUpdated);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", String.format("phase1=%d, phase2=%d, totalLines=%d -> %s", r.addedPhase1, r.addedPhase2, r.totalLines, r.outputPath));
            res.put("execution_output", exec);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }
}


