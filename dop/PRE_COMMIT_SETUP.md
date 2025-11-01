# Pre-commit Configuration for Spring Boot Java Project

This document explains the refactored `.pre-commit-config.yaml` configuration tailored for the Spring Boot Java project.

## What's New

The configuration has been significantly enhanced from the basic setup to include comprehensive checks for Java development:

### 1. General Pre-commit Hooks (Updated)
- **trailing-whitespace**: Removes trailing whitespace
- **end-of-file-fixer**: Ensures files end with a newline
- **check-added-large-files**: Prevents committing large files (max 500KB)
- **check-case-conflict**: Prevents case conflicts in filenames
- **check-merge-conflict**: Detects merge conflict markers
- **check-xml**: Validates XML files (important for Maven pom.xml)
- **check-yaml**: Validates YAML files
- **check-json**: Validates JSON files
- **pretty-format-json**: Auto-formats JSON files
- **mixed-line-ending**: Ensures consistent line endings (LF)

### 2. Java Code Formatting
- **pretty-format-java**: Automatically formats Java code using Google Java Format with AOSP (Android Open Source Project) style

### 3. Maven-Specific Checks
- **maven-wrapper-permissions**: Ensures Maven wrapper (mvnw) is executable
- **maven-compile**: Validates that the project compiles successfully
- **maven-test**: Runs all tests before allowing commit

### 4. Security & Quality Checks (Manual Stages)
- **maven-dependency-check**: OWASP dependency vulnerability scanning
- **maven-checkstyle**: Code style enforcement (requires checkstyle plugin)
- **maven-spotbugs**: Static code analysis (requires spotbugs plugin)

## Installation & Setup

### 1. Install pre-commit
```bash
# macOS (using Homebrew)
brew install pre-commit

# Or using pip
pip install pre-commit
```

### 2. Install the hooks
```bash
# In your project directory
pre-commit install
```

### 3. Optional: Install additional Maven plugins
Add these plugins to your `pom.xml` to enable the optional checks:

#### Checkstyle Plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.0</version>
    <configuration>
        <configLocation>google_checks.xml</configLocation>
        <encoding>UTF-8</encoding>
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
    </configuration>
</plugin>
```

#### SpotBugs Plugin
```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.7.3.6</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Low</threshold>
        <failOnError>true</failOnError>
    </configuration>
</plugin>
```

#### OWASP Dependency Check Plugin
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.4.0</version>
    <configuration>
        <failBuildOnCVSS>7.0</failBuildOnCVSS>
    </configuration>
</plugin>
```

## Usage

### Automatic Execution
Pre-commit hooks run automatically when you commit:
```bash
git add .
git commit -m "Your commit message"
# Hooks will run automatically
```

### Manual Execution
Run hooks on all files:
```bash
pre-commit run --all-files
```

Run specific hook:
```bash
pre-commit run maven-test
```

Run manual-stage hooks (security/quality checks):
```bash
pre-commit run --hook-stage manual maven-dependency-check
pre-commit run --hook-stage manual maven-checkstyle
pre-commit run --hook-stage manual maven-spotbugs
```

### Skip Hooks (when needed)
```bash
# Skip all hooks
git commit -m "Emergency fix" --no-verify

# Skip specific hook
SKIP=maven-test git commit -m "WIP: tests not ready"
```

## Hook Behavior

### Fast Hooks (run on every commit)
- File format checks and fixes
- Java code formatting
- Maven wrapper permissions
- Compilation check
- Test execution

### Manual Hooks (run on demand)
- Security vulnerability scanning
- Code style analysis
- Static code analysis

## Troubleshooting

### Common Issues

1. **Maven wrapper not executable**
   ```bash
   chmod +x mvnw
   git add mvnw
   git commit -m "Fix mvnw permissions"
   ```

2. **Compilation failures**
   - Fix Java compilation errors before committing
   - Ensure all dependencies are available

3. **Test failures**
   - Fix failing tests before committing
   - Use `SKIP=maven-test` only for WIP commits

4. **Large files detected**
   - Use Git LFS for large files
   - Or adjust the size limit in the config

### Performance Tips

- The hooks are designed to be fast by only running on changed files
- Manual hooks (security/quality) are optional and run only when explicitly triggered
- Consider running manual hooks in CI/CD pipeline instead of pre-commit for better performance

## Benefits

This configuration provides:
- ✅ Consistent code formatting
- ✅ Early detection of compilation issues
- ✅ Prevention of broken commits
- ✅ Security vulnerability awareness
- ✅ Code quality enforcement
- ✅ Proper file handling and validation

The setup ensures that only high-quality, properly formatted, and tested code enters your repository.
