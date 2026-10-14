package dev.controlparental.child.boot

import dev.controlparental.child.lock.ArchivoSnapshotLocal
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.util.UUID

internal data class RegistroPiloto(val enrolamiento: String, val armado: Boolean, val boot: Int?,
    val elapsed: Long, val generacion: Long)
internal class FalloPiloto : RuntimeException()
internal fun exigirPiloto(ok: Boolean) { if (!ok) throw FalloPiloto() }
internal object CodecPiloto {
    fun codificar(r: RegistroPiloto): ByteArray {
        exigirPiloto(UUID.fromString(r.enrolamiento).toString() == r.enrolamiento &&
            r.elapsed >= 0 && r.generacion > 0 && (r.boot == null || r.boot >= 0))
        val b = ByteArrayOutputStream().also { out -> DataOutputStream(out).use {
            it.writeInt(1); it.writeUTF(r.enrolamiento); it.writeBoolean(r.armado)
            it.writeInt(r.boot ?: -1); it.writeLong(r.elapsed); it.writeLong(r.generacion)
        } }.toByteArray()
        return b + MessageDigest.getInstance("SHA-256").digest(b)
    }
    fun decodificar(b: ByteArray): RegistroPiloto {
        exigirPiloto(b.size in 64..128)
        val payload = b.copyOfRange(0, b.size - 32)
        exigirPiloto(MessageDigest.isEqual(b.copyOfRange(b.size - 32, b.size), MessageDigest.getInstance("SHA-256").digest(payload)))
        return DataInputStream(ByteArrayInputStream(payload)).use {
            exigirPiloto(it.readInt() == 1); val id = it.readUTF(); val armado = it.readUnsignedByte()
            exigirPiloto(armado in 0..1); val boot = it.readInt(); exigirPiloto(boot >= -1)
            val r = RegistroPiloto(id, armado == 1, boot.takeIf { n -> n >= 0 }, it.readLong(), it.readLong())
            exigirPiloto(it.available() == 0 && codificar(r).contentEquals(b)); r
        }
    }
}

/** DP separado de H04/CP1. Marca + registro detectan pérdida parcial; no defensa root/rollback. */
internal class AlmacenPiloto(private val marca: ArchivoSnapshotLocal, private val registro: ArchivoSnapshotLocal,
    private val fuente: FuenteArranque, private val crearId: () -> String = { UUID.randomUUID().toString() }) {
    fun leer(): RegistroPiloto? {
        val m = marca.leer(); val r = registro.leer()
        if (m == null && r == null) return null
        exigirPiloto(m != null && r != null)
        val s = CodecPiloto.decodificar(r!!)
        val ancla = CodecPiloto.decodificar(m!!)
        exigirPiloto(ancla.enrolamiento == s.enrolamiento && ancla.armado && ancla.generacion == 1L)
        return s
    }
    fun armar(autoridad: () -> Boolean): RegistroPiloto {
        exigirPiloto(autoridad() && leer() == null)
        val o = fuente.observar(); exigirPiloto(o.elapsedRealtimeMs >= 0 && o.systemBootCount != null && o.systemBootCount >= 0)
        val r = RegistroPiloto(crearId(), true, o.systemBootCount, o.elapsedRealtimeMs, 1)
        val b = CodecPiloto.codificar(r); exigirPiloto(autoridad())
        marca.escribir(b); registro.escribir(b); exigirPiloto(leer() == r); return r
    }
    fun observar(): RegistroPiloto? {
        val r = leer() ?: return null
        if (!r.armado) return r
        val o = fuente.observar(); exigirPiloto(o.elapsedRealtimeMs >= 0)
        val boot = o.systemBootCount?.takeIf { it >= 0 }
        val continuidad = boot != null && boot == r.boot && o.elapsedRealtimeMs >= r.elapsed
        val nuevo = r.copy(boot = boot, elapsed = o.elapsedRealtimeMs,
            generacion = if (continuidad) r.generacion else Math.addExact(r.generacion, 1))
        registro.escribir(CodecPiloto.codificar(nuevo)); exigirPiloto(leer() == nuevo); return nuevo
    }
    fun vigente(r: RegistroPiloto): Boolean = try {
        val actual = leer(); val o = fuente.observar()
        r.armado && actual?.armado == true && actual.enrolamiento == r.enrolamiento && actual.generacion == r.generacion &&
            o.systemBootCount?.takeIf { it >= 0 } == actual.boot && o.elapsedRealtimeMs >= actual.elapsed
    } catch (_: Exception) { false }
    fun desarmar(autoridad: () -> Boolean) {
        exigirPiloto(autoridad()); val r = leer() ?: throw FalloPiloto()
        registro.escribir(CodecPiloto.codificar(r.copy(armado = false, generacion = Math.addExact(r.generacion, 1))))
        exigirPiloto(leer()?.armado == false)
    }
    fun finalizar(restaurado: Boolean) {
        exigirPiloto(restaurado && leer()?.armado == false)
        registro.borrar(); marca.borrar(); exigirPiloto(leer() == null)
    }
}
