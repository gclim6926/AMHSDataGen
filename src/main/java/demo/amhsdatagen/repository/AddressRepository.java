package demo.amhsdatagen.repository;

import demo.amhsdatagen.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
}


