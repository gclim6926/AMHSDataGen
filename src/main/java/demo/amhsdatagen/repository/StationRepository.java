package demo.amhsdatagen.repository;

import demo.amhsdatagen.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {
}


