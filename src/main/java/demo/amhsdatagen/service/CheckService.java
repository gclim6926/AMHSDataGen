package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CheckService {

    private final ConfigService configService;

    public CheckService(ConfigService configService) {
        this.configService = configService;
    }

    public Result runCheck() throws IOException {
        JsonNode root = configService.loadOutputFromDb().orElseThrow(() -> new IOException("output not found in DB"));
        ArrayNode addressesNode = safeArray(root.path("addresses"));
        ArrayNode linesNode = safeArray(root.path("lines"));

        StringBuilder log = new StringBuilder();
        log.append("==============================================\n");
        log.append("데이터 무결성 검사 시작 ").append(now()).append("\n");
        log.append("addresses=").append(addressesNode.size()).append(", lines=").append(linesNode.size()).append("\n");

        // 1) Address 중복 검사 (id, name, pos)
        Set<Long> seenAddrIds = new HashSet<>();
        Set<Long> dupAddrIds = new HashSet<>();
        Map<String, Integer> nameCount = new HashMap<>();
        Map<String, Integer> posCount = new HashMap<>();

        for (JsonNode a : addressesNode) {
            long id = a.path("id").asLong();
            String name = a.path("name").asText("");
            String posKey = posKey(a.path("pos"));
            if (!seenAddrIds.add(id)) dupAddrIds.add(id);
            nameCount.put(name, nameCount.getOrDefault(name, 0) + 1);
            posCount.put(posKey, posCount.getOrDefault(posKey, 0) + 1);
        }

        long dupName = nameCount.values().stream().filter(c -> c > 1).count();
        long dupPos = posCount.values().stream().filter(c -> c > 1).count();
        log.append("[Addresses] dupId=").append(dupAddrIds.size()).append(", dupNameKeys=").append(dupName).append(", dupPosKeys=").append(dupPos).append("\n");

        // 2) Line 중복 검사 (id, identical/reverse)
        Set<Long> seenLineIds = new HashSet<>();
        Set<Long> dupLineIds = new HashSet<>();
        Set<String> seenEdge = new HashSet<>();
        List<Integer> overlapIndexes = new ArrayList<>();
        for (int i = 0; i < linesNode.size(); i++) {
            JsonNode l = linesNode.get(i);
            long id = l.path("id").asLong();
            if (!seenLineIds.add(id)) dupLineIds.add(id);
            long from = l.path("fromAddress").asLong();
            long to = l.path("toAddress").asLong();
            String key = edgeKey(from, to);
            String rkey = edgeKey(to, from);
            if (seenEdge.contains(key) || seenEdge.contains(rkey)) {
                overlapIndexes.add(i);
            } else {
                seenEdge.add(key);
            }
        }
        log.append("[Lines] dupId=").append(dupLineIds.size()).append(", overlaps=").append(overlapIndexes.size()).append("\n");

        // 3) 겹치는 라인 제거 (중복 연결: 동일/역방향)
        if (!overlapIndexes.isEmpty()) {
            log.append("겹치는 라인 삭제 대상: ").append(overlapIndexes.size()).append("\n");
            // 역순 삭제
            List<Integer> sorted = new ArrayList<>(overlapIndexes);
            sorted.sort(Comparator.reverseOrder());
            for (int idx : sorted) {
                // remove by rebuilding array
                removeAt((ArrayNode) linesNode, idx);
            }
            log.append("삭제 후 lines=").append(linesNode.size()).append("\n");
        }

        // 4) 고연결 주소(>=4)
        Map<Long, Integer> usage = new HashMap<>();
        for (JsonNode l : linesNode) {
            long from = l.path("fromAddress").asLong();
            long to = l.path("toAddress").asLong();
            usage.put(from, usage.getOrDefault(from, 0) + 1);
            usage.put(to, usage.getOrDefault(to, 0) + 1);
        }
        long highly = usage.values().stream().filter(c -> c >= 4).count();
        log.append("highlyConnected(>=4)=").append(highly).append("\n");

        // 결과를 DB에 저장
        configService.saveOutputToDb(root);

        Result r = new Result();
        r.status = "OK";
        r.summary = String.format("addrDup=%d, nameDupKeys=%d, posDupKeys=%d, lineDup=%d, overlapsRemoved=%d, highly>=4=%d",
                dupAddrIds.size(), dupName, dupPos, dupLineIds.size(), overlapIndexes.size(), highly);
        r.logText = log.toString();
        r.layoutPath = "db://AMHS_data:layout_seed.output";
        return r;
    }

    // no-op: legacy file->entity conversion helpers removed after DB-only refactor

    private static ArrayNode safeArray(JsonNode node) {
        if (node != null && node.isArray()) return (ArrayNode) node;
        return new ObjectMapper().createArrayNode();
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static String posKey(JsonNode pos) {
        double x = pos.path("x").asDouble();
        double y = pos.path("y").asDouble();
        double z = pos.path("z").asDouble();
        return String.format("(%.1f,%.1f,%.1f)", x, y, z);
    }

    private static String edgeKey(long a, long b) {
        return a + "->" + b;
    }

    private static void removeAt(ArrayNode array, int index) {
        ArrayNode newArr = array.arrayNode();
        for (int i = 0; i < array.size(); i++) {
            if (i == index) continue;
            newArr.add(array.get(i));
        }
        array.removeAll();
        array.addAll(newArr);
    }

    public static class Result {
        public String status;
        public String summary;
        public String logText;
        public String layoutPath;
    }
}


