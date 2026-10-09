"""Inspecciona el APK de C00 sin instalarlo ni conectarse a un teléfono.

Solo requiere Python estándar. No prueba Device Owner, UI ni bloqueo real.
"""
from pathlib import Path
from zipfile import ZipFile


RAIZ = Path(__file__).resolve().parents[1]
APK = RAIZ / "android/app-child/build/outputs/apk/debug/app-child-debug.apk"


def verificar() -> None:
    if not APK.is_file():
        raise FileNotFoundError("Primero construir el APK con :app-child:assembleDebug.")

    ejemplos = {ruta.name for ruta in (RAIZ / "compartido/ejemplos").glob("*.json")}
    clases_de_prueba = (
        b"SimuladorDeMensajes",
        b"PreparacionNucleoTest",
        b"DependenciaNucleoTest",
    )
    with ZipFile(APK) as archivo:
        for nombre in archivo.namelist():
            if Path(nombre).name in ejemplos:
                raise AssertionError(f"Ejemplo de prueba incluido en el APK: {nombre}")
            if nombre.endswith(".dex"):
                contenido = archivo.read(nombre)
                for clase in clases_de_prueba:
                    if clase in contenido:
                        raise AssertionError(f"Clase de prueba incluida: {clase.decode()}")

    print("OK: APK sin ejemplos congelados ni clases del simulador o pruebas de C00.")
    print("Esta inspección no certifica Android, emergencias ni control del tiempo.")


if __name__ == "__main__":
    verificar()
