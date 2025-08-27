package demo.layoutviz.repository;

import demo.layoutviz.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
}


