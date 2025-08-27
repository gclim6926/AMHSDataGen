package demo.layoutviz.service;

import demo.layoutviz.model.Address;
import demo.layoutviz.model.Line;
import demo.layoutviz.repository.AddressRepository;
import demo.layoutviz.repository.LineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DbDataService {
	private final AddressRepository addressRepository;
	private final LineRepository lineRepository;

	public DbDataService(AddressRepository addressRepository, LineRepository lineRepository) {
		this.addressRepository = addressRepository;
		this.lineRepository = lineRepository;
	}

	@Transactional
	public void saveAddressesAndLines(List<Address> addresses, List<Line> lines) {
		addressRepository.saveAll(addresses);
		lineRepository.saveAll(lines);
	}

	public long countAddresses() {
		return addressRepository.count();
	}

	public long countLines() {
		return lineRepository.count();
	}
}
