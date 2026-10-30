# 🎮 Manette Virtuelle

**Ton téléphone fait le Bluetooth de ton PC.**

Connecte ta manette en Bluetooth à ton téléphone : il la transmet à ton PC en Wi-Fi. Pas besoin de Bluetooth sur le PC, pas de câble.

🌐 **Site :** https://kizisofraise-max.github.io/manette-virtuelle/
⬇️ **Télécharger :** [dernière version pour Windows](https://github.com/kizisofraise-max/manette-virtuelle/releases/latest)

Gratuit · Windows 10 et 11 · Licence MIT

---

## Comment ça marche

1. **Installe sur ton PC.** Télécharge l'installateur et lance-le. Il installe aussi le pilote nécessaire pour que Windows reconnaisse la manette.
2. **Relie ta manette au téléphone.** Associe ta manette à ton téléphone en Bluetooth, comme d'habitude. Puis ouvre dans le navigateur du téléphone l'adresse affichée par le programme et tape le code à 4 chiffres.
3. **Joue sur ton PC.** Ton PC voit une manette de type Xbox 360. Elle fonctionne avec les jeux qui acceptent une manette.

## Fonctionnalités

- **📡 Ta manette Bluetooth sur PC** : boutons, sticks et gâchettes sont transmis au PC, palettes de dos comprises (tu choisis à quel bouton elles correspondent).
- **👆 Manette tactile en secours** : pas de manette sous la main ? Utilise l'écran du téléphone (boutons, croix directionnelle, sticks et gâchettes).
- **🔒 Protégé par un code** : un nouveau code est généré à chaque lancement. Personne d'autre sur ton réseau ne peut prendre le contrôle.
- **⚡ Faible latence** : la latence s'affiche en direct sur le téléphone.

## Questions fréquentes

**Windows affiche « Windows a protégé votre ordinateur ».**
C'est normal : le programme n'est pas signé par un éditeur payant. Clique sur « Informations complémentaires » puis « Exécuter quand même ». Le code source est public si tu veux vérifier ce que fait le programme.

**Mon antivirus signale le fichier.**
Les programmes créés avec Python déclenchent parfois de fausses alertes. Tu peux consulter le code source ici, ou ajouter une exception pour le dossier du programme.

**Le téléphone n'arrive pas à se connecter.**
Vérifie que le PC et le téléphone sont sur le même réseau Wi-Fi, et que tu as autorisé le programme sur les réseaux privés dans le pare-feu Windows.

**Pourquoi un pilote est-il installé ?**
Le pilote [ViGEmBus](https://github.com/nefarius/ViGEmBus) permet à Windows de voir une manette virtuelle. Sans lui, aucun jeu ne peut recevoir les commandes.

**Mon PC n'a pas de Bluetooth, ça marche ?**
Oui, c'est fait pour ça. Seul le téléphone a besoin du Bluetooth. Le PC reçoit tout par le réseau : il doit simplement être sur le même Wi-Fi (ou relié par câble au même réseau) que le téléphone.

**Quelles manettes sont compatibles ?**
Celles que ton téléphone reconnaît en Bluetooth et que son navigateur sait lire. Les manettes courantes de type Xbox ou PlayStation fonctionnent généralement, mais cela peut varier selon le téléphone.

**Ça marche sur Mac, Linux ou console ?**
Non, uniquement Windows pour l'instant. Le téléphone peut être un Android ou un iPhone, il suffit d'un navigateur.

## Pour les développeurs

Le programme est écrit en Python (`serveur_manette.py`) et utilise [vgamepad](https://github.com/yannbouteiller/vgamepad) et [websockets](https://github.com/python-websockets/websockets).

Pour construire l'exécutable et l'installateur :

1. Installe [Python](https://www.python.org/) et [Inno Setup](https://jrsoftware.org/isinfo.php).
2. Lance `build.bat` : il installe les dépendances et produit `dist\ManetteVirtuelle.exe`.
3. Place `ViGEmBus_Setup.exe` (installateur officiel de ViGEmBus) à côté de `installateur.iss`.
4. Compile `installateur.iss` avec Inno Setup : l'installateur est créé dans le dossier `Sortie`.

> 💡 Si tes fichiers sont dans un dossier synchronisé par OneDrive, la construction peut échouer avec « Accès refusé ». Travaille plutôt dans un dossier comme `C:\Manette`.

## Avis et problèmes

Une idée, un bug, un retour ? Ouvre un ticket dans l'onglet [Issues](https://github.com/kizisofraise-max/manette-virtuelle/issues).

## Licence

Projet distribué sous licence MIT. Voir le fichier `LICENCE`.

Projet indépendant, non affilié à Microsoft ni à Sony.
