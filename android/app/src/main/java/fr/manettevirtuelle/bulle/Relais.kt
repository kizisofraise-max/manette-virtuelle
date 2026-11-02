package fr.manettevirtuelle.bulle

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Connexion au programme Windows (meme protocole que la page web) :
 *  1. on trouve le PC sur le Wi-Fi (ou on utilise l'adresse tapee a la main)
 *  2. on se connecte a ws://ADRESSE:8765 et on envoie {"auth":"1234"}
 *  3. on envoie l'etat de la manette : {"b":[...17 boutons...],"a":[gx,gy,dx,dy],"lt":x,"rt":y}
 */
object Relais {
    const val DECONNECTE = 0
    const val CONNEXION = 1
    const val CONNECTE = 2
    const val ECHEC = 3

    @Volatile
    var etat = DECONNECTE

    @Volatile
    var message = "Pas encore connecté au PC."

    /** Vrai quand l'application (ou la manette tactile) est affichée : la bulle se cache alors. */
    @Volatile
    var appAuPremierPlan = true

    private val ecouteurs = CopyOnWriteArrayList<() -> Unit>()
    private val principal = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var ws: WebSocket? = null

    @Volatile
    private var recherche = 0
    private var candidats: List<String> = emptyList()
    private var position = 0
    private var codeCourant = ""

    fun ajouterEcouteur(f: () -> Unit) {
        if (!ecouteurs.contains(f)) ecouteurs.add(f)
    }

    fun retirerEcouteur(f: () -> Unit) {
        ecouteurs.remove(f)
    }

    fun notifier() {
        principal.post {
            for (e in ecouteurs) e()
        }
    }

    private fun maj(nouvelEtat: Int, texte: String) {
        etat = nouvelEtat
        message = texte
        notifier()
    }

    /** Accepte aussi "192.168.1.113:8000" ou "http://192.168.1.113:8000" */
    fun nettoyerAdresse(brut: String): String {
        var a = brut.trim()
        a = a.removePrefix("http://").removePrefix("https://").removePrefix("ws://")
        a = a.substringBefore("/").substringBefore(":")
        return a
    }

    /** Connexion avec le seul code a 4 chiffres : le PC est cherche sur le Wi-Fi. */
    fun connecterAuto(contexte: Context, code: String) {
        deconnecter()
        if (code.length != 4) {
            maj(ECHEC, "Entre le code à 4 chiffres affiché sur le PC.")
            return
        }
        val appli = contexte.applicationContext
        recherche++
        val numero = recherche
        maj(CONNEXION, "Recherche du PC sur le Wi-Fi...")
        Thread {
            val trouves = Recherche.chercher(appli)
            if (numero == recherche) {
                if (trouves.isEmpty()) {
                    maj(
                        ECHEC,
                        "Aucun PC trouvé sur ce Wi-Fi. Vérifie que le programme est lancé sur le PC, " +
                            "et que le téléphone et le PC sont sur le même Wi-Fi. " +
                            "Sinon, saisis l'adresse à la main."
                    )
                } else {
                    candidats = trouves
                    position = 0
                    codeCourant = code
                    ouvrir(trouves[0], code)
                }
            }
        }.start()
    }

    /** Connexion avec une adresse tapee a la main (si la recherche ne trouve pas le PC). */
    fun connecter(adresseBrute: String, code: String) {
        deconnecter()
        val adresse = nettoyerAdresse(adresseBrute)
        if (adresse.isEmpty()) {
            maj(ECHEC, "Entre l'adresse du PC.")
            return
        }
        if (code.length != 4) {
            maj(ECHEC, "Entre le code à 4 chiffres affiché sur le PC.")
            return
        }
        candidats = listOf(adresse)
        position = 0
        codeCourant = code
        ouvrir(adresse, code)
    }

    private fun ouvrir(adresse: String, code: String) {
        val requete = try {
            Request.Builder().url("ws://$adresse:8765").build()
        } catch (e: IllegalArgumentException) {
            maj(ECHEC, "Adresse invalide.")
            return
        }
        maj(CONNEXION, "PC trouvé ($adresse), vérification du code...")

        ws = client.newWebSocket(requete, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONObject().put("auth", code).toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (webSocket !== ws) return
                try {
                    val j = JSONObject(text)
                    if (!j.has("auth")) return
                    if (j.optBoolean("auth", false)) {
                        maj(CONNECTE, "Connecté au PC. Tu peux jouer !")
                    } else if (j.has("verrou")) {
                        webSocket.close(1000, null)
                        maj(ECHEC, "Trop d'erreurs : attends " + j.optInt("verrou") + " secondes puis réessaie.")
                    } else {
                        webSocket.close(1000, null)
                        if (position + 1 < candidats.size) {
                            position++
                            ouvrir(candidats[position], codeCourant)
                        } else {
                            maj(ECHEC, "Code refusé. Vérifie le code affiché sur le PC (il change à chaque lancement).")
                        }
                    }
                } catch (e: Exception) {
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (webSocket !== ws) return
                if (etat == CONNECTE) maj(DECONNECTE, "Déconnecté du PC.")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (webSocket !== ws) return
                if (etat == CONNECTE) {
                    maj(ECHEC, "Connexion perdue avec le PC. Reconnecte-toi.")
                } else if (etat == CONNEXION) {
                    maj(ECHEC, "Impossible de joindre le PC. Vérifie que le programme est lancé et que le téléphone est sur le même Wi-Fi.")
                }
            }
        })
    }

    fun deconnecter() {
        recherche++
        val ancien = ws
        ws = null
        ancien?.close(1000, null)
        if (etat != DECONNECTE) maj(DECONNECTE, "Pas connecté au PC.")
    }

    fun envoyer(texte: String) {
        if (etat == CONNECTE) {
            ws?.send(texte)
        }
    }
}
