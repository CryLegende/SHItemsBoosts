<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHItemsBoosts

*Ajoute des boosts d'items dans une arène.*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale-3aa655?style=for-the-badge" alt="SkyHand Opale">

</div>

---

## Présentation

Ajoute des boosts d'items dans une arène.

Utilisé sur : **Opale**.

## Commandes

Commande `/shitemsboosts` (alias `/shib`). Chaque sous-commande a sa permission, `shitemsboosts.admin` les donne toutes.

| Commande | Description | Permission |
|---|---|---|
| `/shitemsboosts add` | Ajoute un point d'apparition à votre position. | `shitemsboosts.admin.add` |
| `/shitemsboosts remove <id>` | Supprime un point d'apparition. | `shitemsboosts.admin.remove` |
| `/shitemsboosts list` | Liste les points (cliquable pour se téléporter ou supprimer). | `shitemsboosts.admin.list` |
| `/shitemsboosts tp <id>` | Vous téléporte à un point. | `shitemsboosts.admin.tp` |
| `/shitemsboosts forcecleanup [monde]` | Supprime les anciens items orphelins d'un monde. | `shitemsboosts.admin.forcecleanup` |
| `/shitemsboosts inspect` | Affiche les infos de l'item le plus proche. | `shitemsboosts.admin.inspect` |
| `/shitemsboosts reload` | Recharge `config.yml` et `messages.yml`, retire puis repose les boosts. | `shitemsboosts.admin.reload` |

`list`, `remove`, `reload` et `forcecleanup <monde>` marchent aussi depuis la console. `locations.yml` n'est pas relu par `reload` : il est réécrit à chaque ajout ou retrait.

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shitemsboosts.admin` | Toute l'administration (parent des permissions ci-dessous). | `op` |
| `shitemsboosts.admin.add` | `add`. | `op` |
| `shitemsboosts.admin.remove` | `remove`. | `op` |
| `shitemsboosts.admin.list` | `list`. | `op` |
| `shitemsboosts.admin.tp` | `tp`. | `op` |
| `shitemsboosts.admin.forcecleanup` | `forcecleanup`. | `op` |
| `shitemsboosts.admin.inspect` | `inspect`. | `op` |
| `shitemsboosts.admin.reload` | `reload`. | `op` |

## Configuration

Fichiers par défaut repris du serveur (secrets remplacés par `CHANGE_ME`). Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHItemsBoosts/
├── <a href="src/main/resources/config.yml">config.yml</a>
└── <a href="src/main/resources/messages.yml">messages.yml</a>
</pre>

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
