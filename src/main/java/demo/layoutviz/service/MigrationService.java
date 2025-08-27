package demo.layoutviz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import demo.layoutviz.model.Address;
import demo.layoutviz.model.Line;
import demo.layoutviz.model.Position;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class MigrationService {
    private final DataFileService dataFileService;
    private final DbDataService dbDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MigrationService(DataFileService dataFileService, DbDataService dbDataService) {
        this.dataFileService = dataFileService;
        this.dbDataService = dbDataService;
    }

    public Result migrateOutputJsonToDb() throws IOException {
        Path outputPath = dataFileService.getOutputPath();
        if (!Files.exists(outputPath)) {
            throw new IOException("output.json not found: " + outputPath);
        }
        JsonNode root = objectMapper.readTree(outputPath.toFile());
        ArrayNode addressesNode = safeArray(root.path("addresses"));
        ArrayNode linesNode = safeArray(root.path("lines"));

        List<Address> addresses = new ArrayList<>();
        for (JsonNode a : addressesNode) {
            long id = a.path("id").asLong();
            long address = a.path("address").asLong();
            String name = a.path("name").asText("");
            JsonNode p = a.path("pos");
            Position pos = new Position(p.path("x").asDouble(), p.path("y").asDouble(), p.path("z").asDouble());
            addresses.add(new Address(id, address, name, pos));
        }

        List<Line> lines = new ArrayList<>();
        for (JsonNode l : linesNode) {
            long id = l.path("id").asLong();
            String name = l.path("name").asText("");
            long from = l.path("fromAddress").asLong();
            long to = l.path("toAddress").asLong();
            JsonNode fp = l.path("fromPos");
            JsonNode tp = l.path("toPos");
            Position fromPos = new Position(fp.path("x").asDouble(), fp.path("y").asDouble(), fp.path("z").asDouble());
            Position toPos = new Position(tp.path("x").asDouble(), tp.path("y").asDouble(), tp.path("z").asDouble());
            boolean curve = l.path("curve").asBoolean(false);
            lines.add(new Line(id, name, from, to, fromPos, toPos, curve));
        }

        dbDataService.saveAddressesAndLines(addresses, lines);

        Result r = new Result();
        r.addresses = addresses.size();
        r.lines = lines.size();
        return r;
    }

    private static ArrayNode safeArray(JsonNode node) {
        if (node != null && node.isArray()) return (ArrayNode) node;
        return new ObjectMapper().createArrayNode();
    }

    public static class Result {
        public int addresses;
        public int lines;
    }
}


