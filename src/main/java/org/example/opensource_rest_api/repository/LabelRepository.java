package org.example.opensource_rest_api.repository;

import org.example.opensource_rest_api.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, Long> {
}
