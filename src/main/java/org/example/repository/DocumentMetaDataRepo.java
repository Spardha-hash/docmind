package org.example.repository;

import org.example.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentMetaDataRepo extends JpaRepository<DocumentMetadata, UUID> {
}
