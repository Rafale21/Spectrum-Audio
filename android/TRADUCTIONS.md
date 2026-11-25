# Traductions de Spectrum Audio 1.18

Les textes sont embarqués dans `app/src/main/assets/languages.json`. La source française sert de clé stable. Les huit catalogues contiennent chacun les mêmes 149 messages : anglais, espagnol, allemand, italien, portugais, russe, japonais et klingon. La liste des langues et la sélection automatique sont définies dans `LanguageChoice.java` et `I18n.java`.

Le menu « Langue / Language » conserve volontairement son nom bilingue pour rester repérable après un changement involontaire. Les noms des langues sont affichés dans leur propre écriture. Le nom de l’application, les noms de codecs, les unités et les diagnostics techniques détaillés ne sont pas traduits. Les sélecteurs de fichiers et les autorisations du système suivent la langue d’Android, indépendamment de celle choisie dans l’application.

Les textes sont des propositions de traduction ; une relecture humaine est recommandée avant publication. Le klingon est une adaptation expérimentale, affichée sous le nom « tlhIngan Hol », avec des périphrases pour les termes techniques. Il n’est ni certifié ni approuvé par le Klingon Language Institute. Référence lexicale consultée : https://www.kli.org/about-klingon/new-klingon-words/w/ ; outil lexical de l’organisme communautaire : https://hol.kag.org/.

Pour compléter ou corriger une traduction, modifier la valeur de la clé française dans le catalogue concerné puis exécuter `checks/run.sh`. Aucun service de traduction ni connexion Internet n’est utilisé par l’application.
