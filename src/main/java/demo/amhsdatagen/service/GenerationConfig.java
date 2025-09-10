package demo.amhsdatagen.service;

import java.nio.file.Path;

public final class GenerationConfig {
    private GenerationConfig() {}

    public static final String LAYOUT_FILE = "layout.json";
    public static final String INPUT_FILE = "input.json";
    public static final String OUTPUT_FILE = "output.json";

    public static final double[] RANDOM_INTERVAL = new double[] {54.0, 58.0};
    public static final long ADDRESS_ID_START = 100001L;
    public static final long LINE_ID_START = 200001L;

    // Stations
    public static final int EQUIPMENTS = 500;
    public static final long STATION_ID_START = 300003L;
    public static final int STATION_Y_INTERVAL = 20;

    // UDP Generation defaults
    public static final String UDP_IP = "10.10.10.1";
    public static final String UDP_PORT = "3600";
    public static final String UDP_DESCRIPTION = "DT";
    public static final String UDP_MESSAGE = "2";
    public static final String UDP_MCP = "OHT";
    public static final String UDP_VEHICLE = "V00001";
    public static final String UDP_STATE = "1";
    public static final String UDP_PRODUCT = "0";
    public static final String UDP_ERROR_CODE = "0000";
    public static final String UDP_COMM_STATE = "1";
    public static final String UDP_DISTANCE = "0";
    public static final String UDP_RUN_CYCLE = "2";
    public static final String UDP_RUN_CYCLE_INTERVAL = "1";
    public static final String UDP_CARRIER = "AAAA0000";
    public static final String UDP_EM_STATE = "00000000";
    public static final String UDP_GROUP_ID = "0000";
    public static final String UDP_RETURN_PRIORITY = "0";
    public static final String UDP_JOB_DETAIL = "101";
    public static final String UDP_MOVE_DISTANCE = "0";

    public static final int UDP_TIME_INCREMENT_MIN_MS = 300;
    public static final int UDP_TIME_INCREMENT_MAX_MS = 400;

    public static Path dataDir(Path projectRoot) {
        // 기본적으로 프로젝트 루트의 copy_layout 디렉토리를 데이터 폴더로 사용
        return projectRoot.resolve("copy_layout");
    }
}