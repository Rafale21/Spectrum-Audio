# Contrôle des encodeurs sur Android

Ce contrôle utilise les mêmes sources RecordingStore, RecordingFormat, WavFile et AudioBuffers que l’application. Il crée de l’audio synthétique ; aucune permission microphone n’est demandée. L’APK de test est distinct de Spectrum Audio et ne modifie pas ses fichiers.

Avec un téléphone ou émulateur Android 10+ connecté par ADB et les variables ANDROID_HOME/JAVA_HOME configurées :

```bash
bash checks/device/build.sh
adb install -r checks/device-build/CodecChecks.apk
adb shell am instrument -w studio.noads.spectrumaudio.checks/studio.noads.spectrumaudio.AudioSmoke
```

Le résultat doit contenir `PASS`. Sous Android 8–9, le test annonce `SKIP`. Il teste Opus dans OGG et AAC dans M4A, avec deux prises dont la longueur n’est pas alignée sur les paquets, puis redécode le fichier et contrôle durée, présence d’un signal et suppression du brouillon après réussite. Une tolérance de 100 ms couvre le délai et le bourrage du codec ; ce test ne mesure pas la fidélité psychoacoustique ni la qualité subjective. Les fichiers temporaires sont supprimés.

Le contrôle a été compilé mais n’a pas été exécuté dans l’environnement de livraison. Les tests exécutés sur ordinateur sont dans `checks/run.sh`.
