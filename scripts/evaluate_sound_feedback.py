#!/usr/bin/env python3
"""Summarize voluntary AURORA SENSE feedback from locally exported TXT files.

This does not estimate detection accuracy. It reports only user-reviewed
detections and user-declared misses. Audio is never read or transferred.
"""
import argparse
from collections import Counter
import json
from pathlib import Path
import re

MARK = re.compile(r"VALIDACIÓN MANUAL · (ACERTÓ|NO COINCIDE) · (.*?) · puntaje IA ([0-9]{1,3})/100")
MISS = re.compile(r"OMISIÓN MANUAL · (.*?) · informado por la persona que prueba")


def summarize_texts(texts):
    correct = Counter()
    incorrect = Counter()
    missed = Counter()
    files = 0
    for content in texts:
        files += 1
        for line in content.splitlines():
            m = MARK.search(line)
            if m:
                label = m.group(2).strip()
                score = int(m.group(3))
                if not label or score > 100:
                    continue
                (correct if m.group(1) == "ACERTÓ" else incorrect)[label] += 1
            else:
                m = MISS.search(line)
                if m:
                    missed[m.group(1).strip()] += 1
    return {
        "source_files": files,
        "reviewed_detections": sum(correct.values()) + sum(incorrect.values()),
        "marked_correct": sum(correct.values()),
        "marked_incorrect": sum(incorrect.values()),
        "reported_omissions": sum(missed.values()),
        "per_label": {
            label: {
                "marked_correct": correct[label],
                "marked_incorrect": incorrect[label],
            }
            for label in sorted(set(correct) | set(incorrect))
        },
        "reported_omissions_by_type": dict(sorted(missed.items())),
        "warning": (
            "NOT an accuracy, precision or recall estimate. Feedback is voluntary "
            "and unreviewed detections are not counted. Collect fully annotated "
            "audio trials and independently timed non-events to measure those metrics."
        ),
    }


def main():
    parser = argparse.ArgumentParser(description="AURORA: informe de validaciones manuales")
    parser.add_argument("paths", nargs="+", type=Path, help="Archivos TXT exportados por AURORA")
    parser.add_argument("--json", type=Path, help="Ruta opcional de salida JSON")
    args = parser.parse_args()
    contents = []
    for path in args.paths:
        if not path.is_file() or path.suffix.lower() != ".txt":
            parser.error(f"No se encuentra un TXT legible: {path}")
        if path.stat().st_size > 4_000_000:
            parser.error(f"Archivo demasiado grande: {path}")
        contents.append(path.read_text(encoding="utf-8"))
    result = summarize_texts(contents)
    output = json.dumps(result, indent=2, ensure_ascii=False)
    if args.json:
        args.json.write_text(output + "\n", encoding="utf-8")
    print(output)


if __name__ == "__main__":
    main()
