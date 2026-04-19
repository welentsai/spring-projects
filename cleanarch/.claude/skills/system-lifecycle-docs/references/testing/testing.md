# Reference: Testing Strategy

## Testing Philosophy

Tests in a Clean Architecture project have a natural home in each layer. The goal is a
**test pyramid**: many fast unit tests at the bottom, fewer integration tests in the middle,
and minimal end-to-end tests at the top. Each layer is independently testable — which is one
of the key benefits Clean Architecture buys you.

```
         ┌─────────────────────┐
         │   E2E / Contract    │  ← few, slow, catch wire-up bugs
         │  ───────────────── │
         │    Integration      │  ← datasource routing, full Spring context
         │  ───────────────── │
         │ Architecture Rules  │  ← ArchUnit, fast, structural correctness
         │  ───────────────── │
         │     Unit Tests      │  ← most tests, fast, no Spring context
         └─────────────────────┘
```

---

## Layer-by-Layer Testing Guide

### Domain Layer (`domain.model`)

Pure Java records/classes — test with plain JUnit, no Spring:

```java
class CityTest {
    @Test
    void city_shouldBeCreatedWithValidFields() {
        var city = new City("1", "Taipei", "TW");
        assertThat(city.name()).isEqualTo("Taipei");
        assertThat(city.countryCode()).isEqualTo("TW");
    }
}
```

### Use Case Layer (`usecase.ports.in.impl`)

Test with Mockito — no Spring context, constructor-inject mocks:

```java
@ExtendWith(MockitoExtension.class)
class FindCitiesUseCaseImplTest {

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private FindCitiesUseCaseImpl useCase;

    @Test
    void execute_shouldReturnCities_whenRegionExists() {
        var cities = List.of(new CityDto("1", "Taipei", "TW"));
        given(cityRepository.findByRegion("TW")).willReturn(cities);

        var result = useCase.execute(new FindCitiesInput("TW"));

        assertThat(result.cities()).hasSize(1);
        assertThat(result.cities().get(0).name()).isEqualTo("Taipei");
        then(cityRepository).should().findByRegion("TW");
    }

    @Test
    void execute_shouldReturnEmptyList_whenRegionHasNoCities() {
        given(cityRepository.findByRegion("XX")).willReturn(List.of());

        var result = useCase.execute(new FindCitiesInput("XX"));

        assertThat(result.cities()).isEmpty();
    }
}
```

### Controller Layer (`adapter.in`)

Use `@WebMvcTest` — loads only web layer, mock use cases with `@MockitoBean`:

```java
@WebMvcTest(CitiesController.class)
class CitiesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindCitiesUseCase findCitiesUseCase;

    @Test
    void getCities_shouldReturn200_withValidRegion() throws Exception {
        var result = new FindCitiesResult(List.of(new CityDto("1", "Taipei", "TW")));
        given(findCitiesUseCase.execute(any())).willReturn(result);

        mockMvc.perform(get("/api/v1/cities").param("region", "TW"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cities[0].name").value("Taipei"));
    }

    @Test
    void getCities_shouldReturn400_whenRegionIsBlank() throws Exception {
        mockMvc.perform(get("/api/v1/cities").param("region", ""))
            .andExpect(status().isBadRequest());
    }
}
```

### Repository Layer (`adapter.out.repository`)

Use `@JdbcTest` or `@SpringBootTest` with an in-memory DB:

```java
@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CityRepositoryImplTest {

    @Autowired
    private JdbcClient jdbcClient;

    private CityRepositoryImpl repository;

    @BeforeEach
    void setup() {
        repository = new CityRepositoryImpl(jdbcClient);
    }

    @Test
    void findByRegion_shouldReturnMatchingCities() {
        var cities = repository.findByRegion("TW");
        assertThat(cities).isNotEmpty();
    }
}
```

---

## Architecture Tests (ArchUnit)

ArchUnit tests run fast (no Spring context) and enforce structural rules automatically.
Run after any package or class restructuring:

```java
@AnalyzeClasses(packages = "com.example.demo")
class ArchunitRuleTest {

    @ArchTest
    static final ArchRule domainHasNoSpringDependency =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule usecaseImplHasNoSpringAnnotations =
        noClasses().that().resideInAPackage("..usecase..impl..")
            .should().beAnnotatedWith(Service.class)
            .orShould().beAnnotatedWith(Component.class)
            .orShould().beAnnotatedWith(Repository.class);

    @ArchTest
    static final ArchRule controllerNamingConvention =
        classes().that().resideInAPackage("..adapter.in..")
            .and().areAnnotatedWith(RestController.class)
            .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule noDirectControllerToDaoAccess =
        noClasses().that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter.out..");
}
```

---

## Integration Tests

Test full request flow including datasource routing, transaction boundaries:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCities_shouldRouteToCorrectDatasource() throws Exception {
        mockMvc.perform(get("/api/v1/cities")
                .param("region", "TW")
                .header("X-Datasource", "PRIMARY"))
            .andExpect(status().isOk());
    }
}
```

For datasource routing specifically:

```java
@SpringBootTest
class DynamicDataSourceIntegrationTest {

    @Test
    void primaryDatasource_shouldBeAccessible() {
        DataSourceContextHolder.set(DataSourceKey.PRIMARY);
        // perform query
        DataSourceContextHolder.clear();
    }
}
```

---

## Test Naming Convention

Use `methodName_shouldDoX_whenConditionY` format:

```java
void execute_shouldReturnEmpty_whenRegionHasNoCities()
void getCities_shouldReturn400_whenRegionIsBlank()
void findByRegion_shouldThrow_whenDatabaseIsDown()
```

This makes test failure messages self-explanatory without opening the test file.

---

## Test Data Management

- **Unit tests**: construct objects inline — no test data files
- **Integration tests**: use `@Sql` to load fixtures from `src/test/resources/sql/`
- **H2 in-memory**: schema auto-created from JPA entities or `schema.sql` in test resources

```java
@Test
@Sql("/sql/cities.sql")
void getCities_shouldReturnSeededData() { ... }
```

---

## Coverage Targets

| Layer | Target | Tool |
|-------|--------|------|
| Domain model | 100% | JaCoCo |
| Use case implementations | 90%+ | JaCoCo |
| Controllers | 80%+ | JaCoCo |
| Repository implementations | 70%+ | JaCoCo (integration tests) |

```xml
<!-- pom.xml — fail build if coverage drops below threshold -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.75</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

---

## Doc Template Sections

When generating `docs/testing.md`, include:
1. Test pyramid diagram with counts from actual `src/test/` (how many unit / integration / arch tests)
2. Example of each test type taken from the actual codebase (real class names)
3. ArchUnit rules currently enforced (from `ArchunitRuleTest.java`)
4. How to run tests: `./mvnw test`, specific test class, specific profile
5. Coverage report: how to generate and where to view (`target/site/jacoco/index.html`)
6. How to add a test for a new use case (step-by-step checklist)
7. Known gaps: test categories that are missing or under-covered
