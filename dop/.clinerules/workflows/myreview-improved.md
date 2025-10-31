# Comprehensive Code Review Workflow (Improved)

You are an expert Senior Java Developer conducting a comprehensive code review comparing the current branch with the master branch in this Spring Boot 3.x and Java 17 project.

## Workflow Overview

This workflow provides a step-by-step, executable process for conducting thorough code reviews. Each step includes specific tool calls, progress tracking, and validation criteria.

---

## Phase 1: Branch Analysis & Discovery

### Step 1: Identify Current Branch and Changes

**Objective**: Discover what has changed and establish review scope

**Actions**:
1. Execute git commands to identify changes:
   ```bash
   git branch --show-current
   git diff master...HEAD --name-only
   git log master..HEAD --oneline --no-merges
   git status --porcelain
   ```
   - if master branch not exist, try using main branch
   - if main branch not exist, ask user to input the base branch name 

2. Analyze the project structure if not familiar:
   ```xml
   <list_code_definition_names>
   <path>src/main/java</path>
   <task_progress>
   - [x] Identify branch changes
   - [ ] Analyze modified files
   - [ ] Review architecture changes
   - [ ] Assess code quality
   - [ ] Check test coverage
   - [ ] Generate final report
   </task_progress>
   </list_code_definition_names>
   ```

**Success Criteria**: 
- Current branch identified
- List of changed files obtained
- Commit history reviewed
- No uncommitted changes blocking review

---

## Phase 2: File-by-File Analysis

### Step 2: Examine Each Modified File

**For each modified file identified in Step 1**:

1. Read the current state of the file:
   ```xml
   <read_file>
   <path>[modified-file-path]</path>
   <task_progress>
   - [x] Identify branch changes  
   - [x] Analyze modified files ([current-file])
   - [ ] Review architecture changes
   - [ ] Assess code quality
   - [ ] Check test coverage
   - [ ] Generate final report
   </task_progress>
   </read_file>
   ```

2. For Java classes, find related files (tests, interfaces, implementations):
   ```xml
   <search_files>
   <path>src</path>
   <regex>[ClassName](Test|Impl|Interface)?\.java</regex>
   <file_pattern>*.java</file_pattern>
   </search_files>
   ```

3. Search for usage of modified classes across the codebase:
   ```xml
   <search_files>
   <path>src</path>
   <regex>[ClassName]</regex>
   <file_pattern>*.java</file_pattern>
   </search_files>
   ```

**Validation**: Each modified file has been read and its relationships understood.

---

## Phase 3: Architecture & Spring Boot Analysis

### Step 3: Spring Boot Specific Review

**For Controller classes** (`@RestController`, `@Controller`):
- Check HTTP method mappings
- Validate input validation with `@Valid`
- Review exception handling
- Assess response entity usage
- Verify path variable and request param usage

**For Service classes** (`@Service`):
- Review transaction management (`@Transactional`)
- Check error handling patterns
- Validate business logic separation
- Assess dependency injection patterns

**For Repository classes** (`@Repository`, JPA repositories):
- Review query optimization
- Check JPA annotations usage
- Validate custom queries

**For Configuration classes** (`@Configuration`):
- Review bean definitions
- Check property bindings
- Assess security configurations

**Tool Usage**:
```xml
<search_files>
<path>src/main/java</path>
<regex>@(RestController|Controller|Service|Repository|Configuration)</regex>
<file_pattern>*.java</file_pattern>
<task_progress>
- [x] Identify branch changes  
- [x] Analyze modified files
- [x] Review architecture changes
- [ ] Assess code quality
- [ ] Check test coverage
- [ ] Generate final report
</task_progress>
</search_files>
```

---

## Phase 4: Code Quality Assessment

### Step 4: Java Code Quality Analysis

**Systematic checks for each file**:

1. **Naming Conventions**:
   - Classes: PascalCase
   - Methods: camelCase
   - Variables: camelCase, descriptive
   - Constants: UPPER_SNAKE_CASE

2. **Code Structure**:
   - Method length (ideally < 20 lines)
   - Class responsibilities (SRP)
   - Import statements (no wildcards)

3. **Error Handling**:
   - Proper exception handling
   - Resource cleanup (try-with-resources)
   - Appropriate logging levels

4. **Performance & Security**:
   - Stream API efficiency
   - SQL injection prevention
   - Input sanitization
   - Memory usage patterns

**Tool Usage**:
```xml
<search_files>
<path>src/main/java</path>
<regex>(class|interface).*\{</regex>
<file_pattern>*.java</file_pattern>
</search_files>
```

---

## Phase 5: Test Coverage Verification

### Step 5: Test Coverage Analysis

**For each modified production class**:

1. Find corresponding test files:
   ```xml
   <search_files>
   <path>src/test/java</path>
   <regex>[ClassName](Test|TestCase)</regex>
   <file_pattern>*.java</file_pattern>
   <task_progress>
   - [x] Identify branch changes  
   - [x] Analyze modified files
   - [x] Review architecture changes
   - [x] Assess code quality
   - [x] Check test coverage
   - [ ] Generate final report
   </task_progress>
   </search_files>
   ```

