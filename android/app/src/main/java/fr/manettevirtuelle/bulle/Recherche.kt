package fr.manettevirtuelle.bulle

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Cherche, sur le meme reseau Wi-Fi que le telephone, les PC ou le programme est lance (port 8765). */
object Recherche {
    const val PORT = 8765

    fun chercher(contexte: Context): List<String> {
        val cm = contexte.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val reseau = cm.activeNetwork ?: return emptyList()
        val liens = cm.getLinkProperties(reseau) ?: return emptyList()

        var moi: Inet4Address? = null
        for (la in liens.linkAddresses) {
            val a = la.address
            if (a is Inet4Address && !a.isLoopbackAddress) {
                moi = a
                break
            }
        }
        val octets = moi?.address ?: return emptyList()
        val base = "" + (octets[0].toInt() and 0xFF) + "." + (octets[1].toInt() and 0xFF) + "." + (octets[2].toInt() and 0xFF)
        val numeroMoi = octets[3].toInt() and 0xFF

        val trouves = Collections.synchronizedList(ArrayList<String>())
        val pool = Executors.newFixedThreadPool(48)
        val fini = CountDownLatch(254)
        for (i in 1..254) {
            pool.execute {
                try {
                    if (i != numeroMoi) {
                        val s = Socket()
                        s.connect(InetSocketAddress("$base.$i", PORT), 600)
                        s.close()
                        trouves.add("$base.$i")
                    }
                } catch (e: Exception) {
                } finally {
                    fini.countDown()
                }
            }
        }
        fini.await(15, TimeUnit.SECONDS)
        pool.shutdownNow()
        return ArrayList(trouves).sorted()
    }
}
