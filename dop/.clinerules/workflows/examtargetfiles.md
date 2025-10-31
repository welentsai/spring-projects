# Target File Analysis

## Analyze Each Modified File

For each modified file identified:

1. Read the current state of the file:
   ```xml
   <read_file>
   <path>path/to/modified/file</path>
   </read_file>
   ```

2. For Java files, also examine related files (tests, interfaces, etc.):
   ```xml
   <search_files>
   <path>src</path>
   <regex>class.*FileName|interface.*FileName</regex>
   <file_pattern>*.java</file_pattern>
   </search_files>
