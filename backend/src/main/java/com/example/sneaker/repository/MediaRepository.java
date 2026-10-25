package com.example.sneaker.repository;

import com.example.sneaker.model.MediaAsset;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface MediaRepository
    extends JpaRepository<MediaAsset, String>, JpaSpecificationExecutor<MediaAsset> {
  Optional<MediaAsset> findBySha256(String sha256);
}
