package com.example.demo.adapter.out.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.usecase.ports.out.entity.CityJpaEntity;
import com.example.demo.usecase.ports.out.repository.CitiesQueryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

@JdbcTest
@Import(CitiesQueryRepositoryImpl.class)
@ActiveProfiles("test")
class CitiesQueryRepositoryTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private CitiesQueryRepository citiesQueryRepository;

    @BeforeEach
    void setUp() {
        // Create table for testing (since @JdbcTest doesn't include JPA auto-creation)
        jdbcClient.sql("""
            CREATE TABLE IF NOT EXISTS city_jpa_entity (
                id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255),
                country VARCHAR(255)
            )
        """).update();

        // Clear any existing data
        jdbcClient.sql("DELETE FROM city_jpa_entity").update();
    }

    @Test
    void should_find_all_cities_when_cities_exist() {
        // Given
        insertTestCity("1", "Tokyo", "Japan");
        insertTestCity("2", "Seoul", "South Korea");
        insertTestCity("3", "Bangkok", "Thailand");

        // When
        List<CityJpaEntity> cities = citiesQueryRepository.findAll();

        // Then
        assertThat(cities).hasSize(3);
        assertThat(cities)
                .extracting(CityJpaEntity::getName)
                .containsExactlyInAnyOrder("Tokyo", "Seoul", "Bangkok");
        assertThat(cities)
                .extracting(CityJpaEntity::getCountry)
                .containsExactlyInAnyOrder("Japan", "South Korea", "Thailand");
    }

    @Test
    void should_return_empty_list_when_no_cities_exist() {
        // When
        List<CityJpaEntity> cities = citiesQueryRepository.findAll();

        // Then
        assertThat(cities).isEmpty();
    }

    @Test
    void should_find_city_by_name_when_city_exists() {
        // Given
        insertTestCity("1", "Tokyo", "Japan");
        insertTestCity("2", "Seoul", "South Korea");

        // When
        Optional<CityJpaEntity> foundCity = citiesQueryRepository.findByName("Tokyo");

        // Then
        assertThat(foundCity).isPresent();
        assertThat(foundCity.get().getId()).isEqualTo("1");
        assertThat(foundCity.get().getName()).isEqualTo("Tokyo");
        assertThat(foundCity.get().getCountry()).isEqualTo("Japan");
    }

    @Test
    void should_return_empty_optional_when_city_name_does_not_exist() {
        // Given
        insertTestCity("1", "Tokyo", "Japan");

        // When
        Optional<CityJpaEntity> foundCity = citiesQueryRepository.findByName("NonExistentCity");

        // Then
        assertThat(foundCity).isEmpty();
    }

    @Test
    void should_handle_null_name_in_find_by_name() {
        // Given
        insertTestCity("1", "Tokyo", "Japan");

        // When
        Optional<CityJpaEntity> foundCity = citiesQueryRepository.findByName(null);

        // Then
        assertThat(foundCity).isEmpty();
    }

    @Test
    void should_find_cities_by_country_when_cities_exist() {
        // Given
        insertTestCity("1", "Tokyo", "Japan");
        insertTestCity("2", "Osaka", "Japan");
        insertTestCity("3", "Seoul", "South Korea");
        insertTestCity("4", "Busan", "South Korea");
        insertTestCity("5", "Bangkok", "Thailand");

        // When
        List<CityJpaEntity> japanCities = citiesQueryRepository.findAllByCountry("Japan");

        // Then
        assertThat(japanCities).hasSize(2);
        assertThat(japanCities)
                .extracting(CityJpaEntity::getName)
                .containsExactlyInAnyOrder("Tokyo", "Osaka");
        assertThat(japanCities).allMatch(city -> "Japan".equals(city.getCountry()));
    }

    @Test
    void should_return_empty_list_when_no_cities_in_country() {
        // Given
        insertTestCity("1", "Tokyo", "Japan");
        insertTestCity("2", "Seoul", "South Korea");

        // When
        List<CityJpaEntity> cities = citiesQueryRepository.findAllByCountry("NonExistentCountry");

        // Then
        assertThat(cities).isEmpty();
    }

    @Test
    void should_handle_null_country_in_find_by_country() {
        // Given
        insertTestCity("1", "Tokyo", "Japan");

        // When
        List<CityJpaEntity> cities = citiesQueryRepository.findAllByCountry(null);

        // Then
        assertThat(cities).isEmpty();
    }

    @Test
    void should_handle_case_sensitive_searches() {
        // Given
        insertTestCity("1", "Tokyo", "Japan");

        // When
        Optional<CityJpaEntity> foundCity1 = citiesQueryRepository.findByName("Tokyo");
        Optional<CityJpaEntity> foundCity2 = citiesQueryRepository.findByName("TOKYO");
        Optional<CityJpaEntity> foundCity3 = citiesQueryRepository.findByName("tokyo");

        // Then
        assertThat(foundCity1).isPresent();
        assertThat(foundCity2).isEmpty(); // Case sensitive
        assertThat(foundCity3).isEmpty(); // Case sensitive
    }

    @Test
    void should_handle_special_characters_in_city_names() {
        // Given
        insertTestCity("1", "São Paulo", "Brazil");
        insertTestCity("2", "México City", "Mexico");
        insertTestCity("3", "Zürich", "Switzerland");

        // When
        Optional<CityJpaEntity> saoPaulo = citiesQueryRepository.findByName("São Paulo");
        Optional<CityJpaEntity> mexicoCity = citiesQueryRepository.findByName("México City");
        Optional<CityJpaEntity> zurich = citiesQueryRepository.findByName("Zürich");

        // Then
        assertThat(saoPaulo).isPresent();
        assertThat(saoPaulo.get().getCountry()).isEqualTo("Brazil");

        assertThat(mexicoCity).isPresent();
        assertThat(mexicoCity.get().getCountry()).isEqualTo("Mexico");

        assertThat(zurich).isPresent();
        assertThat(zurich.get().getCountry()).isEqualTo("Switzerland");
    }

    @Test
    void should_work_with_predefined_test_data() {
        // Given - Insert test data manually
        insertTestCity("query1", "TestQueryCity", "TestCountry");
        insertTestCity("query2", "AnotherQueryCity", "TestCountry");
        insertTestCity("query3", "QueryCity3", "AnotherTestCountry");

        // When
        List<CityJpaEntity> allCities = citiesQueryRepository.findAll();

        // Then
        assertThat(allCities).hasSize(3);

        // Test specific queries with predefined data
        Optional<CityJpaEntity> testCity = citiesQueryRepository.findByName("TestQueryCity");
        List<CityJpaEntity> testCountryCities =
                citiesQueryRepository.findAllByCountry("TestCountry");

        assertThat(testCity).isPresent();
        assertThat(testCity.get().getId()).isEqualTo("query1");
        assertThat(testCountryCities).hasSize(2);
    }

    @Test
    void should_maintain_data_consistency_across_multiple_queries() {
        // Given
        insertTestCity("1", "Tokyo", "Japan");
        insertTestCity("2", "Osaka", "Japan");
        insertTestCity("3", "Seoul", "South Korea");

        // When - Multiple queries
        List<CityJpaEntity> allCities = citiesQueryRepository.findAll();
        List<CityJpaEntity> japanCities = citiesQueryRepository.findAllByCountry("Japan");
        Optional<CityJpaEntity> tokyo = citiesQueryRepository.findByName("Tokyo");

        // Then - Verify consistency
        assertThat(allCities).hasSize(3);
        assertThat(japanCities).hasSize(2);
        assertThat(tokyo).isPresent();

        // Verify the same Tokyo entity is consistent across queries
        CityJpaEntity tokyoFromAll = allCities.stream()
                .filter(city -> "Tokyo".equals(city.getName()))
                .findFirst()
                .orElseThrow();

        CityJpaEntity tokyoFromJapan = japanCities.stream()
                .filter(city -> "Tokyo".equals(city.getName()))
                .findFirst()
                .orElseThrow();

        assertThat(tokyo.get().getId()).isEqualTo(tokyoFromAll.getId());
        assertThat(tokyo.get().getId()).isEqualTo(tokyoFromJapan.getId());
    }

    private void insertTestCity(String id, String name, String country) {
        jdbcClient
                .sql("INSERT INTO city_jpa_entity (id, name, country) VALUES (?, ?, ?)")
                .params(id, name, country)
                .update();
    }
}
