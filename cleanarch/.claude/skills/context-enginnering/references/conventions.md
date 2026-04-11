# Naming Conventions

> This file is populated after the first codebase scan.
> Claude should update this file based on patterns observed in the actual project.

---

## Package Structure (to be confirmed after scan)

```
{root-package}/
├── domain/
│   ├── model/          ← Entities, Value Objects
│   ├── service/        ← Domain Services
│   └── event/          ← Domain Events
├── usecase/
│   ├── port/
│   │   ├── in/         ← Input port interfaces (what use cases expose)
│   │   └── out/        ← Output port interfaces (what use cases need)
│   ├── interactor/     ← Use case implementations
│   └── dto/            ← Command / Query objects
└── adapter/
    ├── in/
    │   └── web/        ← REST Controllers
    └── out/
        └── persistence/ ← Repository implementations, JPA entities
```

---

## Naming Patterns (to be confirmed after scan)

| Concept | Pattern | Example |
|---------|---------|---------|
| Input Port | `{Feature}UseCase` | `RegisterUserUseCase` |
| Output Port | `{Entity}Repository` | `UserRepository` |
| Use Case Impl | `{Feature}Interactor` | `RegisterUserInteractor` |
| REST Controller | `{Feature}Controller` | `UserController` |
| Command DTO | `{Feature}Command` | `RegisterUserCommand` |
| Query DTO | `{Feature}Query` | `FindUserQuery` |
| Persistence Entity | `{Entity}Jpa` or `{Entity}Entity` | `UserJpaEntity` |
| Mapper | `{Entity}Mapper` | `UserMapper` |

---

## Observed Patterns

> Claude: populate this section after scanning the project.

- **Root package**: TBD
- **Naming style**: TBD
- **Exception handling**: TBD
- **Validation location**: TBD (domain / usecase / adapter)
- **Mapper strategy**: TBD (MapStruct / manual / ModelMapper)
- **Test naming**: TBD
