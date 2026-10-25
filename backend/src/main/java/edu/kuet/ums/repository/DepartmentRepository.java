package edu.kuet.ums.repository;

import edu.kuet.ums.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);
    Optional<Department> findByName(String name);
    List<Department> findAllByOrderByNameAsc();
}

