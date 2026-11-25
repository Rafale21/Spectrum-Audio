# This file is part of Spectrum Audio.
# Copyright (C) 2026 No Ads Studio and contributors.
# SPDX-License-Identifier: GPL-3.0-only
#
# Spectrum Audio is free software: you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by the
# Free Software Foundation, version 3 only.
# Spectrum Audio is distributed WITHOUT ANY WARRANTY; without even the
# implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU General Public License in the repository root LICENSE file.

"""Check actual UI call sites against every shipped UTF-8 translation catalog."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
catalog = json.loads((root / 'app/src/main/assets/languages.json').read_text())
expected = {'en', 'es', 'de', 'it', 'pt', 'ru', 'ja', 'tlh'}
assert set(catalog) == expected
keys = set(catalog['en'])
assert len(keys) >= 130
for language, strings in catalog.items():
    assert set(strings) == keys, language
    assert all(isinstance(v, str) and v.strip() and '\ufffd' not in v for v in strings.values()), language
sources = root / 'app/src/main/java/studio/noads/spectrumaudio'
for name in ('MainActivity.java', 'SpectrogramView.java', 'I18n.java'):
    source = (sources / name).read_text()
    for literal in re.findall(r'\bt\(("(?:\\.|[^"\\])*")\)', source):
        assert json.loads(literal) in keys, (name, literal)
for label in ('Auto', 'Paysage forcé', 'Portrait forcé'):
    assert label in keys
for code in expected:
    assert 'Formats : M4A/AAC' in next(k for k in catalog[code] if k.startswith('Formats :'))
print(f'PASS: {len(keys)} UI messages, {len(catalog)} complete UTF-8 catalogs plus French source; call-site and orientation coverage.')
