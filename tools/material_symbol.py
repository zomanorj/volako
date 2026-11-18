#!/usr/bin/env python3
"""Telecharge une icone Material Symbols (style Rounded) et la convertit en vector drawable Android.

Usage : python3 tools/material_symbol.py smartphone payments
Les icones vont dans core/designsystem/src/main/res/drawable/ic_<nom>.xml
"""
import re
import sys
import urllib.request
from pathlib import Path

URL = ("https://raw.githubusercontent.com/google/material-design-icons/master/"
       "symbols/web/{n}/materialsymbolsrounded/{n}_24px.svg")
OUT = Path(__file__).resolve().parent.parent / "core/designsystem/src/main/res/drawable"

TEMPLATE = """<?xml version="1.0" encoding="utf-8"?>
<!-- Material Symbols Rounded : {name} (Apache 2.0) -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="960"
    android:viewportHeight="960">
    <group android:translateY="960">
        <path
            android:fillColor="@android:color/white"
            android:pathData="{path}" />
    </group>
</vector>
"""


def main(names):
    OUT.mkdir(parents=True, exist_ok=True)
    for name in names:
        svg = urllib.request.urlopen(URL.format(n=name)).read().decode()
        # le viewBox des symbols est "0 -960 960 960", d'ou le translateY
        path = re.search(r'<path d="([^"]+)"', svg).group(1)
        (OUT / f"ic_{name}.xml").write_text(TEMPLATE.format(name=name, path=path))
        print("ok", name)


if __name__ == "__main__":
    main(sys.argv[1:])
