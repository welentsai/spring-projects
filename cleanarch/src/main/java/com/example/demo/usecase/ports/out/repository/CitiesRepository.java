package com.example.demo.usecase.ports.out.repository;

import com.example.demo.usecase.ports.out.entity.CityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


public interface CitiesRepository extends JpaRepository<CityJpaEntity, String> {
    // Command operations (insert, update, delete) are inherited from JpaRepository

    // custom delete method
    @Transactional
    @Modifying
    @Query("DELETE FROM CityJpaEntity c WHERE c.name = :name")
    int deleteByNameReturningCount(@Param("name") String name);
}
