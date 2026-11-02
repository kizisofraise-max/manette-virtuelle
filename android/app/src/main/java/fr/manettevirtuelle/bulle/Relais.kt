package fr.manettevirtuelle.bulle

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
 *  1. on se connecte a ws://ADRESSE:8765
 *  2. on envoie {"auth":"1234"} -> le PC repond {"auth":true} ou {"auth":false,...}
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

    private val ecouteurs = CopyOnWriteArrayList<() -> Unit>()
    private val principal = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var ws: WebSocket? = null

    fun ajouterEcouteur(f: () -> Unit) {
        if (!ecouteurs.contains(f)) ecouteurs.add(f)
    }

    fun retirerEcouteur(f: () -> Unit) {
        ecouteurs.remove(f)
    }

    private fun maj(nouvelEtat: Int, texte: String) {
        etat = nouvelEtat
        message = texte
        principal.post {
            for (e in ecouteurs) e()
        }
    }

    /** Accepte aussi "192.168.1.113:8000" ou "http://192.168.1.113:8000" */
    fun nettoyerAdresse(brut: String): String {
        var a = brut.trim()
        a = a.removePrefix("http://").removePrefix("https://").removePrefix("ws://")
        a = a.substringBefore("/").substringBefore(":")
        return a
    }

    fun connecter(adresseBrute: String, code: String) {
        deconnecter()
        val adresse = nettoyerAdresse(adresseBrute)
        if (adresse.isEmpty()) {
            maj(ECHEC, "Entre l'adresse du PC (elle est affichée dans la fenêtre du programme).")
            return
        }
        if (code.length != 4) {
            maj(ECHEC, "Entre le code à 4 chiffres affiché sur le PC.")
            return
        }
        val requete = try {
            Request.Builder().url("ws://$adresse:8765").build()
        } catch (e: IllegalArgumentException) {
            maj(ECHEC, "Adresse invalide.")
            return
        }
        maj(CONNEXION, "Connexion en cours...")

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
                        maj(ECHEC, "Trop d'erreurs : attends " + j.optInt("verrou") + " secondes puis réessaie.")
                        webSocket.close(1000, null)
                    } else {
                        maj(ECHEC, "Code refusé. Vérifie le code affiché sur le PC (il change à chaque lancement).")
                        webSocket.close(1000, null)
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
                    maj(ECHEC, "Impossible de joindre le PC. Vérifie l'adresse, que le téléphone est sur le même Wi-Fi et que le programme est lancé.")
                }
            }
        })
    }

    fun deconnecter() {
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
