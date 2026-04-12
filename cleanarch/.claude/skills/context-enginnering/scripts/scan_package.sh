#!/usr/bin/env bash
# scan_package.sh
#
# 用途：掃描指定 package 目錄，輸出所有 .java 原始碼供 Claude 萃取 canonical pattern。
#
# 使用方式：
#   bash scan_package.sh <package-absolute-path>
#
# 範例：
#   bash scan_package.sh /path/to/src/main/java/com/example/demo/adapter/out/gateway
#
# 輸出：
#   - 每個 .java 檔案的完整內容，附帶分隔標記
#   - 最後輸出摘要（檔案數、class 名稱清單）
#
# 後續步驟（Claude 執行）：
#   1. 讀取本 script 的輸出
#   2. 識別最具代表性的 class（Controller / XxxImpl / interface 等）
#   3. 萃取結構骨架（不含實作細節）
#   4. 依照 references/templates/package-claude-md.md 填入 §5.2 正確範例
#   5. 產生 CLAUDE.md 並寫入 <package-absolute-path>/CLAUDE.md

set -euo pipefail

PACKAGE_DIR="${1:-}"

if [[ -z "$PACKAGE_DIR" || ! -d "$PACKAGE_DIR" ]]; then
    echo "Usage: $0 <package-absolute-path>"
    echo "Example: $0 /path/to/src/main/java/com/example/demo/adapter/in"
    exit 1
fi

JAVA_FILES=$(find "$PACKAGE_DIR" -maxdepth 1 -name "*.java" | sort)
FILE_COUNT=$(echo "$JAVA_FILES" | grep -c "\.java$" || true)

echo "================================================================"
echo "SCAN TARGET: $PACKAGE_DIR"
echo "FILES FOUND: $FILE_COUNT"
echo "================================================================"
echo ""

if [[ "$FILE_COUNT" -eq 0 ]]; then
    echo "(no .java files found — package may be empty or path is wrong)"
    exit 0
fi

# Output each file with clear separator
for f in $JAVA_FILES; do
    FILENAME=$(basename "$f")
    echo "----------------------------------------------------------------"
    echo "FILE: $FILENAME"
    echo "----------------------------------------------------------------"
    cat "$f"
    echo ""
done

echo "================================================================"
echo "SUMMARY"
echo "================================================================"
echo "Package : $PACKAGE_DIR"
echo "Files   : $FILE_COUNT"
echo ""
echo "Class / Interface list:"
for f in $JAVA_FILES; do
    FILENAME=$(basename "$f" .java)
    # Extract class/interface/record declaration line
    DECL=$(grep -m1 -E "^(public |abstract |final )*(class|interface|record|enum) " "$f" || echo "(unknown)")
    echo "  - $FILENAME: $DECL"
done

echo ""
echo "Suggested canonical example: $(echo "$JAVA_FILES" | head -1 | xargs basename)"
echo ""
echo "Next step: Read the file content above and generate CLAUDE.md"
echo "Template : references/templates/package-claude-md.md"
