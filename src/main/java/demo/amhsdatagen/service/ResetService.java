package demo.amhsdatagen.service;

import demo.amhsdatagen.repository.AddressRepository;
import demo.amhsdatagen.repository.LineRepository;
import demo.amhsdatagen.repository.StationRepository;
import demo.amhsdatagen.repository.ConfigEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResetService {
    private final AddressRepository addressRepository;
    private final LineRepository lineRepository;
    private final StationRepository stationRepository;
    private final ConfigEntryRepository configRepository;

    public ResetService(AddressRepository addressRepository,
                        LineRepository lineRepository,
                        StationRepository stationRepository,
                        ConfigEntryRepository configRepository) {
        this.addressRepository = addressRepository;
        this.lineRepository = lineRepository;
        this.stationRepository = stationRepository;
        this.configRepository = configRepository;
    }

    @Transactional
    public void resetAll() {
        // 순서 주의: 라인이 주소를 참조할 수 있으므로 라인 먼저 삭제
        lineRepository.deleteAll();
        addressRepository.deleteAll();
        stationRepository.deleteAll();
        configRepository.deleteAll();
    }
}


