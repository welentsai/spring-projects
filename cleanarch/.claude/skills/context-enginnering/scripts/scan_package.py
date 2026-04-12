#!/usr/bin/env python3
"""
scan_package.py

用途：掃描指定 package 目錄，輸出所有 .java 原始碼供 Claude 萃取 canonical pattern。

使用方式：
    python scan_package.py <package-absolute-path>

範例：
    python scan_package.py /path/to/src/main/java/com/example/demo/adapter/out/gateway
    python scan_package.py C:\\workspace\\cleanarch\\src\\main\\java\\com\\example\\demo\\adapter\\in

輸出：
    - 每個 .java 檔案的完整內容，附帶分隔標記
    - 摘要（檔案數、class / interface / record 宣告清單）
    - 建議的 canonical example 檔案

後續步驟（Claude 執行）：
    1. 讀取本 script 的輸出
    2. 識別最具代表性的 class（Controller / XxxImpl / interface 等）
    3. 萃取結構骨架（不含實作細節）
    4. 依照 references/templates/package-claude-md.md 填入 §5.2 正確範例
    5. 產生 CLAUDE.md 並寫入 <package-absolute-path>/CLAUDE.md

相容性：Python 3.8+，Windows / Linux / macOS
"""

import re
import sys
from pathlib import Path

# ── Priority hints: files matching these patterns are preferred as canonical examples ──
CANONICAL_PRIORITY = [
    r"Controller\.java$",       # adapter/in  → RestController
    r"UseCaseImpl\.java$",      # usecase/ports/in/impl
    r"GatewayImpl\.java$",      # adapter/out/gateway
    r"RepositoryImpl\.java$",   # adapter/out/repository
    r"UseCase\.java$",          # usecase/ports/in  (interface)
    r"Gateway\.java$",          # usecase/ports/out/gateway (interface)
    r"Repository\.java$",       # usecase/ports/out/repository (interface)
    r"JpaEntity\.java$",        # usecase/ports/out/entity
    r"Result\.java$",           # usecase/ports/in  (Result subclass)
    r"Input\.java$",            # usecase/ports/in  (Input class)
]

# ── Regex to match a class/interface/record/enum declaration line ──
# Applied line-by-line (no MULTILINE needed) to avoid \s* crossing newlines.
DECL_LINE_PATTERN = re.compile(
    r"(public\s+|protected\s+|abstract\s+|final\s+|sealed\s+)*"
    r"(class|interface|record|enum|@interface)\s+\w+"
)

SEPARATOR_MAJOR = "=" * 64
SEPARATOR_MINOR = "-" * 64


def find_java_files(package_dir: Path) -> list[Path]:
    """Return sorted list of .java files directly in package_dir (non-recursive)."""
    return sorted(package_dir.glob("*.java"))


def extract_declaration(source: str) -> str:
    """Extract the first class/interface/record/enum declaration line.

    Iterates line-by-line so that whitespace matching never crosses newlines.
    """
    for line in source.splitlines():
        stripped = line.strip()
        if stripped and DECL_LINE_PATTERN.search(stripped):
            return stripped
    return "(unknown)"


def pick_canonical(files: list[Path]) -> Path:
    """Pick the most representative file based on CANONICAL_PRIORITY."""
    for pattern in CANONICAL_PRIORITY:
        for f in files:
            if re.search(pattern, f.name):
                return f
    return files[0]  # fallback: first file alphabetically


def print_files(files: list[Path]) -> None:
    for f in files:
        print(SEPARATOR_MINOR)
        print(f"FILE: {f.name}")
        print(SEPARATOR_MINOR)
        print(f.read_text(encoding="utf-8"))
        print()


def print_summary(package_dir: Path, files: list[Path]) -> None:
    canonical = pick_canonical(files)

    print(SEPARATOR_MAJOR)
    print("SUMMARY")
    print(SEPARATOR_MAJOR)
    print(f"Package : {package_dir}")
    print(f"Files   : {len(files)}")
    print()
    print("Class / Interface list:")
    for f in files:
        source = f.read_text(encoding="utf-8")
        decl = extract_declaration(source)
        marker = " ← canonical" if f == canonical else ""
        print(f"  - {f.stem}: {decl}{marker}")

    print()
    print(f"Suggested canonical example : {canonical.name}")
    print()
    print("Next step: Read the file content above and generate CLAUDE.md")
    print("Template : references/templates/package-claude-md.md")


def main() -> int:
    if len(sys.argv) < 2:
        print("Usage: python scan_package.py <package-absolute-path>", file=sys.stderr)
        print(
            "Example: python scan_package.py /path/to/adapter/out/gateway",
            file=sys.stderr,
        )
        return 1

    package_dir = Path(sys.argv[1])

    if not package_dir.exists():
        print(f"Error: path does not exist: {package_dir}", file=sys.stderr)
        return 1

    if not package_dir.is_dir():
        print(f"Error: not a directory: {package_dir}", file=sys.stderr)
        return 1

    files = find_java_files(package_dir)

    print(SEPARATOR_MAJOR)
    print(f"SCAN TARGET: {package_dir}")
    print(f"FILES FOUND: {len(files)}")
    print(SEPARATOR_MAJOR)
    print()

    if not files:
        print("(no .java files found — package may be empty or path is wrong)")
        return 0

    print_files(files)
    print_summary(package_dir, files)
    return 0


if __name__ == "__main__":
    sys.exit(main())
