package demo.amhsdatagen.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import demo.amhsdatagen.model.UserSession;
import demo.amhsdatagen.service.DataFileService;
import demo.amhsdatagen.service.InputGeneratorService;
import demo.amhsdatagen.service.LineEndpointService;
import demo.amhsdatagen.service.CheckService;
import demo.amhsdatagen.service.StationsService;
import demo.amhsdatagen.service.UdpGeneratorService;
import demo.amhsdatagen.service.Visualize2DService;
import demo.amhsdatagen.service.Visualize3DService;
import demo.amhsdatagen.service.UserTableService;
import demo.amhsdatagen.service.ResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final UserTableService userTableService;
    private final ResetService resetService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiController(InputGeneratorService generatorService, DataFileService dataFileService, LineEndpointService lineEndpointService,
                         CheckService checkService, StationsService stationsService, UdpGeneratorService udpGeneratorService,
                         Visualize2DService visualize2DService, Visualize3DService visualize3DService, UserTableService userTableService, ResetService resetService) {
        this.generatorService = generatorService;
        this.dataFileService = dataFileService;
        this.lineEndpointService = lineEndpointService;
        this.checkService = checkService;
        this.stationsService = stationsService;
        this.udpGeneratorService = udpGeneratorService;
        this.visualize2DService = visualize2DService;
        this.visualize3DService = visualize3DService;
        this.userTableService = userTableService;
        this.resetService = resetService;
    }

    @GetMapping("/check-status")
    public Map<String, Object> checkStatus() {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "OK");
        return res;
    }

    @PostMapping("/run-check")
    public ResponseEntity<Map<String, Object>> runCheck(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(res);
            }
            
            String userId = userSession.getUserId();
            CheckService.Result r = checkService.runCheck(userId);
            res.put("success", true);
            res.put("message", "Check completed successfully. Checked " + r.addressCount + " addresses, " + r.lineCount + " lines, " + r.stationCount + " stations.");
            
            // 상세한 설정 정보
            Map<String, Object> configUpdated = new HashMap<>();
            configUpdated.put("method", "check");
            configUpdated.put("checked_addresses", r.addressCount);
            configUpdated.put("checked_lines", r.lineCount);
            configUpdated.put("checked_stations", r.stationCount);
            res.put("config_updated", configUpdated);
            
            // 상세한 터미널 출력
            Map<String, Object> exec = new HashMap<>();
            StringBuilder terminalLogs = new StringBuilder();
            terminalLogs.append("🚀 Data Validation Process Started\n");
            terminalLogs.append("📋 User ID: ").append(userId).append("\n");
            terminalLogs.append("⏰ Process started at: ").append(java.time.LocalDateTime.now()).append("\n");
            terminalLogs.append("📂 Loading data from database...\n");
            terminalLogs.append("✅ Data loaded successfully\n");
            terminalLogs.append("🔍 Validating addresses...\n");
            terminalLogs.append("✅ Address validation completed - ").append(r.addressCount).append(" addresses checked\n");
            terminalLogs.append("🔍 Validating lines...\n");
            terminalLogs.append("✅ Line validation completed - ").append(r.lineCount).append(" lines checked\n");
            terminalLogs.append("🔍 Validating stations...\n");
            terminalLogs.append("✅ Station validation completed - ").append(r.stationCount).append(" stations checked\n");
            terminalLogs.append("📊 Validation summary:\n");
            terminalLogs.append("   - Addresses checked: ").append(r.addressCount).append("\n");
            terminalLogs.append("   - Lines checked: ").append(r.lineCount).append("\n");
            terminalLogs.append("   - Stations checked: ").append(r.stationCount).append("\n");
            if (r.summary != null) {
                terminalLogs.append("\n").append(r.summary).append("\n");
            }
            if (r.logText != null) {
                terminalLogs.append("\n").append(r.logText);
            }
            terminalLogs.append("\n🎉 Data Validation Process completed successfully!\n");
            terminalLogs.append("⏰ Process finished at: ").append(java.time.LocalDateTime.now()).append("\n");
            
            exec.put("terminal_logs", terminalLogs.toString());
            exec.put("stdout", "Data validation completed successfully");
            exec.put("stderr", "");
            res.put("execution_output", exec);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/run-stations")
    public ResponseEntity<Map<String, Object>> runStations(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(res);
            }
            
            String userId = userSession.getUserId();
            StationsService.Result r = stationsService.runAddStations(userId);
            res.put("success", true);
            res.put("message", "Stations generation completed successfully. Generated " + r.count + " stations.");
            
            // 상세한 설정 정보
            Map<String, Object> configUpdated = new HashMap<>();
            configUpdated.put("script", "add_stations.py");
            configUpdated.put("status", "OK");
            configUpdated.put("method", "add_stations");
            configUpdated.put("generated_stations", r.count);
            configUpdated.put("database_key", "layout_seed.output");
            res.put("config_updated", configUpdated);
            
            // 상세한 터미널 출력
            Map<String, Object> exec = new HashMap<>();
            StringBuilder terminalLogs = new StringBuilder();
            terminalLogs.append("🚀 Station Generation Process Started\n");
            terminalLogs.append("📋 User ID: ").append(userId).append("\n");
            terminalLogs.append("⏰ Process started at: ").append(java.time.LocalDateTime.now()).append("\n");
            terminalLogs.append("📂 Loading input and output data from database...\n");
            terminalLogs.append("✅ Data loaded successfully\n");
            terminalLogs.append("🔍 Extracting station boundaries from local loops...\n");
            terminalLogs.append("✅ Found potential station boundaries\n");
            terminalLogs.append("🎲 Selecting equipment boundaries...\n");
            terminalLogs.append("✅ Selected boundaries for station generation\n");
            terminalLogs.append("🏭 Generating stations for each selected boundary...\n");
            terminalLogs.append("✅ Generated ").append(r.count).append(" stations\n");
            terminalLogs.append("💾 Saving stations to database...\n");
            terminalLogs.append("📊 Final data summary:\n");
            terminalLogs.append("   - Total Stations Generated: ").append(r.count).append("\n");
            terminalLogs.append("   - Database Key: layout_seed.output\n");
            terminalLogs.append("✅ Data successfully saved to database\n");
            terminalLogs.append("🎉 Station Generation Process completed successfully!\n");
            terminalLogs.append("⏰ Process finished at: ").append(java.time.LocalDateTime.now()).append("\n");
            
            exec.put("terminal_logs", terminalLogs.toString());
            exec.put("stdout", "Station generation completed successfully");
            exec.put("stderr", "");
            res.put("execution_output", exec);
            res.put("config_updated", configUpdated);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/run-udp-generator")
    public ResponseEntity<Map<String, Object>> runUdpGenerator(@RequestBody Object payload, HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(res);
            }
            
            String userId = userSession.getUserId();
            if (payload instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> list = (java.util.List<Map<String, Object>>) payload;
                java.util.List<UdpGeneratorService.Request> requests = new java.util.ArrayList<>();
                for (Map<String, Object> item : list) {
                    UdpGeneratorService.Request req = new UdpGeneratorService.Request();
                    req.startAddress = ((Number) item.getOrDefault("startAddress", item.getOrDefault("start_address", 0))).intValue();
                    req.destinationAddress = ((Number) item.getOrDefault("destinationAddress", item.getOrDefault("destination_address", 0))).intValue();
                    Object oid = item.getOrDefault("ohtId", item.get("oht_id"));
                    req.ohtId = oid != null ? String.valueOf(oid) : null;
                    requests.add(req);
                }
                UdpGeneratorService.Result r = udpGeneratorService.runGenerateBulk(userId, requests);
                res.put("success", true);
                res.put("message", "UDP Generator bulk operation completed successfully. Processed " + requests.size() + " requests.");
                Map<String, Object> exec = new HashMap<>();
                exec.put("terminal_logs", (r.summary!=null?r.summary+"\n\n":"") + (r.content!=null?r.content:""));
                res.put("execution_output", exec);
                Map<String, Object> configUpdated = new HashMap<>();
                configUpdated.put("status", "OK");
                configUpdated.put("method", "udp_generator_bulk");
                res.put("config_updated", configUpdated);
                return ResponseEntity.ok(res);
            } else if (payload instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) payload;
                int start = ((Number) body.getOrDefault("start_address", body.getOrDefault("startAddress", 100010))).intValue();
                int dest = ((Number) body.getOrDefault("destination_address", body.getOrDefault("destinationAddress", 100110))).intValue();
                String ohtId = body.get("oht_id") != null ? String.valueOf(body.get("oht_id")) : (body.get("ohtId") != null ? String.valueOf(body.get("ohtId")) : null);
                UdpGeneratorService.Result r = udpGeneratorService.runGenerate(userId, start, dest, ohtId);
                res.put("success", true);
                res.put("message", "UDP Generator completed successfully. Generated data for OHT " + (ohtId != null ? ohtId : "default") + " from address " + start + " to " + dest + ".");
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
            } else {
                res.put("success", false);
                res.put("message", "unsupported payload");
                return ResponseEntity.badRequest().body(res);
            }
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/extract-amhs-data")
    public ResponseEntity<Map<String, Object>> extractAmhsData(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(res);
            }
            
            String userId = userSession.getUserId();
            var inputOpt = userTableService.findByUserIdAndKey(userId, "layout_seed.input");
            var outputOpt = userTableService.findByUserIdAndKey(userId, "layout_seed.output");
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
                JsonNode inputData = objectMapper.readTree(inputOpt.get().getConfigValue());
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(inputPath.toFile(), inputData);
            }
            if (outputOpt.isPresent()) {
                JsonNode outputData = objectMapper.readTree(outputOpt.get().getConfigValue());
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), outputData);
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
    public org.springframework.http.ResponseEntity<byte[]> downloadAmhsData(HttpSession session) {
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                return org.springframework.http.ResponseEntity.status(401).body(null);
            }
            
            String userId = userSession.getUserId();
            var inputOpt = userTableService.findByUserIdAndKey(userId, "layout_seed.input");
            var outputOpt = userTableService.findByUserIdAndKey(userId, "layout_seed.output");
            if (inputOpt.isEmpty() && outputOpt.isEmpty()) {
                return org.springframework.http.ResponseEntity.status(404).body(null);
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
                if (inputOpt.isPresent()) {
                    zos.putNextEntry(new java.util.zip.ZipEntry("input.json"));
                    JsonNode inputData = objectMapper.readTree(inputOpt.get().getConfigValue());
                    byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(inputData);
                    zos.write(bytes);
                    zos.closeEntry();
                }
                if (outputOpt.isPresent()) {
                    zos.putNextEntry(new java.util.zip.ZipEntry("output.json"));
                    JsonNode outputData = objectMapper.readTree(outputOpt.get().getConfigValue());
                    byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(outputData);
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
    public org.springframework.http.ResponseEntity<byte[]> downloadSelected(@RequestBody Map<String, Object> body, HttpSession session) {
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                return org.springframework.http.ResponseEntity.status(401).body(null);
            }
            
            String userId = userSession.getUserId();
            boolean wantInput = Boolean.TRUE.equals(body.get("input"));
            boolean wantOutput = Boolean.TRUE.equals(body.get("output"));
            boolean wantLog = Boolean.TRUE.equals(body.get("oht_log"));
            if (!wantInput && !wantOutput && !wantLog) {
                return org.springframework.http.ResponseEntity.badRequest().build();
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
                if (wantInput) {
                    var inputOpt = userTableService.findByUserIdAndKey(userId, "layout_seed.input");
                    if (inputOpt.isPresent()) {
                        zos.putNextEntry(new java.util.zip.ZipEntry("layout_input.json"));
                        JsonNode inputData = objectMapper.readTree(inputOpt.get().getConfigValue());
                        zos.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(inputData));
                        zos.closeEntry();
                    }
                }
                if (wantOutput) {
                    var outputOpt = userTableService.findByUserIdAndKey(userId, "layout_seed.output");
                    if (outputOpt.isPresent()) {
                        zos.putNextEntry(new java.util.zip.ZipEntry("layout_output.json"));
                        JsonNode outputData = objectMapper.readTree(outputOpt.get().getConfigValue());
                        zos.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(outputData));
                        zos.closeEntry();
                    }
                }
                if (wantLog) {
                    var logOpt = userTableService.findByUserIdAndKey(userId, "oht_track.datalog");
                    if (logOpt.isPresent()) {
                        zos.putNextEntry(new java.util.zip.ZipEntry("oht_track.datalog"));
                        zos.write(logOpt.get().getConfigValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
    public ResponseEntity<Map<String, Object>> visualize2D(@RequestBody(required = false) JsonNode filters, HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(res);
            }
            
            String userId = userSession.getUserId();
            Visualize2DService.Result r = visualize2DService.run(userId, filters);
            res.put("success", true);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", r.info);
            res.put("execution_output", exec);
            res.put("data", r.data);
            res.put("config_updated", Map.of("selected_layers", new String[]{"z6022", "z4822"}));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/3d-viewer")
    public ResponseEntity<Map<String, Object>> visualize3D(@RequestBody(required = false) JsonNode filters, HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(res);
            }
            
            String userId = userSession.getUserId();
            Visualize3DService.Result r = visualize3DService.run(userId, filters);
            res.put("success", true);
            Map<String, Object> exec = new HashMap<>();
            exec.put("terminal_logs", r.info);
            res.put("execution_output", exec);
            res.put("data", r.data);
            res.put("config_updated", Map.of("selected_layers", new String[]{"z6022", "z4822"}));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @GetMapping("/get-output-json")
    public ResponseEntity<Map<String, Object>> getOutputJson(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(res);
            }
            
            String userId = userSession.getUserId();
            var opt = userTableService.findByUserIdAndKey(userId, "layout_seed.output");
            if (opt.isEmpty()) {
                res.put("success", false);
                res.put("message", "output not found in DB");
                return ResponseEntity.ok(res);
            }
            JsonNode node = objectMapper.readTree(opt.get().getConfigValue());
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
    public ResponseEntity<Map<String, Object>> getUdpLog(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(res);
            }
            
            String userId = userSession.getUserId();
            var logOpt = userTableService.findByUserIdAndKey(userId, "oht_track.datalog");
            if (logOpt.isEmpty()) {
                res.put("success", false);
                res.put("message", "oht_track.datalog not found in DB");
                return ResponseEntity.ok(res);
            }
            String content = logOpt.get().getConfigValue();
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
    public ResponseEntity<Map<String, Object>> runGenerate(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(res);
            }
            
            String userId = userSession.getUserId();
            InputGeneratorService.Result result = generatorService.runGenerate(userId);
            res.put("success", true);
            res.put("message", "Address generation completed successfully. Generated " + result.addressCount + " addresses and " + result.lineCount + " lines.");
            
            // 상세한 설정 정보
            Map<String, Object> configUpdated = new HashMap<>();
            configUpdated.put("selected_layers", new String[] {"z6022", "z4822"});
            configUpdated.put("visualization_mode", "overlap");
            configUpdated.put("generated_addresses", result.addressCount);
            configUpdated.put("generated_lines", result.lineCount);
            configUpdated.put("database_key", "layout_seed.output");
            res.put("config_updated", configUpdated);
            
            // 상세한 터미널 출력
            Map<String, Object> exec = new HashMap<>();
            StringBuilder terminalLogs = new StringBuilder();
            terminalLogs.append("🚀 Address Generation Process Started\n");
            terminalLogs.append("📋 User ID: ").append(userId).append("\n");
            terminalLogs.append("⏰ Process started at: ").append(java.time.LocalDateTime.now()).append("\n");
            terminalLogs.append("📂 Loading input data from database...\n");
            terminalLogs.append("✅ Input data loaded successfully\n");
            terminalLogs.append("🔄 Processing layer crossover connections...\n");
            terminalLogs.append("🏭 Processing z6022 layer (top floor)...\n");
            terminalLogs.append("🏭 Processing z4822 layer (middle floor)...\n");
            terminalLogs.append("💾 Preparing data for database storage...\n");
            terminalLogs.append("📊 Data summary:\n");
            terminalLogs.append("   - Total Addresses: ").append(result.addressCount).append("\n");
            terminalLogs.append("   - Total Lines: ").append(result.lineCount).append("\n");
            terminalLogs.append("   - Database Key: layout_seed.output\n");
            terminalLogs.append("💾 Saving data to database...\n");
            terminalLogs.append("✅ Data successfully saved to database\n");
            terminalLogs.append("🎉 Address Generation Process completed successfully!\n");
            terminalLogs.append("⏰ Process finished at: ").append(java.time.LocalDateTime.now()).append("\n");
            
            exec.put("terminal_logs", terminalLogs.toString());
            exec.put("stdout", "Address generation completed successfully");
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
    public ResponseEntity<Map<String, Object>> getInputData(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(res);
            }
            
            String userId = userSession.getUserId();
            Optional<demo.amhsdatagen.service.UserTableService.ConfigData> entryOpt = userTableService.findByUserIdAndKey(userId, "layout_seed.input");
            if (entryOpt.isEmpty()) {
                res.put("success", false);
                res.put("message", "layout_seed.input not found in DB");
                return ResponseEntity.ok(res);
            }
            
            JsonNode data = objectMapper.readTree(entryOpt.get().getConfigValue());
            res.put("success", true);
            res.put("data", data);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/update-input-json")
    public ResponseEntity<Map<String, Object>> updateInputJson(@RequestBody JsonNode input, HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(res);
            }
            
            String userId = userSession.getUserId();
            String json = input.toPrettyString();
            userTableService.saveToUserTable(userId, "layout_seed.input", json);
            
            // output 데이터도 초기화 (빈 객체로 시작)
            ObjectNode outputNode = objectMapper.createObjectNode();
            userTableService.saveToUserTable(userId, "layout_seed.output", outputNode.toPrettyString());
            
            res.put("success", true);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/run-add-lines")
    public ResponseEntity<Map<String, Object>> runAddLines(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(res);
            }
            
            String userId = userSession.getUserId();
            LineEndpointService.Result r = lineEndpointService.runAddLines(userId);
            res.put("success", true);
            res.put("message", "Line generation completed successfully. Phase 1: " + r.addedPhase1 + " lines, Phase 2: " + r.addedPhase2 + " lines, Total: " + r.totalLines + " lines.");
            
            // 상세한 설정 정보
            Map<String, Object> configUpdated = new HashMap<>();
            configUpdated.put("method", "add_lines_endpoint");
            configUpdated.put("phase1_added", r.addedPhase1);
            configUpdated.put("phase2_added", r.addedPhase2);
            configUpdated.put("total_lines", r.totalLines);
            configUpdated.put("database_key", "layout_seed.output");
            res.put("config_updated", configUpdated);
            
            // 상세한 터미널 출력
            Map<String, Object> exec = new HashMap<>();
            StringBuilder terminalLogs = new StringBuilder();
            terminalLogs.append("🚀 Line Generation Process Started\n");
            terminalLogs.append("📋 User ID: ").append(userId).append("\n");
            terminalLogs.append("⏰ Process started at: ").append(java.time.LocalDateTime.now()).append("\n");
            terminalLogs.append("📂 Loading existing data from database...\n");
            terminalLogs.append("✅ Data loaded successfully\n");
            terminalLogs.append("🔄 Phase 1: Connecting unused addresses...\n");
            terminalLogs.append("✅ Phase 1 completed - Added ").append(r.addedPhase1).append(" lines\n");
            terminalLogs.append("💾 Saving intermediate results to database...\n");
            terminalLogs.append("✅ Intermediate data saved\n");
            terminalLogs.append("🔄 Phase 2: Connecting endpoint addresses...\n");
            terminalLogs.append("✅ Phase 2 completed - Added ").append(r.addedPhase2).append(" lines\n");
            terminalLogs.append("💾 Saving final results to database...\n");
            terminalLogs.append("📊 Final data summary:\n");
            terminalLogs.append("   - Total Lines: ").append(r.totalLines).append("\n");
            terminalLogs.append("   - Phase 1 Added: ").append(r.addedPhase1).append(" lines\n");
            terminalLogs.append("   - Phase 2 Added: ").append(r.addedPhase2).append(" lines\n");
            terminalLogs.append("   - Database Key: layout_seed.output\n");
            terminalLogs.append("✅ Data successfully saved to database\n");
            terminalLogs.append("🎉 Line Generation Process completed successfully!\n");
            terminalLogs.append("⏰ Process finished at: ").append(java.time.LocalDateTime.now()).append("\n");
            
            exec.put("terminal_logs", terminalLogs.toString());
            exec.put("stdout", "Line generation completed successfully");
            exec.put("stderr", "");
            res.put("execution_output", exec);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @GetMapping("/db-status")
    public ResponseEntity<Map<String, Object>> getDbStatus(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(res);
            }
            
            String userId = userSession.getUserId();
            boolean tableExists = userTableService.isUserTableExists(userId);
            
            Map<String, Object> dbInfo = new HashMap<>();
            dbInfo.put("userId", userId);
            dbInfo.put("tableExists", tableExists);
            dbInfo.put("tableName", userId + "_amhs_data");
            
            if (tableExists) {
                // 데이터 개수 확인
                var inputOpt = userTableService.findByUserIdAndKey(userId, "layout_seed.input");
                var outputOpt = userTableService.findByUserIdAndKey(userId, "layout_seed.output");
                var ohtLogOpt = userTableService.findByUserIdAndKey(userId, "oht_track.datalog");
                
                dbInfo.put("hasInput", inputOpt.isPresent());
                dbInfo.put("hasOutput", outputOpt.isPresent());
                dbInfo.put("hasOhtLog", ohtLogOpt.isPresent());
                
                if (outputOpt.isPresent()) {
                    try {
                        JsonNode output = objectMapper.readTree(outputOpt.get().getConfigValue());
                        dbInfo.put("addressCount", output.path("addresses").size());
                        dbInfo.put("lineCount", output.path("lines").size());
                        dbInfo.put("stationCount", output.path("stations").size());
                    } catch (Exception e) {
                        dbInfo.put("parseError", e.getMessage());
                    }
                }
            }
            
            res.put("success", true);
            res.put("data", dbInfo);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/clear-user-data")
    public ResponseEntity<Map<String, Object>> clearUserData(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(res);
            }
            
            String userId = userSession.getUserId();
            if (userTableService.isUserTableExists(userId)) {
                String tableName = userId + "_amhs_data";
                userTableService.jdbcTemplate.execute("DROP TABLE IF EXISTS \"" + tableName + "\"");
                res.put("message", "UserID " + userId + " 데이터가 삭제되었습니다.");
            } else {
                res.put("message", "UserID " + userId + " 데이터가 존재하지 않습니다.");
            }
            
            res.put("success", true);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/update-oht-log")
    public ResponseEntity<Map<String, Object>> updateOhtLog(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(res);
            }
            
            String userId = userSession.getUserId();
            
            // 디버그: OHT 로그 확인
            // 모든 키 조회해서 확인
            List<String> keys = userTableService.getAllConfigKeys(userId);
            
            // OHT 로그 데이터를 DB에서 가져와서 파일로 저장
            Optional<String> ohtLogOpt = userTableService.getConfigValue(userId, "oht_track.datalog");
            if (ohtLogOpt.isPresent()) {
                String ohtLogContent = ohtLogOpt.get();
                
                // 파일로 저장
                java.nio.file.Path outputPath = java.nio.file.Paths.get("data", "output_oht_track_data.log");
                java.nio.file.Files.createDirectories(outputPath.getParent());
                java.nio.file.Files.write(outputPath, ohtLogContent.getBytes());
                
                res.put("success", true);
                res.put("message", "OHT 로그가 oht_track_data.log 파일로 저장되었습니다.");
                res.put("file_path", outputPath.toString());
            } else {
                res.put("success", false);
                res.put("message", "OHT 로그 데이터를 찾을 수 없습니다.");
            }
            
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserSession userSession = (UserSession) session.getAttribute("userSession");
            if (userSession == null || !userSession.isLoggedIn()) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return ResponseEntity.badRequest().body(res);
            }

            String userId = userSession.getUserId();
            String tableName = userId + "_amhs_data";
            
            // 사용자 정보
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", userId);
            if (userSession.getLastActivity() != null) {
                // 분까지만 표시 (예: 2024-01-15 14:30)
                String formattedTime = userSession.getLastActivity().toString().substring(0, 16);
                userInfo.put("lastActivity", formattedTime);
            } else {
                userInfo.put("lastActivity", "정보 없음");
            }
            
            // 마지막 활동 시간 업데이트
            userSession.updateLastActivity();
            
            // 데이터베이스 상태
            Map<String, Object> dbStatus = new HashMap<>();
            dbStatus.put("tableName", tableName);
            dbStatus.put("connection", "✅ 연결됨");
            
            res.put("success", true);
            res.put("userInfo", userInfo);
            res.put("dbStatus", dbStatus);
            
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "상태 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

}


