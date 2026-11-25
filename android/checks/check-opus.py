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

import json
import subprocess
import tempfile
from pathlib import Path

# Real reference containers exercise the application's output header guard.
# This is not a test of an Android encoder; run checks/device for that.
with tempfile.TemporaryDirectory() as directory:
    output = Path(directory) / 'reference.ogg'
    subprocess.run(['ffmpeg', '-v', 'error', '-f', 'lavfi', '-i', 'sine=frequency=880:sample_rate=48000:duration=1.237', '-c:a', 'libopus', '-b:a', '128k', str(output)], check=True)
    probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_streams', '-of', 'json', str(output)]))
    assert probe['streams'][0]['codec_name'] == 'opus'
    subprocess.run(['java', '-cp', 'checks/classes', 'studio.noads.spectrumaudio.FormatCheck', str(output)], check=True)
    decoded = subprocess.check_output(['ffmpeg', '-v', 'error', '-i', str(output), '-f', 's16le', '-ac', '1', '-ar', '48000', '-'])
    assert abs(len(decoded) / 2 / 48000 - 1.237) < .001
    print('PASS: header guard accepts real Opus in OGG; reference decoder preserves non-frame-aligned duration.')
