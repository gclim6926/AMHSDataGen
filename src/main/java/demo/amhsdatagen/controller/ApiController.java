package demo.amhsdatagen.controller;

import com.fasterxml.jackson.databind.JsonNode;
import demo.amhsdatagen.config.StartupInitializer;
import demo.amhsdatagen.service.DataFileService;
import demo.amhsdatagen.service.ConfigService;
import demo.amhsdatagen.service.InputGeneratorService;
import demo.amhsdatagen.service.LineEndpointService;
import demo.amhsdatagen.service.CheckService;
import demo.amhsdatagen.service.StationsService;
import demo.amhsdatagen.service.UdpGeneratorService;
import demo.amhsdatagen.service.Visualize2DService;
import demo.amhsdatagen.service.ResetService;
import demo.amhsdatagen.service.Visualize3DService;
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
    private final ConfigService configService;
    private final ResetService resetService;
    private final StartupInitializer startupInitializer;

    public ApiController(InputGeneratorService generatorService, DataFileService dataFileService, LineEndpointService lineEndpointService,
                         CheckService checkService, StationsService stationsService, UdpGeneratorService udpGeneratorService,
                         Visualize2DService visualize2DService, Visualize3DService visualize3DService, ConfigService configService, ResetService resetService, StartupInitializer startupInitializer) {
        this.generatorService = generatorService;
        this.dataFileService = dataFileService;
        this.lineEndpointService = lineEndpointService;
        this.checkService = checkService;
        this.stationsService = stationsService;
        this.udpGeneratorService = udpGeneratorService;
        this.visualize2DService = visualize2DService;
        this.visualize3DService = visualize3DService;
        this.configService = configService;
        this.resetService = resetService;
        this.startupInitializer = startupInitializer;
    }

    @GetMapping("/check-status")
    public Map<String, Object> checkStatus() {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "OK");
        return res;
    }

    @PostMapping("/reset-db")
    public ResponseEntity<Map<String, Object>> resetDb() {
        Map<String, Object> res = new HashMap<>();
        try {
            resetService.resetAll();
            // 리셋 후 기본 데이터가 없으면 sample로 재초기화
            startupInitializer.initializeIfEmpty();
            res.put("success", true);
            res.put("message", "DB reset completed");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }
    @PostMapping("/run-check")
    public ResponseEntity<Map<String, Object>> runCheck() {
        Map<String, Object> res = new HashMap<>();
        try {
            CheckService.Result r = checkService.runCheck();
            res.put("success", true);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", (r.summary != null ? r.summary + "\n\n" : "") + (r.logText != null ? r.logText : ""));
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
            exec.put("terminal_logs", (r.summary!=null?r.summary+"\n\n":"") + (r.content!=null?r.content:""));
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
            exec.put("terminal_logs", (r.summary!=null?r.summary+"\n\n":"") + (r.content!=null?r.content:""));
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

    @PostMapping("/extract-amhs-data")
    public ResponseEntity<Map<String, Object>> extractAmhsData() {
        Map<String, Object> res = new HashMap<>();
        try {
            var inputOpt = configService.loadInputFromDb();
            var outputOpt = configService.loadOutputFromDb();
            if (inputOpt.isEmpty() && outputOpt.isEmpty()) {
                res.put("success", false);
                res.put("message", "no AMHS_data in DB");
                return ResponseEntity.ok(res);
            }
            var dataDir = dataFileService.getDataDir();
            java.nio.file.Files.createDirectories(dataDir);
            java.nio.file.Path inputPath = dataDir.resolve("input.json");
            java.nio.file.Path outputPath = dataDir.resolve("output.json");
            if (inputOpt.isPresent()) {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                om.writerWithDefaultPrettyPrinter().writeValue(inputPath.toFile(), inputOpt.get());
            }
            if (outputOpt.isPresent()) {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                om.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), outputOpt.get());
            }
            res.put("success", true);
            res.put("input_path", inputPath.toString());
            res.put("output_path", outputPath.toString());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @GetMapping("/download-amhs-data")
    public org.springframework.http.ResponseEntity<byte[]> downloadAmhsData() {
        try {
            var inputOpt = configService.loadInputFromDb();
            var outputOpt = configService.loadOutputFromDb();
            if (inputOpt.isEmpty() && outputOpt.isEmpty()) {
                return org.springframework.http.ResponseEntity.status(404).body(null);
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                if (inputOpt.isPresent()) {
                    zos.putNextEntry(new java.util.zip.ZipEntry("input.json"));
                    byte[] bytes = om.writerWithDefaultPrettyPrinter().writeValueAsBytes(inputOpt.get());
                    zos.write(bytes);
                    zos.closeEntry();
                }
                if (outputOpt.isPresent()) {
                    zos.putNextEntry(new java.util.zip.ZipEntry("output.json"));
                    byte[] bytes = om.writerWithDefaultPrettyPrinter().writeValueAsBytes(outputOpt.get());
                    zos.write(bytes);
                    zos.closeEntry();
                }
            }

            byte[] zipBytes = baos.toByteArray();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=amhs_data.zip");
            headers.set(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/zip");
            headers.setContentLength(zipBytes.length);
            return org.springframework.http.ResponseEntity.ok().headers(headers).body(zipBytes);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/download-selected")
    public org.springframework.http.ResponseEntity<byte[]> downloadSelected(@RequestBody Map<String, Object> body) {
        try {
            boolean wantInput = Boolean.TRUE.equals(body.get("input"));
            boolean wantOutput = Boolean.TRUE.equals(body.get("output"));
            boolean wantLog = Boolean.TRUE.equals(body.get("oht_log"));
            if (!wantInput && !wantOutput && !wantLog) {
                return org.springframework.http.ResponseEntity.badRequest().build();
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                if (wantInput) {
                    var inputOpt = configService.loadInputFromDb();
                    if (inputOpt.isPresent()) {
                        zos.putNextEntry(new java.util.zip.ZipEntry("layout_input.json"));
                        zos.write(om.writerWithDefaultPrettyPrinter().writeValueAsBytes(inputOpt.get()));
                        zos.closeEntry();
                    }
                }
                if (wantOutput) {
                    var outputOpt = configService.loadOutputFromDb();
                    if (outputOpt.isPresent()) {
                        zos.putNextEntry(new java.util.zip.ZipEntry("layout_output.json"));
                        zos.write(om.writerWithDefaultPrettyPrinter().writeValueAsBytes(outputOpt.get()));
                        zos.closeEntry();
                    }
                }
                if (wantLog) {
                    var logOpt = configService.loadOhtLogFromDb();
                    if (logOpt.isPresent()) {
                        zos.putNextEntry(new java.util.zip.ZipEntry("oht_track.datalog"));
                        zos.write(logOpt.get().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        zos.closeEntry();
                    }
                }
            }

            byte[] zipBytes = baos.toByteArray();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=selected_data.zip");
            headers.set(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/zip");
            headers.setContentLength(zipBytes.length);
            return org.springframework.http.ResponseEntity.ok().headers(headers).body(zipBytes);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
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
            var opt = configService.loadOutputFromDb();
            if (opt.isEmpty()) {
                res.put("success", false);
                res.put("message", "output not found in DB");
                return ResponseEntity.ok(res);
            }
            var node = opt.get();
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
            var logOpt = configService.loadOhtLogFromDb();
            if (logOpt.isEmpty()) {
                res.put("success", false);
                res.put("message", "oht_track.datalog not found in DB");
                return ResponseEntity.ok(res);
            }
            var content = logOpt.get();
            var lines = java.util.Arrays.asList(content.split("\n"));
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
            exec.put("terminal_logs", String.format("addresses=%d, lines=%d -> saved to DB (%s)", result.addressCount, result.lineCount, result.outputPath));
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
            var opt = configService.loadInputFromDb();
            if (opt.isEmpty()) {
                res.put("success", false);
                res.put("message", "layout_seed.input not found in DB");
                return ResponseEntity.ok(res);
            }
            res.put("success", true);
            res.put("data", opt.get());
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
            // 파일 대신 DB에 저장
            configService.saveInputToDb(input);
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


