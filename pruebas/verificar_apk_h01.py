"""Inspección local H01 de manifests compilados. No usa ADB ni instala APK."""
import os
from pathlib import Path
import re
import subprocess


RAIZ = Path(__file__).resolve().parents[1]
AAPT = Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk/build-tools/35.0.0/aapt.exe"
SALIDAS = RAIZ / "android/app-child/build/outputs/apk"


def arbol(apk: Path, recurso: str) -> str:
    if not apk.is_file():
        raise FileNotFoundError(f"Construir primero el APK: {apk.name}")
    return subprocess.run(
        [str(AAPT), "dump", "xmltree", str(apk), recurso],
        check=True, capture_output=True, encoding="utf-8",
    ).stdout


def verificar() -> None:
    for variante, nombre in (("debug", "app-child-debug.apk"), ("release", "app-child-release-unsigned.apk")):
        apk = SALIDAS / variante / nombre
        manifest = arbol(apk, "AndroidManifest.xml")
        if variante == "debug":
            assert re.search(r"android:testOnly[^\n]*0xffffffff", manifest), "Debug debe ser testOnly"
        else:
            assert "android:testOnly" not in manifest, "Release no debe declarar testOnly"
            assert "android:debuggable" not in manifest, "Release no debe ser depurable"
        for esperado in (
            "dev.controlparental.child.admin.ChildAdminReceiver",
            "dev.controlparental.child.ui.LauncherActivity",
            "dev.controlparental.child.ui.ActividadInicial",
            "android.permission.BIND_DEVICE_ADMIN", "android.app.device_admin",
            "android.app.action.DEVICE_ADMIN_ENABLED", "android.intent.category.HOME",
            "android.intent.category.DEFAULT", "android.intent.category.LAUNCHER",
            "android:lockTaskMode",
            "dev.controlparental.child.boot.BootReceiver", "android:directBootAware",
            "android.intent.action.LOCKED_BOOT_COMPLETED", "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.MY_PACKAGE_REPLACED",
        ):
            assert esperado in manifest, f"Declaración ausente: {esperado}"
        permisos = subprocess.run(
            [str(AAPT), "dump", "permissions", str(apk)],
            check=True, capture_output=True, encoding="utf-8",
        ).stdout
        usados = re.findall(r"uses-permission: name='([^']+)'", permisos)
        assert set(usados) == {"dev.controlparental.child.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                               "android.permission.RECEIVE_BOOT_COMPLETED"}, usados
        # AndroidX añade su permiso signature para receivers internos; no es monitoreo.
        recursos = subprocess.run(
            [str(AAPT), "dump", "--values", "resources", str(apk)],
            check=True, capture_output=True, encoding="utf-8",
        ).stdout
        ruta = re.search(r':xml/device_admin: t=[^\n]+\n\s+\(string8\) "([^"]+)"', recursos)
        assert ruta, "No se encuentra el recurso XML del administrador"
        # Release puede acortar rutas de recursos; no asumir el nombre fuente.
        politicas = arbol(apk, ruta.group(1))
        elementos = re.findall(r"E: ([a-z-]+)", politicas)
        assert elementos == ["device-admin", "uses-policies", "force-lock", "wipe-data"], elementos
        print(f"OK {variante}: DPC/HOME y políticas declarados; separación testOnly verificada.")
    print("Inspección local: NO demuestra Device Owner real, UI, emergencia ni quiosco.")


if __name__ == "__main__":
    verificar()
