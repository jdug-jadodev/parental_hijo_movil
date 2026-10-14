package dev.controlparental.child.network

import dev.controlparental.child.lock.*
import dev.controlparental.child.local.*
import org.junit.Assert.*
import org.junit.Test

class UsoTiempoTest {
    private class ArchivoPrueba : ArchivoSnapshotLocal {
        var bytes: ByteArray? = null; var falla = false
        override fun leer() = bytes
        override fun escribir(bytes: ByteArray) { if (!falla) this.bytes = bytes.copyOf() }
        override fun borrar() { bytes = null }
    }
    private var t = 100L; private var boot = 5; private var revision = 0L
    private val marca = ArchivoPrueba(); private val archivo = ArchivoPrueba()
    private val m = UsoRedLocal(marca, archivo, { t }, { boot })
    private fun iniciar() { assertTrue(m.preparar { true }); m.iniciarObservador(1); red(ValidacionRed.VALIDADA) }
    private fun red(v: ValidacionRed) = m.observar(v, 1, ++revision)
    private fun permitir(tipo: UsoLocal = UsoLocal.NORMAL, minutos: String = "1") {
        assertTrue(m.autorizar(tipo, MinutosUso.desdeTexto(minutos)!!) { true })
    }
    private fun vencer() { m.comprobarDeadline(m.referencia!!) }
    @Test fun `eleccion vacia decimal signo espacios y fuera de rango rechazados`() {
        for (s in listOf("", " ", "0", "1441", "-1", "+1", "1.0", "1,5", "1 ", " 1", "١", "999999999999999"))
            assertNull(s, MinutosUso.desdeTexto(s))
        assertEquals(1, MinutosUso.desdeTexto("1")!!.cantidad)
        assertEquals(1440, MinutosUso.desdeTexto("1440")!!.cantidad)
    }
    @Test fun `un minuto frontera59999 permite60000 revoca durable`() {
        iniciar(); permitir(); assertEquals(100L, m.inicioUso); assertEquals(60100L, m.vencimientoUso)
        t = 60099; vencer(); assertTrue(m.permiteUso)
        t++; vencer(); assertFalse(m.permiteUso); assertNull(m.proximoPlazo)
        assertEquals(MotivoRed.TIEMPO, CodecObligacionRed.decodificar(archivo.bytes!!).motivo)
    }
    @Test fun `1440min cuenta24h exactas en reloj falso sin promesa fisica`() {
        iniciar(); permitir(minutos = "1440"); assertEquals(86_400_100L, m.vencimientoUso)
        t = 86_400_099; vencer(); assertTrue(m.permiteUso)
        t++; vencer(); assertFalse(m.permiteUso)
    }
    @Test fun `overflow addExact nunca produce ALLOWED ni deadline negativo`() {
        iniciar(); t = Long.MAX_VALUE - 10
        assertFalse(m.autorizar(UsoLocal.NORMAL, MinutosUso.desdeTexto("1")!!) { true })
        assertFalse(m.permiteUso); assertNull(m.vencimientoUso)
        try { MinutosUso.desdeTexto("1440")!!.deadlineDesde(Long.MAX_VALUE); fail() } catch (_: ArithmeticException) {}
    }
    @Test fun `eventosduplicados no renuevan deadline original ni inicio`() {
        iniciar(); permitir(); val ref = m.referencia
        repeat(10) { t += 1000; red(ValidacionRed.VALIDADA) }
        assertEquals(100L, m.inicioUso); assertEquals(60100L, m.vencimientoUso); assertEquals(ref, m.referencia)
    }
    @Test fun `reconexion antesgracia conserva tiempo original`() {
        iniciar(); permitir(); t += 1000; red(ValidacionRed.NO_VALIDADA); t += 9999; red(ValidacionRed.VALIDADA)
        assertNull(m.deadline); assertEquals(60100L, m.vencimientoUso); assertTrue(m.permiteUso)
        t = 60100; vencer(); assertFalse(m.permiteUso)
    }
    @Test fun `red10s revoca antesdeltiempo y reconectar no restaura`() {
        iniciar(); permitir(minutos = "1440"); red(ValidacionRed.NO_VALIDADA); t += 10000
        vencer(); assertFalse(m.permiteUso); assertNull(m.vencimientoUso)
        assertEquals(MotivoRed.RED_10S, CodecObligacionRed.decodificar(archivo.bytes!!).motivo)
        red(ValidacionRed.VALIDADA); assertFalse(m.permiteUso)
    }
    @Test fun `tiempo vence primero aunque gracia tenga resto`() {
        iniciar(); permitir(); t = 55100; red(ValidacionRed.NO_VALIDADA)
        assertEquals(60100L, m.proximoPlazo); t = 60100; vencer()
        assertEquals(MotivoRed.TIEMPO, CodecObligacionRed.decodificar(archivo.bytes!!).motivo)
    }
    @Test fun `entrega tardia con ambos vencidos elige primera obligacion no doblegeneracion`() {
        iniciar(); permitir(); t = 1000; red(ValidacionRed.NO_VALIDADA)
        val ref = m.referencia!!; t = 100_000; m.comprobarDeadline(ref)
        val nuevo = m.referencia!!; assertEquals(ref.generacion + 1, nuevo.generacion)
        assertEquals(MotivoRed.RED_10S, CodecObligacionRed.decodificar(archivo.bytes!!).motivo)
        m.comprobarDeadline(ref); red(ValidacionRed.VALIDADA); assertEquals(nuevo, m.referencia)
    }
    @Test fun `empate tiempo y gracia revoca una vez como tiempo`() {
        iniciar(); permitir(); t = 50100; red(ValidacionRed.NO_VALIDADA); t = 60100; vencer()
        assertEquals(MotivoRed.TIEMPO, CodecObligacionRed.decodificar(archivo.bytes!!).motivo)
    }
    @Test fun `OFFLINE temporal no se revoca por red pero vence sin PIN`() {
        iniciar(); red(ValidacionRed.NO_VALIDADA); permitir(UsoLocal.EXCEPCION_OFFLINE)
        t += 30_000; red(ValidacionRed.NO_VALIDADA); assertNull(m.deadline); assertTrue(m.permiteUso)
        t = 60100; vencer(); assertFalse(m.permiteUso)
        assertEquals(MotivoRed.TIEMPO, CodecObligacionRed.decodificar(archivo.bytes!!).motivo)
    }
    @Test fun `red vuelve OFFLINE no se transforma ni renueva tiempo`() {
        iniciar(); red(ValidacionRed.NO_VALIDADA); permitir(UsoLocal.EXCEPCION_OFFLINE)
        t += 20_000; red(ValidacionRed.VALIDADA)
        assertEquals(UsoLocal.EXCEPCION_OFFLINE, m.uso); assertEquals(60100L, m.vencimientoUso)
    }
    @Test fun `libre reemplaza temporal callbackviejo no bloquea`() {
        iniciar(); permitir(); val vieja = m.referencia!!; t += 1000
        assertTrue(m.autorizar(UsoLocal.NORMAL) { true }); assertNull(m.vencimientoUso)
        t += 100_000; m.comprobarDeadline(vieja); assertTrue(m.permiteUso)
    }
    @Test fun `temporal reemplaza libre y temporal anterior con nueva referencia`() {
        iniciar(); m.autorizar(UsoLocal.NORMAL) { true }; permitir(); val vieja = m.referencia!!
        t += 1000; permitir(minutos = "2"); val nueva = m.referencia
        assertEquals(121100L, m.vencimientoUso); t = 60100; m.comprobarDeadline(vieja)
        assertTrue(m.permiteUso); assertEquals(nueva, m.referencia)
    }
    @Test fun `bloquear adulto cancela ambosplazos en NORMAL y OFFLINE`() {
        iniciar()
        for (tipo in listOf(UsoLocal.NORMAL, UsoLocal.EXCEPCION_OFFLINE)) {
            permitir(tipo); val ref = m.referencia!!; m.bloquear { true }
            assertFalse(m.permiteUso); assertNull(m.proximoPlazo); t += 100000
            m.comprobarDeadline(ref); assertFalse(m.permiteUso)
        }
    }
    @Test fun `fondo y caducidad login no cancelan permiso ya aceptado`() {
        iniciar(); val s = SesionAdulto({ t }, { boot }); s.reanudar()
        val entrada = s.solicitar(FinAdulto.ENTRAR)!!
        s.completar(entrada, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 7))
        val accion = s.solicitar(FinAdulto.USO_TIEMPO_NORMAL)!!
        assertTrue(s.autorizarAccion(accion, ResultadoAdulto(TipoAdulto.AUTENTICADO, revision = 7)))
        assertTrue(m.autorizar(UsoLocal.NORMAL, MinutosUso.desdeTexto("3")!!) { s.accionVigente(accion) })
        s.cerrar(); t += 120001; vencer(); assertTrue(m.permiteUso)
        t = 180100; vencer(); assertFalse(m.permiteUso)
    }
    @Test fun `boot y reloj regresivo invalidan temporal sin reconstruirlo`() {
        iniciar(); permitir(); boot++; vencer(); assertFalse(m.permiteUso); assertNull(m.vencimientoUso)
        permitir(UsoLocal.EXCEPCION_OFFLINE); t--; vencer(); assertFalse(m.permiteUso)
    }
    @Test fun `nuevo proceso nunca carga inicio ni temporal de DP`() {
        iniciar(); permitir(UsoLocal.EXCEPCION_OFFLINE)
        val nuevo = UsoRedLocal(marca, archivo, { t }, { boot }); assertTrue(nuevo.cargar())
        assertFalse(nuevo.permiteUso); assertNull(nuevo.inicioUso); assertNull(nuevo.vencimientoUso)
    }
    @Test fun `IO o corrupcion al vencer no devuelve uso ni borra para reparar`() {
        iniciar(); permitir(); archivo.falla = true; t = 60100; vencer()
        assertEquals(UsoLocal.ERROR, m.uso); assertFalse(m.permiteUso); assertNull(m.proximoPlazo)
        archivo.bytes = byteArrayOf(1); assertFalse(m.cargar()); assertFalse(m.preparar { true })
        assertArrayEquals(byteArrayOf(1), archivo.bytes)
    }
    @Test fun `Keyguard enforcement pendiente no reanuda ni renueva deadline tras unlock`() {
        iniciar(); permitir(); t = 60100; vencer(); val ref = m.referencia!!
        var keyguard = true; var modo = ModoLocal.NONE; var restores = 0
        val puerto = object : PuertoLockLocal {
            override fun preparar() {}
            override fun aplicar() {}
            override fun solicitarInicio() { if (keyguard) throw FalloLocal(CausaLocal.KEYGUARD); modo = ModoLocal.LOCKED }
            override fun modo() = modo
            override fun restaurar() { restores++ }
            override fun restaurado() = false
        }
        val transicion = TransicionLocal(puerto, { t }, { !m.permiteUso && m.referencia == ref }, conservarEnFallo = true)
        transicion.entrar(); assertEquals(CausaLocal.KEYGUARD, transicion.diagnostico.causa)
        assertEquals(FaseLocal.ERROR, transicion.diagnostico.fase); assertFalse(m.permiteUso); assertNull(m.vencimientoUso)
        keyguard = false; t += 30000; val g = transicion.entrar(); transicion.observar(g)
        assertEquals(FaseLocal.LOCKED, transicion.diagnostico.fase); assertEquals(0, restores)
        assertEquals(ref, m.referencia); assertFalse(m.permiteUso)
    }
}
