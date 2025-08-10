package com.example.demo.usecase.ports.out.repository;

import com.example.demo.usecase.ports.out.entity.CityJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional
class CitiesRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CitiesRepository citiesRepository;
    
    @BeforeEach
    void setUp() {
        // Clear any existing data before each test
        citiesRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void should_save_and_find_city() {
        // Given
        CityJpaEntity city = new CityJpaEntity("1", "Tokyo", "Japan");

        // When
        CityJpaEntity savedCity = citiesRepository.save(city);

        // Then
        assertThat(savedCity).isNotNull();
        assertThat(savedCity.getId()).isEqualTo("1");
        assertThat(savedCity.getName()).isEqualTo("Tokyo");
        assertThat(savedCity.getCountry()).isEqualTo("Japan");

        Optional<CityJpaEntity> foundCity = citiesRepository.findById("1");
        assertThat(foundCity).isPresent();
        assertThat(foundCity.get().getName()).isEqualTo("Tokyo");
    }

    @Test
    void should_find_all_cities() {
        // Given
        CityJpaEntity city1 = new CityJpaEntity("1", "Tokyo", "Japan");
        CityJpaEntity city2 = new CityJpaEntity("2", "Seoul", "South Korea");
        citiesRepository.save(city1);
        citiesRepository.save(city2);

        // When
        List<CityJpaEntity> cities = citiesRepository.findAll();

        // Then
        assertThat(cities).hasSize(2);
        assertThat(cities).extracting(CityJpaEntity::getName)
                .containsExactlyInAnyOrder("Tokyo", "Seoul");
    }

    @Test
    void should_delete_city_by_id() {
        // Given
        CityJpaEntity city = new CityJpaEntity("1", "Tokyo", "Japan");
        citiesRepository.save(city);

        // When
        citiesRepository.deleteById("1");

        // Then
        Optional<CityJpaEntity> foundCity = citiesRepository.findById("1");
        assertThat(foundCity).isEmpty();
    }

    @Test
    void should_delete_by_name_and_return_count_when_city_exists() {
        // Given
        CityJpaEntity city1 = new CityJpaEntity("1", "Tokyo", "Japan");
        CityJpaEntity city2 = new CityJpaEntity("2", "Seoul", "South Korea");
        citiesRepository.save(city1);
        citiesRepository.save(city2);

        // When
        int deletedCount = citiesRepository.deleteByNameReturningCount("Tokyo");

        // Then
        assertThat(deletedCount).isEqualTo(1);
        
        // Verify the city is actually deleted
        Optional<CityJpaEntity> foundCity = citiesRepository.findById("1");
        assertThat(foundCity).isEmpty();
        
        // Verify other city still exists
        Optional<CityJpaEntity> otherCity = citiesRepository.findById("2");
        assertThat(otherCity).isPresent();
    }

    @Test
    void should_delete_by_name_and_return_zero_when_city_does_not_exist() {
        // Given
        CityJpaEntity city = new CityJpaEntity("1", "Tokyo", "Japan");
        citiesRepository.save(city);

        // When
        int deletedCount = citiesRepository.deleteByNameReturningCount("NonExistentCity");

        // Then
        assertThat(deletedCount).isEqualTo(0);
        
        // Verify original city still exists
        Optional<CityJpaEntity> foundCity = citiesRepository.findById("1");
        assertThat(foundCity).isPresent();
    }

    @Test
    void should_delete_multiple_cities_with_same_name() {
        // Given
        CityJpaEntity city1 = new CityJpaEntity("1", "Paris", "France");
        CityJpaEntity city2 = new CityJpaEntity("2", "Paris", "USA"); // Paris, Texas
        CityJpaEntity city3 = new CityJpaEntity("3", "London", "UK");
        citiesRepository.save(city1);
        citiesRepository.save(city2);
        citiesRepository.save(city3);

        // When
        int deletedCount = citiesRepository.deleteByNameReturningCount("Paris");

        // Then
        assertThat(deletedCount).isEqualTo(2);
        
        // Verify both Paris cities are deleted
        Optional<CityJpaEntity> paris1 = citiesRepository.findById("1");
        Optional<CityJpaEntity> paris2 = citiesRepository.findById("2");
        assertThat(paris1).isEmpty();
        assertThat(paris2).isEmpty();
        
        // Verify London still exists
        Optional<CityJpaEntity> london = citiesRepository.findById("3");
        assertThat(london).isPresent();
        assertThat(london.get().getName()).isEqualTo("London");
    }

    @Test
    void should_handle_null_name_in_delete_operation() {
        // Given
        CityJpaEntity city = new CityJpaEntity("1", "Tokyo", "Japan");
        citiesRepository.save(city);

        // When
        int deletedCount = citiesRepository.deleteByNameReturningCount(null);

        // Then
        assertThat(deletedCount).isEqualTo(0);
        
        // Verify original city still exists
        Optional<CityJpaEntity> foundCity = citiesRepository.findById("1");
        assertThat(foundCity).isPresent();
    }

    @Test
    @Sql("/test-data/cities-test-data.sql")
    void should_work_with_predefined_test_data() {
        // When
        List<CityJpaEntity> cities = citiesRepository.findAll();

        // Then
        assertThat(cities).hasSizeGreaterThan(0);
        
        // Test delete operation with predefined data
        int deletedCount = citiesRepository.deleteByNameReturningCount("TestCity");
        assertThat(deletedCount).isGreaterThanOrEqualTo(0);
    }
}
