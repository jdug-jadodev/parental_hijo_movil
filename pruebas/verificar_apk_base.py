"""Inspecciona APK debug/release sin instalar ni conectarse a un teléfono.

Solo requiere Python estándar. No prueba Device Owner, UI ni bloqueo real.
"""
from pathlib import Path
import re
from zipfile import ZipFile


RAIZ = Path(__file__).resolve().parents[1]
APK = RAIZ / "android/app-child/build/outputs/apk/debug/app-child-debug.apk"
RELEASE = RAIZ / "android/app-child/build/outputs/apk/release/app-child-release-unsigned.apk"


def verificar() -> None:
    if not APK.is_file():
        raise FileNotFoundError("Primero construir el APK con :app-child:assembleDebug.")
    if not RELEASE.is_file():
        raise FileNotFoundError("Construir también :app-child:assembleRelease para comprobar separación debug.")

    ejemplos = {ruta.name for ruta in (RAIZ / "compartido/ejemplos").glob("*.json")}
    ejemplos.add("vectores_crypto.json")
    clases_de_prueba = set()
    for modulo in ("app-child", "core-protocol"):
        for carpeta in ("test", "testDebug", "testRelease", "testFixtures", "androidTest"):
            fuentes = RAIZ / "android" / modulo / "src" / carpeta
            for fuente in fuentes.rglob("*.kt"):
                texto = fuente.read_text(encoding="utf-8")
                for nombre in re.findall(r"\b(?:class|object)\s+([A-Za-z][A-Za-z0-9_]*)", texto):
                    clases_de_prueba.add(nombre.encode("ascii"))
    with ZipFile(APK) as archivo:
        for nombre in archivo.namelist():
            if Path(nombre).name in ejemplos:
                raise AssertionError(f"Ejemplo de prueba incluido en el APK: {nombre}")
            if nombre.endswith(".dex"):
                contenido = archivo.read(nombre)
                for clase in clases_de_prueba:
                    if clase in contenido:
                        raise AssertionError(f"Clase de prueba incluida: {clase.decode()}")

    print("OK: APK sin ejemplos congelados ni clases declaradas del simulador o pruebas.")
    print("Esta inspección no certifica Android, emergencias ni control del tiempo.")

    marcadores = (b"ArranqueH04", b"Ldev/controlparental/child/boot/FormatoTrazaArranque;")
    for apk, debe_contener in ((APK, True), (RELEASE, False)):
        with ZipFile(apk) as archivo:
            dex = b"".join(archivo.read(nombre) for nombre in archivo.namelist() if nombre.endswith(".dex"))
        for marcador in marcadores:
            assert (marcador in dex) == debe_contener, f"Separación incorrecta de traza debug: {apk.name}"
    print("OK: traza técnica H04 presente en debug y ausente de release.")


if __name__ == "__main__":
    verificar()
