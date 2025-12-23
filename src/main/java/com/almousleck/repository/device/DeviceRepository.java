package com.almousleck.repository.device;

import com.almousleck.model.Device;
import com.almousleck.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findBySerialNumber(String serialNumber);
    List<Device> findByOwner(User owner);
    boolean existsBySerialNumber(String serialNumber);
}
