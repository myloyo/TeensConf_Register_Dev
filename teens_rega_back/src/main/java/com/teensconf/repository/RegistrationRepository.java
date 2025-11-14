package com.teensconf.repository;

import com.teensconf.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    long countByRegistrationCompletedAtIsNotNull();
    long countByRegistrationCompletedAtIsNull();
}