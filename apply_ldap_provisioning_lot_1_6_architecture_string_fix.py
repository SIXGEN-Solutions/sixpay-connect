from pathlib import Path

ROOT = Path.cwd()
target = ROOT / "backend/administration/src/test/java/com/sixpay/administration/api/DirectoryUserSecurityBoundaryArchitectureTest.java"

if not target.is_file():
    raise RuntimeError(f"Required file not found: {target}")

text = target.read_text(encoding="utf-8")

# Locate the statement structurally, including the malformed literal spanning two physical lines.
start_marker = "        String combined = controller +"
end_marker = "+ exceptionHandler;"
start = text.find(start_marker)
if start < 0:
    raise RuntimeError("Start of combined-source expression not found")
end = text.find(end_marker, start)
if end < 0:
    raise RuntimeError("End of combined-source expression not found")
end += len(end_marker)

replacement = "        String combined = String.join(System.lineSeparator(), controller, exceptionHandler);"
text = text[:start] + replacement + text[end:]
target.write_text(text, encoding="utf-8")

print("Fixed:", target)
print("Source concatenation now uses String.join(System.lineSeparator(), ...).")
print("No production code, git command, worktree check, gate, commit or push was executed.")
