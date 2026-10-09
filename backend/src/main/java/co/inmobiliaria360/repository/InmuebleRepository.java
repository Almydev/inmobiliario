package co.inmobiliaria360.repository;

import co.inmobiliaria360.domain.Inmueble;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InmuebleRepository extends JpaRepository<Inmueble, Long> {
}
