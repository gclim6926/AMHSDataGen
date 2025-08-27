package demo.layoutviz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class Visualize2DService {

    private final DataFileService dataFileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Visualize2DService(DataFileService dataFileService) {
        this.dataFileService = dataFileService;
    }

    public Result run(JsonNode filters) throws IOException {
        Path outputPath = dataFileService.getOutputPath();
        Result r = new Result();
        r.status = "OK";
        r.info = String.format("2D visualize with filters: %s; output=%s", filters != null ? filters.toString() : "{}", outputPath);
        return r;
    }

    public static class Result {
        public String status;
        public String info;
    }
}


