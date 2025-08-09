## Executive Summary
Hi Claude! Could you design a software architecture? If you need more information from me, ask me 1-2 key questions right away. If you think I should upload any documents that would help you do a better job, let me know. You can use the tools you have access to — like Google Drive, web search, etc. — if they’ll help you better accomplish this task. Do not use analysis tool. Please keep your responses friendly, brief and conversational. Please execute the task as soon as you can - an artifact would be great if it makes sense. If using an artifact, consider what kind of artifact (interactive, visual, checklist, etc.) might be most helpful for this specific task. Thanks for your help!

### Enterprise Backend application using clean architecture
I'm developing an enterprise backend application using Java 17 with Spring boot 3.3.4, and I need comprehensive clean architecture guidance to create an intuitive, maintainable, testable, and domain driven project architecture design 

### Project Context:
- Technology stack: Java 17 with Spring boot 3.3.4
- Using ArchUnit to test developers not against the clean architecture rules
- User volume: 10000 use counts per day

### Folder Structure
#### Presentation Layers
- folder `adapter.in.controller`  for all spring `Controllers
  - all controllers need to have suffix `Controller`
  - all controllers can not depend on another controller
  - `Input` for request
  - `Output` for response
  - if `Output` had nested data structure, the inner data object declaration will have suffix `Dto`
#### Infrastructure Layer (External Dependencies)
- folder `adapter.out.rest` for all external restful api dependencies
  - all classes in `adapter.out.rest` need to have suffix `Gateway`

### Application Layer (Use Cases & Services)
- folder `usecase.ports.in` for all use cases
  - all use case must have Interfaces and have a suffix `UseCase`
    - all use case interface have a member function called `execute`
    - the `execute` function takes a object as argument with a suffix `Input` and produces a return object with a suffix `Result`
    - `Input` is a marker interface
    - `Output` is a marker interface
    - `Result` implement `Output` interface and have `returnCode`, `errorMessage` and `data` data member
  - all use case implementation must implement a use case interface and have a suffix `UseCaseImpl`
- folder `usecase.ports.out.repository` for all interfaces that related to data base access
  - repository query will using `namedparameterjdbctemplate` or `JdbcClient`  to query data
  - repository command (insert, update, delete) will using `JpaRepository`
- folder `usecase.ports.out.gateway` for all interfaces that related to external api call
  - 

### Domain Layer
- folder `domain.model` for all domain objects

### Dependency Constraints
- not to use `@Autowired` in classes (exclude test classes), using constructor injection
- Domain Layer should not use any spring boot framework 
- Application layer will not have spring annotation like `@Service` , `@Component`

### Naming Conventions
- all controller must been named with suffix `Controller`
- all use case must been named with suffix `UseCase`
- all repository must been named with suffix `Repository`
- all gateway must been named with suffix `Gateway` 

### Testing Strategy
- all Controllers using `@WebMvcTest` 
- all UseCase using `@ExtendWith(MockitoExtension.class)`

Please provide practical, actionable advice that considers both clean code and clean architecture principles and the technical constraints of a Java SpringBoot application. I'm particularly interested in best practices and patterns that have proven successful in real enterprise environments.