# Release history / Historique

## 1.21

- Keeps the playhead visible during playback; pausing or finishing starts the usual 1.5-second inactivity timeout.
- Pages the zoomed time view at the edge with a little previous context, retaining the zoom and adaptive detail.
- Adds **Follow playback automatically**, enabled by default, persistent, and translated into every UI language. Disable it in Settings and tap Apply to browse a fixed view.
- Does not auto-pan during a held graph gesture or while dragging the playback seek bar.
- Same application ID and signing key as 1.20: installs directly as an update.

### Français

- Curseur visible pendant toute la lecture ; délai de disparition de 1,5 seconde après la pause ou la fin.
- Déplacement de la vue par portions au bord du graphe, avec un peu de contexte précédent, sans modifier le zoom ni la définition adaptative.
- Option **Suivre automatiquement la lecture**, activée par défaut, mémorisée et traduite. Pour une vue fixe : désactiver dans les réglages puis Appliquer.
- Priorité aux gestes maintenus sur le graphe et à la recherche par la barre de lecture.
- Identifiant Android et clé de signature de la 1.20 conservés : mise à jour directe.

## 1.20

- New Android application ID: `studio.noads.spectrumaudio`. Full Java namespace, build and test tooling, documentation and release metadata updated.
- Retains the No Ads Studio signing key and the improved spectrogram rendering from 1.19.
- Installs separately from older releases. Private data is not migrated automatically.

### Français

- Nouvel identifiant Android `studio.noads.spectrumaudio`, appliqué au code, aux scripts, aux tests et aux documents.
- Clé dédiée No Ads Studio et améliorations du spectrogramme 1.19 conservées.
- Installation distincte des versions antérieures ; données privées non migrées automatiquement.

## 1.19

- Source publication under GPL-3.0-only; complete build instructions and tests. Official APK unchanged.
- Publication des sources sous GPL-3.0-only ; instructions et tests inclus. APK officiel inchangé.

First version prepared for public distribution on this GitHub page.

- Preserves temporal detail while zooming: adaptive FFT selection now considers visible time, instead of forcing a long window based only on image height.
- Better use of wide and tall screens within the same total pixel budget.
- Explicitly disables bitmap smoothing and aligns plot bounds to pixels.
- Reuses transforms for duplicate sample positions at deep zoom.
- Keeps existing settings, recording formats and the dedicated No Ads Studio signing identity.

### Français

Première version préparée pour la diffusion publique sur cette page GitHub.

- Détails temporels mieux conservés : la FFT adaptative tient compte de la durée visible et revient à la taille choisie au zoom rapproché.
- Meilleure utilisation des écrans larges et hauts, avec le même budget global de pixels.
- Désactivation explicite du lissage des bitmaps et alignement du tracé sur les pixels.
- Réutilisation du calcul lorsque plusieurs colonnes correspondent au même échantillon.
- Réglages, formats d’enregistrement et signature publique No Ads Studio conservés.

## 1.18 — preparation / préparation

- Optional Ko-fi support link / Lien de soutien Ko-fi facultatif.
- Default FFT 1024 / 16 kHz displayed limit, new 512 option / Réglages initiaux 1024 / 16 kHz, nouveau choix 512.
- Default OGG/Opus with WAV fallback / Format initial OGG/Opus avec secours WAV.
- Reduced allocations during audio processing / Réduction des allocations dans le traitement audio.