2. Analyze test quality:
   - Unit tests exist and comprehensive
   - Integration tests where appropriate
   - Mock usage is correct
   - Test naming follows conventions
   - Edge cases are covered

**Validation**: Every modified production class has adequate test coverage.

---

## Phase 6: Build Configuration Review

### Step 6: Maven/Build Analysis

**If pom.xml was modified**:

1. Read and analyze pom.xml:
   ```xml
   <read_file>
   <path>pom.xml</path>
   </read_file>
   ```

2. Check for:
   - Appropriate dependency versions
   - No dependency conflicts
   - Security vulnerabilities
   - Proper dependency scopes
   - Version consistency

---

## Phase 7: Conflict & Merge Analysis

### Step 7: Merge Readiness Assessment

**Execute conflict detection**:
```bash
# Check for potential merge conflicts
git merge-tree $(git merge-base master HEAD) master HEAD | grep -E "<<<<<<< |======= |>>>>>>> " | wc -l

# Check branch status
git merge-base --is-ancestor master HEAD && echo "ahead" || echo "diverged"

# Preview merge (dry run)
git merge --no-commit --no-ff master && git merge --abort
```

---

## Phase 8: Final Report Generation

### Step 8: Comprehensive Review Report (using traditional chinese)

**Generate structured report using this template**:

```markdown
# Branch Comparison Code Review Report

## Executive Summary
- **Branch**: [branch-name]
- **Files Modified**: [count] files
- **Commits**: [count] commits ahead of master
- **Overall Status**: [Ready/Needs Work/Conflicts Detected]

## Merge Readiness Assessment
**Status**: [Ready/Needs Work/Conflicts Detected]
- Merge conflicts: [Yes/No - details]
- Missing tests: [Yes/No - which files]
- Breaking changes: [Yes/No - impact assessment]

## Code Quality Score
**Overall Rating**: [Excellent/Good/Fair/Needs Improvement]

## Issues Found

### Critical Issues 🚨 (Must Fix Before Merge)
- [List critical issues with file names and line numbers]

### Major Issues ⚠️ (Should Address)
- [List major issues with specific recommendations]

### Minor Issues ℹ️ (Code Quality Improvements)
- [List minor improvements for better maintainability]

## File-by-File Analysis

### [FileName.java]
**Purpose**: [Brief description of changes]
**Issues**:
1. [Specific issue with line numbers]
2. [Recommendations for improvement]

**Positive Aspects**:
- [What was done well]

## Spring Boot Specific Feedback
- [Framework-specific observations]
- [Architecture compliance assessment]
- [Security considerations]

## Test Coverage Assessment
- **Coverage Status**: [Adequate/Insufficient]
- **Missing Tests**: [List classes needing tests]
- **Test Quality**: [Assessment of existing tests]

## Recommendations & Next Steps

### Before Merge (Priority Order):
1. [Critical fixes required]
2. [Major improvements recommended]
3. [Documentation updates needed]

### Post-Merge Considerations:
- [Future improvements]
- [Technical debt items]
- [Monitoring recommendations]

## Merge Decision
**Recommendation**: [Approve/Request Changes/Major Revision Needed]
**Reasoning**: [Detailed justification]
```

**Use this tool to complete the workflow**:
```xml
<attempt_completion>
<result>Comprehensive code review completed for branch [branch-name]. Generated detailed analysis covering [X] modified files, architecture compliance, code quality, test coverage, and merge readiness. Review report provides specific recommendations categorized by priority.</result>
<task_progress>
- [x] Identify branch changes  
- [x] Analyze modified files
- [x] Review architecture changes
- [x] Assess code quality
- [x] Check test coverage
- [x] Generate final report
</task_progress>
</attempt_completion>
```

---

## Error Handling Guidelines

**If a step fails**:
1. Document the failure in the review report
2. Provide alternative approaches
3. Continue with remaining analysis
4. Flag the issue in final recommendations

**Common failure scenarios**:
- Git commands fail → Check repository state and permissions
- Files cannot be read → Verify file paths and permissions  
- No test files found → Flag as missing test coverage
- Merge conflicts detected → Provide specific conflict resolution steps

---

## Workflow Customization

**Adapt this workflow by**:
- Adding project-specific checks
- Modifying severity thresholds
- Including additional tools or quality gates
- Customizing report templates

**For different project types**:
- **Microservices**: Add API contract analysis
- **Data-heavy**: Include database migration review
- **Security-critical**: Enhanced security assessment
- **High-traffic**: Performance profiling focus

---

## Best Practices Applied

This improved workflow follows these Cline workflow best practices:

1. **Executable Steps**: Each step includes specific tool calls
2. **Progress Tracking**: Uses task_progress throughout
3. **Error Handling**: Includes failure scenarios and recovery
4. **Validation**: Clear success criteria for each phase
5. **Tool Integration**: Leverages Cline's capabilities effectively
6. **Modular Design**: Phases can be executed independently
7. **Completion Criteria**: Clear workflow termination
8. **Template-Based**: Reusable structure for different reviews
