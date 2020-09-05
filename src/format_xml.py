#!/usr/bin/env python3
import sys
import xml.dom.minidom
from pathlib import Path

try:
    if len(sys.argv) == 1:
        raise RuntimeError("syntax: format_xml.py <filename>")

    pom_file = Path(sys.argv[1]).resolve()
    if not pom_file.exists():
        raise RuntimeError("file not found: %s" % pom_file)

    dom = xml.dom.minidom.parseString(
        "".join([line.strip() for line in pom_file.read_text().splitlines()])
    )

    pom_file.write_text(dom.toprettyxml(indent="  ", newl="\n"))

except RuntimeError as e:
    print(e, file=sys.stderr)
    sys.exit(1)
