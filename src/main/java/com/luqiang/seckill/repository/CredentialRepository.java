package com.luqiang.seckill.repository;

import com.luqiang.seckill.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    List<Credential> findByUserId(Long userId);
    Optional<Credential> findByCredentialId(String credentialId);
}
