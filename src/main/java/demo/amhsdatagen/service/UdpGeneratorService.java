package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class UdpGeneratorService {

    private final ConfigService configService;

    public UdpGeneratorService(ConfigService configService) {
        this.configService = configService;
    }

    public Result runGenerate(String userId, int startAddress, int destinationAddress, String ohtId) throws IOException {
        // Build address graph from layout.output (DB)
        Map<Integer, List<Integer>> graph = buildGraphFromOutput(userId);

        List<Integer> path = bfsShortestPath(graph, startAddress, destinationAddress);
        if (path == null || path.size() < 2) throw new IOException("No path found between addresses");

        // Generate only new OHT track log lines (append happens in ConfigService)
        StringBuilder buf = new StringBuilder();
        long now = System.currentTimeMillis();
        Random rnd = new Random();
        for (int i = 0; i < path.size() - 1; i++) {
            int curr = path.get(i);
            int next = path.get(i + 1);
            String line = formatUdpLog(now, curr, next, destinationAddress, ohtId);
            buf.append(line).append('\n');
            int inc = GenerationConfig.UDP_TIME_INCREMENT_MIN_MS + rnd.nextInt(Math.max(1, GenerationConfig.UDP_TIME_INCREMENT_MAX_MS - GenerationConfig.UDP_TIME_INCREMENT_MIN_MS + 1));
            now += inc;
        }
        String content = buf.toString();
        configService.saveOhtLogToDb(userId, content);

        Result r = new Result();
        r.logPath = "db://" + userId + "_amhs_data:oht_track.datalog";
        r.summary = "OHT track entries=" + (path.size() - 1) + ", saved to DB" + (ohtId!=null? (", id="+ohtId): "");
        r.content = content;
        return r;
    }

    public Result runGenerateBulk(String userId, java.util.List<Request> entries) throws IOException {
        if (entries == null || entries.isEmpty()) throw new IOException("no entries");
        // Build address graph from layout.output (DB) once
        Map<Integer, List<Integer>> graph = buildGraphFromOutput(userId);

        // Compute edge lists per OHT
        List<List<int[]>> edgeLists = new ArrayList<>();
        List<String> ohtIds = new ArrayList<>();
        for (Request r : entries) {
            List<Integer> path = bfsShortestPath(graph, r.startAddress, r.destinationAddress);
            if (path == null || path.size() < 2) { edgeLists.add(java.util.Collections.emptyList()); ohtIds.add(r.ohtId); continue; }
            List<int[]> edges = new ArrayList<>();
            for (int i = 0; i < path.size() - 1; i++) edges.add(new int[]{path.get(i), path.get(i+1), r.destinationAddress});
            edgeLists.add(edges); ohtIds.add(r.ohtId);
        }

        // Interleaved write (simultaneous tick per OHT) -> DB buffer
        StringBuilder out = new StringBuilder();
        long baseNow = System.currentTimeMillis();
        Random rnd = new Random();
        int n = edgeLists.size();
        long[] nowPerOht = new long[n];
        int[] idxPerOht = new int[n];
        for (int i = 0; i < n; i++) nowPerOht[i] = baseNow;
        int remaining = edgeLists.stream().mapToInt(List::size).sum();
        while (remaining > 0) {
            // one tick: all OHTs advance one step (if available)
            int incTick = GenerationConfig.UDP_TIME_INCREMENT_MIN_MS + rnd.nextInt(Math.max(1, GenerationConfig.UDP_TIME_INCREMENT_MAX_MS - GenerationConfig.UDP_TIME_INCREMENT_MIN_MS + 1));
            for (int k = 0; k < n; k++) {
                List<int[]> edges = edgeLists.get(k);
                int idx = idxPerOht[k];
                if (idx < edges.size()) {
                    int[] e = edges.get(idx);
                    String line = formatUdpLog(nowPerOht[k], e[0], e[1], e[2], ohtIds.get(k));
                    out.append(line).append('\n');
                }
            }
            // after writing this tick, advance time and indices for those progressed
            for (int k = 0; k < n; k++) {
                List<int[]> edges = edgeLists.get(k);
                int idx = idxPerOht[k];
                if (idx < edges.size()) {
                    nowPerOht[k] += incTick;
                    idxPerOht[k] = idx + 1;
                    remaining--;
                }
            }
        }
        String content = out.toString();
        configService.saveOhtLogToDb(userId, content);

        Result r = new Result();
        r.logPath = "db://" + userId + "_amhs_data:oht_track.datalog";
        r.summary = "OHT track entries written (bulk) -> saved to DB";
        r.content = content;
        return r;
    }

    public static class Request {
        public int startAddress;
        public int destinationAddress;
        public String ohtId;
    }

    private Map<Integer, List<Integer>> buildGraphFromOutput(String userId) throws IOException {
        JsonNode output = configService.loadOutputFromDb(userId).orElseThrow(() -> new IOException("output not found in DB"));
        JsonNode linesNode = output.path("lines");
        Map<Integer, List<Integer>> graph = new HashMap<>();
        if (linesNode != null && linesNode.isArray()) {
            for (JsonNode l : linesNode) {
                int from = l.path("fromAddress").asInt();
                int to = l.path("toAddress").asInt();
                graph.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
                graph.computeIfAbsent(to, k -> new ArrayList<>()).add(from);
            }
        }
        return graph;
    }

    private static List<Integer> bfsShortestPath(Map<Integer, List<Integer>> graph, int start, int dest) {
        if (start == dest) return Collections.singletonList(start);
        if (!graph.containsKey(start) || !graph.containsKey(dest)) return null;
        Queue<Integer> q = new ArrayDeque<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        q.add(start); visited.add(start); parent.put(start, -1);
        while (!q.isEmpty()) {
            int cur = q.poll();
            if (cur == dest) break;
            for (int nx : graph.getOrDefault(cur, Collections.emptyList())) {
                if (!visited.contains(nx)) { visited.add(nx); parent.put(nx, cur); q.add(nx); }
            }
        }
        if (!parent.containsKey(dest)) return null;
        List<Integer> path = new ArrayList<>();
        for (int v = dest; v != -1; v = parent.getOrDefault(v, -1)) path.add(v);
        Collections.reverse(path);
        return path;
    }

    private static String formatUdpLog(long tsMs, int currentAddr, int nextAddr, int destinationAddr, String ohtId) {
        String ts = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date(tsMs));
        String mcp = GenerationConfig.UDP_MCP;
        if (ohtId != null && !ohtId.isEmpty()) {
            String s = ohtId.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9_]", "");
            if (s.startsWith("OHT_")) s = s.substring(4);
            else if (s.startsWith("OHT")) s = s.substring(3);
            String digits = s.replaceAll("\\D+", "");
            if (!digits.isEmpty()) mcp = "OHT" + digits; // e.g., OHT1, OHT2
        }
        return "[" + ts + "]" +
                "IP:" + GenerationConfig.UDP_IP + ", " +
                "Port=" + GenerationConfig.UDP_PORT + ", " +
                "Descrption:" + GenerationConfig.UDP_DESCRIPTION + ", " +
                "Message=" + GenerationConfig.UDP_MESSAGE + "," +
                mcp + "," +
                GenerationConfig.UDP_VEHICLE + "," +
                GenerationConfig.UDP_STATE + "," +
                GenerationConfig.UDP_PRODUCT + "," +
                GenerationConfig.UDP_ERROR_CODE + "," +
                GenerationConfig.UDP_COMM_STATE + "," +
                currentAddr + "," +
                GenerationConfig.UDP_DISTANCE + "," +
                nextAddr + "," +
                GenerationConfig.UDP_RUN_CYCLE + "," +
                GenerationConfig.UDP_RUN_CYCLE_INTERVAL + "," +
                GenerationConfig.UDP_CARRIER + "," +
                destinationAddr + "," +
                GenerationConfig.UDP_EM_STATE + "," +
                GenerationConfig.UDP_GROUP_ID + "," +
                " ," +
                GenerationConfig.UDP_RETURN_PRIORITY + "," +
                GenerationConfig.UDP_JOB_DETAIL + "," +
                GenerationConfig.UDP_MOVE_DISTANCE;
    }

    public static class Result {
        public String logPath;
        public String summary;
        public String content;
    }
}


