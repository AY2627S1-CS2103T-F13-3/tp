# Original logo-click percussion

`logo-drum.wav` was created at Vincent Lin's direction with OpenAI Codex assistance.

## Source and provenance

The complete synthesis/composition source is
[`tools/generate_logo_drum.py`](../../../../tools/generate_logo_drum.py).
It uses only Python 3 standard-library sine oscillators, seeded pseudorandom
noise, pitch sweeps, amplitude envelopes and soft saturation. No external audio,
samples, extracted reference timings or transcription are inputs.

The 100 BPM pattern uses syncopated kicks, snares, hi-hats, tom/clap accents
and a B1–D2–A1 bass contour. The requested similarity is a general short, dry,
punchy drum/bass character, without covering or transcribing an existing recording.
It is computer-generated audio, not a claimed human performance.

From the repository root, reproduce with Python 3 and no extra dependencies:

```sh
python3 tools/generate_logo_drum.py
```

The script locates the repository relative to itself and writes this resource,
so it also works when invoked by absolute path from another working directory.

- Generation seed: `2510432`
- Duration: 3.0 seconds
- Format: Microsoft PCM WAV, stereo, 44,100 Hz, 16-bit
- Size: 529,244 bytes
- SHA-256: `8bae56b075e0a9d048cbf078d85c27666a3f8df58c78f6667bb5ee689727646f`

## Reuse permission

SPDX-License-Identifier: MIT

The generator and generated WAV are offered under the accompanying
[`LICENSE.txt`](LICENSE.txt), to the extent applicable rights exist. The full
MIT notice permits use, copying, modification, publication, distribution,
sublicensing and sale, including bundling in the public repository and JAR,
subject to retaining that notice. The notice is packaged alongside the WAV.
The repository's existing licence and contributors' credits remain intact.
No guarantee of copyrightability or exclusive rights in generic musical
building blocks is made; no rights in third-party recordings are granted.

## Playback

Playback uses the standard Java Sound API. The cue loads only after a primary
logo click; it does not autoplay or loop continuously.
