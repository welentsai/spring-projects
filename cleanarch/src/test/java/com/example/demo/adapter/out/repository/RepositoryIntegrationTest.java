package com.example.demo.adapter.out.repository;

import com.example.demo.usecase.ports.out.entity.CityJpaEntity;
import com.example.demo.usecase.ports.out.repository.CitiesQueryRepository;
import com.example.demo.usecase.ports.out.repository.CitiesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RepositoryIntegrationTest {

    @Autowired
    private CitiesRepository citiesRepository;

    @Autowired
    private CitiesQueryRepository citiesQueryRepository;

    @Test
    void should_maintain_consistency_between_jpa_and_jdbc_repositories() {
        // Given - Insert data using JPA repository
        CityJpaEntity tokyo = new CityJpaEntity("1", "Tokyo", "Japan");
        CityJpaEntity seoul = new CityJpaEntity("2", "Seoul", "South Korea");
        CityJpaEntity bangkok = new CityJpaEntity("3", "Bangkok", "Thailand");
        
        citiesRepository.save(tokyo);
        citiesRepository.save(seoul);
        citiesRepository.save(bangkok);

        // When - Query using JDBC repository
        List<CityJpaEntity> allCitiesViaJdbc = citiesQueryRepository.findAll();
        Optional<CityJpaEntity> tokyoViaJdbc = citiesQueryRepository.findByName("Tokyo");
        List<CityJpaEntity> japanCitiesViaJdbc = citiesQueryRepository.findAllByCountry("Japan");

        // Then - Verify JDBC repository sees JPA changes
        assertThat(allCitiesViaJdbc).hasSize(3);
        assertThat(tokyoViaJdbc).isPresent();
        assertThat(tokyoViaJdbc.get().getId()).isEqualTo("1");
        assertThat(japanCitiesViaJdbc).hasSize(1);
        assertThat(japanCitiesViaJdbc.get(0).getName()).isEqualTo("Tokyo");

        // And - Query using JPA repository to verify consistency
        List<CityJpaEntity> allCitiesViaJpa = citiesRepository.findAll();
        Optional<CityJpaEntity> tokyoViaJpa = citiesRepository.findById("1");

        assertThat(allCitiesViaJpa).hasSize(3);
        assertThat(tokyoViaJpa).isPresent();
        assertThat(tokyoViaJpa.get().getName()).isEqualTo("Tokyo");
    }

    @Test
    void should_reflect_jpa_delete_operations_in_jdbc_queries() {
        // Given - Insert data using JPA
        CityJpaEntity city1 = new CityJpaEntity("1", "Tokyo", "Japan");
        CityJpaEntity city2 = new CityJpaEntity("2", "Osaka", "Japan");
        CityJpaEntity city3 = new CityJpaEntity("3", "Seoul", "South Korea");
        
        citiesRepository.save(city1);
        citiesRepository.save(city2);
        citiesRepository.save(city3);

        // Verify initial state via JDBC
        List<CityJpaEntity> initialCities = citiesQueryRepository.findAll();
        assertThat(initialCities).hasSize(3);

        // When - Delete using JPA custom method
        int deletedCount = citiesRepository.deleteByNameReturningCount("Tokyo");

        // Then - Verify deletion via JDBC queries
        assertThat(deletedCount).isEqualTo(1);
        
        List<CityJpaEntity> remainingCities = citiesQueryRepository.findAll();
        assertThat(remainingCities).hasSize(2);
        assertThat(remainingCities).extracting(CityJpaEntity::getName)
                .containsExactlyInAnyOrder("Osaka", "Seoul");

        Optional<CityJpaEntity> deletedCity = citiesQueryRepository.findByName("Tokyo");
        assertThat(deletedCity).isEmpty();

        List<CityJpaEntity> japanCities = citiesQueryRepository.findAllByCountry("Japan");
        assertThat(japanCities).hasSize(1);
        assertThat(japanCities.get(0).getName()).isEqualTo("Osaka");
    }

    @Test
    void should_handle_bulk_operations_consistently() {
        // Given - Insert multiple cities with same name
        CityJpaEntity paris1 = new CityJpaEntity("1", "Paris", "France");
        CityJpaEntity paris2 = new CityJpaEntity("2", "Paris", "USA");
        CityJpaEntity london = new CityJpaEntity("3", "London", "UK");
        CityJpaEntity madrid = new CityJpaEntity("4", "Madrid", "Spain");
        
        citiesRepository.saveAll(List.of(paris1, paris2, london, madrid));

        // Verify initial state
        List<CityJpaEntity> allCities = citiesQueryRepository.findAll();
        assertThat(allCities).hasSize(4);

        // When - Bulk delete using custom method
        int deletedCount = citiesRepository.deleteByNameReturningCount("Paris");

        // Then - Verify bulk deletion via JDBC
        assertThat(deletedCount).isEqualTo(2);
        
        List<CityJpaEntity> remainingCities = citiesQueryRepository.findAll();
        assertThat(remainingCities).hasSize(2);
        assertThat(remainingCities).extracting(CityJpaEntity::getName)
                .containsExactlyInAnyOrder("London", "Madrid");

        // Verify no Paris cities remain
        Optional<CityJpaEntity> parisCity = citiesQueryRepository.findByName("Paris");
        assertThat(parisCity).isEmpty();
    }

    @Test
    void should_handle_transactional_operations_correctly() {
        // Given - Initial state
        CityJpaEntity initialCity = new CityJpaEntity("1", "InitialCity", "InitialCountry");
        citiesRepository.save(initialCity);

        // Verify initial state via both repositories
        assertThat(citiesRepository.findAll()).hasSize(1);
        assertThat(citiesQueryRepository.findAll()).hasSize(1);

        // When - Perform multiple operations in same transaction
        CityJpaEntity newCity1 = new CityJpaEntity("2", "NewCity1", "Country1");
        CityJpaEntity newCity2 = new CityJpaEntity("3", "NewCity2", "Country2");
        
        citiesRepository.save(newCity1);
        citiesRepository.save(newCity2);
        
        int deletedCount = citiesRepository.deleteByNameReturningCount("InitialCity");

        // Then - Verify all operations are visible in same transaction
        assertThat(deletedCount).isEqualTo(1);
        
        List<CityJpaEntity> finalCitiesViaJpa = citiesRepository.findAll();
        List<CityJpaEntity> finalCitiesViaJdbc = citiesQueryRepository.findAll();
        
        assertThat(finalCitiesViaJpa).hasSize(2);
        assertThat(finalCitiesViaJdbc).hasSize(2);
        
        assertThat(finalCitiesViaJpa).extracting(CityJpaEntity::getName)
                .containsExactlyInAnyOrder("NewCity1", "NewCity2");
        assertThat(finalCitiesViaJdbc).extracting(CityJpaEntity::getName)
                .containsExactlyInAnyOrder("NewCity1", "NewCity2");
    }

    @Test
    void should_handle_edge_cases_consistently() {
        // Test empty database state
        List<CityJpaEntity> emptyCitiesJpa = citiesRepository.findAll();
        List<CityJpaEntity> emptyCitiesJdbc = citiesQueryRepository.findAll();
        
        assertThat(emptyCitiesJpa).isEmpty();
        assertThat(emptyCitiesJdbc).isEmpty();

        // Test delete on empty database
        int deletedCount = citiesRepository.deleteByNameReturningCount("NonExistentCity");
        assertThat(deletedCount).isEqualTo(0);

        // Test queries on empty database
        Optional<CityJpaEntity> notFound = citiesQueryRepository.findByName("NonExistent");
        List<CityJpaEntity> emptyCountryList = citiesQueryRepository.findAllByCountry("NonExistent");
        
        assertThat(notFound).isEmpty();
        assertThat(emptyCountryList).isEmpty();
    }

    @Test
    void should_handle_special_characters_and_unicode_consistently() {
        // Given - Cities with special characters
        CityJpaEntity saoPaulo = new CityJpaEntity("1", "São Paulo", "Brazil");
        CityJpaEntity mexicoCity = new CityJpaEntity("2", "México City", "Mexico");
        CityJpaEntity zurich = new CityJpaEntity("3", "Zürich", "Switzerland");
        
        citiesRepository.saveAll(List.of(saoPaulo, mexicoCity, zurich));

        // When - Query via JDBC
        Optional<CityJpaEntity> saoPauloViaJdbc = citiesQueryRepository.findByName("São Paulo");
        Optional<CityJpaEntity> mexicoCityViaJdbc = citiesQueryRepository.findByName("México City");
        Optional<CityJpaEntity> zurichViaJdbc = citiesQueryRepository.findByName("Zürich");

        // Then - Verify special characters are handled correctly
        assertThat(saoPauloViaJdbc).isPresent();
        assertThat(saoPauloViaJdbc.get().getCountry()).isEqualTo("Brazil");
        
        assertThat(mexicoCityViaJdbc).isPresent();
        assertThat(mexicoCityViaJdbc.get().getCountry()).isEqualTo("Mexico");
        
        assertThat(zurichViaJdbc).isPresent();
        assertThat(zurichViaJdbc.get().getCountry()).isEqualTo("Switzerland");

        // Test deletion with special characters
        int deletedCount = citiesRepository.deleteByNameReturningCount("São Paulo");
        assertThat(deletedCount).isEqualTo(1);
        
        Optional<CityJpaEntity> deletedCity = citiesQueryRepository.findByName("São Paulo");
        assertThat(deletedCity).isEmpty();
    }

    @Test
    void should_demonstrate_performance_characteristics() {
        // Given - Large dataset
        List<CityJpaEntity> cities = List.of(
            new CityJpaEntity("1", "Tokyo", "Japan"),
            new CityJpaEntity("2", "Delhi", "India"),
            new CityJpaEntity("3", "Shanghai", "China"),
            new CityJpaEntity("4", "São Paulo", "Brazil"),
            new CityJpaEntity("5", "Mexico City", "Mexico"),
            new CityJpaEntity("6", "Cairo", "Egypt"),
            new CityJpaEntity("7", "Mumbai", "India"),
            new CityJpaEntity("8", "Beijing", "China"),
            new CityJpaEntity("9", "Dhaka", "Bangladesh"),
            new CityJpaEntity("10", "Osaka", "Japan")
        );
        
        citiesRepository.saveAll(cities);

        // When - Perform queries via both methods
        long startTimeJpa = System.currentTimeMillis();
        List<CityJpaEntity> allCitiesJpa = citiesRepository.findAll();
        long jpaTime = System.currentTimeMillis() - startTimeJpa;

        long startTimeJdbc = System.currentTimeMillis();
        List<CityJpaEntity> allCitiesJdbc = citiesQueryRepository.findAll();
        long jdbcTime = System.currentTimeMillis() - startTimeJdbc;

        // Then - Verify both return same results
        assertThat(allCitiesJpa).hasSize(10);
        assertThat(allCitiesJdbc).hasSize(10);
        
        // Both should contain the same cities (order might differ)
        assertThat(allCitiesJpa).extracting(CityJpaEntity::getName)
                .containsExactlyInAnyOrderElementsOf(
                    allCitiesJdbc.stream().map(CityJpaEntity::getName).toList()
                );

        // Performance comparison (informational - actual times may vary)
        System.out.println("JPA query time: " + jpaTime + "ms");
        System.out.println("JDBC query time: " + jdbcTime + "ms");
    }
}
