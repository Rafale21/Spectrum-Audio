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

"""Compare the actual Windows 1.1 C DSP against the Android Java DSP."""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
binary = root / 'checks/classes/desktop-parity'
subprocess.run(['cc', '-O2', str(root / 'checks/reference/desktop-parity.c'), '-lm', '-o', str(binary)], check=True)
c = subprocess.check_output([str(binary)], text=True).splitlines()
j = subprocess.check_output(['java', '-cp', str(root / 'checks/classes'), 'studio.noads.spectrumaudio.DesktopParity'], text=True).splitlines()
assert len(c) == len(j)
error = 0.0
for a, b in zip(c, j):
    aa, bb = a.split(), b.split()
    assert aa[:3] == bb[:3]
    delta = abs(float(aa[3]) - float(bb[3]))
    error = max(error, delta)
    assert delta < 1e-11, (aa[:3], delta)
print(f'PASS: {len(c)} bins match Windows 1.1 C reference at all five FFT sizes / three methods (max power error {error:.3g}).')
