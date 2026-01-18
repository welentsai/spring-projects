package com.example.demo.adapter.out.repository;

import com.example.demo.usecase.ports.out.entity.CityJpaEntity;
import com.example.demo.usecase.ports.out.repository.CitiesQueryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CitiesQueryRepositoryImpl implements CitiesQueryRepository {

    private final JdbcClient jdbcClient;

    public CitiesQueryRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<CityJpaEntity> findAll() {
        String sql = "SELECT id, name, country FROM city_jpa_entity";

        return jdbcClient
                .sql(sql)
                .query(
                        (rs, rowNum) -> {
                            CityJpaEntity entity = new CityJpaEntity();
                            entity.setId(rs.getString("id"));
                            entity.setName(rs.getString("name"));
                            entity.setCountry(rs.getString("country"));
                            return entity;
                        })
                .list();
    }

    @Override
    public Optional<CityJpaEntity> findByName(String name) {
        String sql = "SELECT id, name, country FROM city_jpa_entity WHERE name = :name";

        return jdbcClient
                .sql(sql)
                .param("name", name)
                .query(
                        (rs, rowNum) -> {
                            CityJpaEntity entity = new CityJpaEntity();
                            entity.setId(rs.getString("id"));
                            entity.setName(rs.getString("name"));
                            entity.setCountry(rs.getString("country"));
                            return entity;
                        })
                .optional();
    }

    @Override
    public List<CityJpaEntity> findAllByCountry(String country) {
        String sql = "SELECT id, name, country FROM city_jpa_entity WHERE country = :country";

        return jdbcClient
                .sql(sql)
                .param("country", country)
                .query(
                        (rs, rowNum) -> {
                            CityJpaEntity entity = new CityJpaEntity();
                            entity.setId(rs.getString("id"));
                            entity.setName(rs.getString("name"));
                            entity.setCountry(rs.getString("country"));
                            return entity;
                        })
                .list();
    }
}
