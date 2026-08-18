#!/usr/bin/env python3
"""
PT-BR Localization Audit Script
Searches for Chinese characters in Android UI files.
"""

import os
import re
import sys
from pathlib import Path

# Chinese character ranges
CJK_PATTERN = re.compile(r'[\u4e00-\u9fff\u3400-\u4dbf\U00020000-\U0002a6df\U0002a700-\U0002b73f\U0002b740-\U0002b81f\U0002b820-\U0002ceaf\U0002ceb0-\U0002ebef\U00030000-\U0003134f]')

# Exceptions (allowed Chinese)
EXCEPTIONS = [
    'Qwen',  # Model name
    'Gemma',  # Model name
    'GGUF',  # File format
    'CLIP',  # Model name
    'YOLO',  # Model name
    'VLM',  # Model name
    'MNN',  # Runtime name
    'ONNX',  # Runtime name
    'TFLite',  # Runtime name
    'Harzva',  # Author name
    'MobileCore',  # Project name
    'TuiMa',  # Project name
    'ModelScope',  # Platform name
    'Hugging Face',  # Platform name
    'Oxford-Pets',  # Dataset name
    'CIFAR10',  # Dataset name
    'MNIST',  # Dataset name
    'SHA-256',  # Hash algorithm
    'GGUF',  # File format
    'llama.cpp',  # Runtime name
    'ONNX',  # Runtime name
    'TFLite',  # Runtime name
    'MNN',  # Runtime name
    'NPU',  # Hardware
    'GPU',  # Hardware
    'CPU',  # Hardware
    'NNAPI',  # Hardware
    'QNN',  # Hardware
]

def is_exception(text):
    """Check if text is an allowed exception."""
    for exc in EXCEPTIONS:
        if exc in text:
            return True
    return False

def scan_file(filepath, exceptions=None):
    """Scan a file for Chinese characters."""
    if exceptions is None:
        exceptions = []
    
    results = []
    try:
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            lines = f.readlines()
            
        for i, line in enumerate(lines, 1):
            if CJK_PATTERN.search(line):
                # Check if it's an exception
                if not is_exception(line):
                    results.append({
                        'file': filepath,
                        'line': i,
                        'content': line.strip()
                    })
    except Exception as e:
        print(f"Error reading {filepath}: {e}", file=sys.stderr)
    
    return results

def find_ui_files(root_dir):
    """Find all UI-related files."""
    ui_files = []
    
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith(('.kt', '.xml')):
                filepath = os.path.join(root, file)
                # Skip test files and build directories
                if '/test/' in filepath or '/androidTest/' in filepath:
                    continue
                if '/build/' in filepath:
                    continue
                ui_files.append(filepath)
    
    return ui_files

def main():
    """Main function."""
    # Get the Android app directory
    android_app_dir = Path(__file__).parent.parent.parent
    
    print("=" * 80)
    print("PT-BR Localization Audit Script")
    print("=" * 80)
    print()
    
    # Find UI files
    ui_files = find_ui_files(android_app_dir)
    
    print(f"Scanning {len(ui_files)} UI files...")
    print()
    
    all_results = []
    
    for filepath in ui_files:
        results = scan_file(filepath)
        all_results.extend(results)
    
    if not all_results:
        print("No Chinese characters found in UI files!")
        return
    
    print(f"Found {len(all_results)} Chinese characters in UI files:")
    print()
    
    # Group by file
    files_with_chinese = {}
    for result in all_results:
        filepath = result['file']
        if filepath not in files_with_chinese:
            files_with_chinese[filepath] = []
        files_with_chinese[filepath].append(result)
    
    for filepath, results in sorted(files_with_chinese.items()):
        print(f"File: {filepath}")
        print(f"  Found {len(results)} Chinese characters")
        for result in results[:5]:  # Show first 5
            print(f"    Line {result['line']}: {result['content'][:100]}...")
        if len(results) > 5:
            print(f"    ... and {len(results) - 5} more")
        print()
    
    print("=" * 80)
    print("Summary:")
    print(f"  Total files scanned: {len(ui_files)}")
    print(f"  Files with Chinese: {len(files_with_chinese)}")
    print(f"  Total Chinese characters: {len(all_results)}")
    print()
    
    # Write report
    report_path = android_app_dir / "docs" / "PTBR_LOCALIZATION_AUDIT.md"
    report_path.parent.mkdir(exist_ok=True)
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("# PT-BR Localization Audit Report\n\n")
        f.write(f"**Date:** {__import__('datetime').datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"## Summary\n\n")
        f.write(f"- Files scanned: {len(ui_files)}\n")
        f.write(f"- Files with Chinese: {len(files_with_chinese)}\n")
        f.write(f"- Total Chinese characters: {len(all_results)}\n\n")
        
        f.write("## Files with Chinese Characters\n\n")
        for filepath, results in sorted(files_with_chinese.items()):
            f.write(f"### {filepath}\n\n")
            f.write(f"Found {len(results)} Chinese characters:\n\n")
            for result in results:
                f.write(f"- **Line {result['line']}:** `{result['content'][:200]}`\n")
            f.write("\n")
        
        f.write("## Recommendations\n\n")
        f.write("1. Move remaining Chinese strings to strings.xml\n")
        f.write("2. Add @string references in Kotlin files\n")
        f.write("3. Ensure all UI strings are externalized\n")
        f.write("4. Run this script again to verify\n")
    
    print(f"Report saved to: {report_path}")

if __name__ == "__main__":
    main()
