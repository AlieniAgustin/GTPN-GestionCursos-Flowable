#!/usr/bin/env python3
"""Deja el stack con una instancia parada en cada estado, para la defensa.

La idea es no tener que improvisar durante la presentación: cuando arranca, ya hay un
curso esperando en cada punto del proceso, así que cada escenario se muestra entrando a
la pantalla que corresponde en vez de recorrer el flujo entero en vivo.

Al terminar imprime qué quedó dónde y con qué usuario verlo.

Uso:
    python scripts/preparar_demo.py            # prepara los 11 escenarios
    python scripts/preparar_demo.py --limpiar  # cancela lo que haya antes de empezar

Requiere el stack levantado y el venv de `mineria/` (ver mineria/README.md).
"""

import argparse
import os
import sys
import time

import requests

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "mineria"))
from generar_casos import EMPRESAS, Motor, elaborar_propuesta  # noqa: E402

FECHA_LEJANA_DIAS = 60

# Escenario 1: minutos desde que corre el script hasta el inicio del curso. En 0 el curso
# arranca hoy, así que el temporizador "Fecha programada" ya está vencido cuando la demo
# llega ahí y dispara en unos 4 segundos. Si querés mostrarlo esperando, poné 2 o 3 y
# corré el script justo antes de presentar: la instancia queda parada en el temporizador
# hasta que se cumpla la hora acordada.
MINUTOS_HASTA_INICIO = 0


class Demo:
    def __init__(self, bff, motor, sesion):
        self.bff = bff
        self.motor = motor
        self.sesion = sesion
        self.preparados = []

    # ------------------------------------------------------------------ utilidades

    def nueva(self, titulo, empresa_idx=0):
        """Da de alta la solicitud y devuelve **su** instancia.

        Va por `POST /api/courses` en vez del formulario: el alta web redirige sin
        devolver el id, y mirar "el último curso del listado" se equivoca de instancia
        cuando dos altas caen juntas. El servicio que corre por detrás es el mismo.
        """
        import datetime
        nombre, email, telefono = EMPRESAS[empresa_idx % len(EMPRESAS)]
        hoy = datetime.date.today()

        respuesta = self.sesion.post(f"{self.bff}/api/courses", json={
            "contact": {"name": nombre, "email": email, "phone": telefono},
            "name": titulo,
            "description": f"Curso de demostración: {titulo}",
            "participantCount": 15,
            "possibleDates": [(hoy + datetime.timedelta(days=d)).isoformat() for d in (60, 75)],
        }, timeout=30)
        respuesta.raise_for_status()

        instancia = respuesta.json().get("processInstanceId")
        if not instancia:
            raise RuntimeError(f"el alta de '{titulo}' no devolvió instancia")
        return instancia

    def completar(self, instancia, clave, variables=None):
        tarea = self.motor.tarea(instancia, clave)
        if tarea:
            self.motor.completar(tarea, variables)
            return True
        return False

    def hasta_preparacion(self, instancia, fecha_dias=FECHA_LEJANA_DIAS, minutos=0):
        """Propuesta + aceptación: deja la instancia con las tres ramas abiertas.

        ``fecha_dias`` es la fecha de inicio que acuerda la empresa, contada en días desde
        hoy. Con ``minutos`` se acuerda en cambio una hora puntual dentro de hoy, para
        mostrar el temporizador esperando un rato corto en vez de hasta la fecha real.
        """
        import datetime
        if not elaborar_propuesta(self.motor, instancia, 150000):
            raise RuntimeError("la solicitud salió NO VIABLE: no hay cupo "
                               "(hay cinco cursos en ejecución). Probá con --limpiar")
        if minutos:
            fecha = (datetime.datetime.now().astimezone()
                     + datetime.timedelta(minutes=minutos)).replace(microsecond=0).isoformat()
        else:
            fecha = (datetime.date.today() + datetime.timedelta(days=fecha_dias)).isoformat()
        self.motor.mensaje(instancia, "Aceptación", {"fechaAcordada": fecha})
        time.sleep(0.6)

    def cerrar_preparacion(self, instancia, aula_fisica=False):
        """Completa las tres ramas para que el subproceso termine.

        Por defecto pide **aula virtual**: el pool de aulas físicas del web service
        simulado tiene tres, y los cursos que avanzan hasta el dictado la retienen hasta
        cerrar. Si todos los escenarios pidieran aula física se agotarían entre ellos y
        la preparación quedaría trabada en *Gestionar falta de aula* (escenario 7), que
        acá no es lo que queremos mostrar.
        """
        self.completar(instancia, "evaluarrecursos", {
            "requiereAula": aula_fisica,
            "requierePlataforma": not aula_fisica,
            "requiereEquipamiento": False})
        self.completar(instancia, "prepararmateriales")
        if not aula_fisica:
            self.completar(instancia, "solicitaraulavirtual")
        self.motor.mensaje(instancia, "Nómina de participantes")
        time.sleep(0.6)
        self.completar(instancia, "confeccionarlistadefinitiva")
        time.sleep(0.6)

    def anotar(self, escenario, donde, quien, instancia):
        self.preparados.append((escenario, donde, quien, instancia))

    # ------------------------------------------------------------------ escenarios

    def escenario_solicitud_nueva(self):
        i = self.nueva("Introduccion a BPMN", 0)
        self.anotar("9. Solicitud desde el sitio",
                    "bandeja: Registrar y Elaborar propuesta", "jefeadministrativo", i)

    def escenario_esperando_respuesta(self):
        i = self.nueva("Seguridad e Higiene", 1)
        elaborar_propuesta(self.motor, i, 120000)
        self.anotar("2/3/4. Respuesta de la empresa",
                    "app de comunicaciones: aceptar, rechazar o pedir modificaciones",
                    "(app comunicaciones)", i)

    def escenario_revisar_propuesta(self):
        i = self.nueva("Excel Avanzado", 2)
        elaborar_propuesta(self.motor, i, 110000)
        self.motor.mensaje(i, "Solicitud de modificaciones",
                           {"observaciones": "Reducir la carga horaria a 8 horas"})
        time.sleep(0.6)
        self.anotar("2. Modificaciones y nueva versión del PDF",
                    "bandeja: Revisar propuesta", "jefeadministrativo", i)

    def escenario_en_preparacion(self):
        i = self.nueva("Atencion al Cliente", 3)
        # Este es el curso del escenario 1, el único que se recorre hasta el final en vivo.
        # Se acuerda el inicio para hoy a propósito: cuando la demo llega al temporizador
        # "Fecha programada" ya está vencido y dispara en unos segundos, en vez de dejar la
        # instancia esperando la fecha real. Subir MINUTOS_HASTA_INICIO para verlo esperar.
        self.hasta_preparacion(i, fecha_dias=0, minutos=MINUTOS_HASTA_INICIO)
        # Se deja con las tres ramas abiertas: es lo que la empresa ve en su pantalla
        self.anotar("10. Acceso de la empresa y preparación en paralelo",
                    "sitio: mis cursos (empresa) — tres ramas a la vez", "la empresa", i)

    def escenario_pendiente_de_inicio(self):
        """No se usa por defecto: no entra en el límite de cinco cursos (ver main)."""
        i = self.nueva("Primeros Auxilios", 4)
        self.hasta_preparacion(i)
        self.cerrar_preparacion(i)
        self.anotar("Confirmado, pendiente de inicio",
                    "bandeja: Definir docente", "jefedocente", i)

    def escenario_en_desarrollo(self):
        i = self.nueva("Liderazgo de Equipos", 5)
        self.hasta_preparacion(i, fecha_dias=1)
        self.cerrar_preparacion(i)
        self.completar(i, "definirdocentequedesarrollaelcurso", {"docenteSeleccionado": "docente1"})
        time.sleep(0.6)
        self.motor.disparar_timer(i)          # llega la fecha de inicio
        time.sleep(0.8)
        self.anotar("Curso en desarrollo", "bandeja: Desarrollar curso", "docente1", i)

    def escenario_informe(self):
        i = self.nueva("Gestion del Tiempo", 0)
        self.hasta_preparacion(i, fecha_dias=1)
        self.cerrar_preparacion(i)
        self.completar(i, "definirdocentequedesarrollaelcurso", {"docenteSeleccionado": "docente2"})
        time.sleep(0.6)
        self.motor.disparar_timer(i)
        time.sleep(0.8)
        self.completar(i, "desarrollarcurso", {"cursoDesarrollado": True})
        time.sleep(0.6)
        self.anotar("Elaboración del informe",
                    "bandeja: Elaborar informe de resultados", "jefedocente", i)

    def escenario_envio_informe(self):
        i = self.nueva("Comunicacion Efectiva", 1)
        self.hasta_preparacion(i, fecha_dias=1)
        self.cerrar_preparacion(i)
        self.completar(i, "definirdocentequedesarrollaelcurso", {"docenteSeleccionado": "jefedocente"})
        time.sleep(0.6)
        self.motor.disparar_timer(i)
        time.sleep(0.8)
        self.completar(i, "desarrollarcurso", {"cursoDesarrollado": True})
        time.sleep(0.6)
        self.completar(i, "elaborarinformederesultados",
                       {"informe": "Asistieron 18 de 20 inscriptos. Todos aprobaron."})
        time.sleep(0.6)
        self.anotar("1. Cierre del curso con liberación de recursos",
                    "bandeja: Enviar informe y archivar", "jefeadministrativo", i)

    def escenario_cancelacion(self):
        i = self.nueva("Trabajo en Equipo", 2)
        self.hasta_preparacion(i)
        # Con aula física reservada, la compensación tiene algo que liberar
        self.completar(i, "evaluarrecursos", {
            "requiereAula": True, "requierePlataforma": False, "requiereEquipamiento": False})
        time.sleep(0.6)
        self.motor.mensaje(i, "Cancelación")
        time.sleep(0.8)
        self.anotar("6. Cancelación con compensación",
                    "bandeja: liberar recursos (compensación en curso)", "administrativo1", i)

    def escenario_sin_respuesta(self):
        i = self.nueva("Normativa Laboral", 3)
        elaborar_propuesta(self.motor, i, 90000)
        for _ in range(3):
            if not self.motor.disparar_timer(i):
                break
        self.anotar("4. Cierre por falta de respuesta",
                    "bandeja: Archivar solicitud (van 2 recordatorios)", "jefeadministrativo", i)

    def escenario_no_viable(self):
        i = self.nueva("Curso Excedente", 4)
        tarea = self.motor.tarea(i, "registraryelaborarpropuesta")
        if tarea:
            self.anotar("5. (no se pudo forzar: hay cupo libre)",
                        "bandeja: Registrar y Elaborar propuesta", "jefeadministrativo", i)
        else:
            self.anotar("5. Límite de cursos en ejecución",
                        "ya cerrada: la solicitud salió NO VIABLE", "—", i)


def limpiar(motor, url, auth):
    """Cierra las instancias en curso para empezar con el cupo libre.

    No alcanza con cancelar: la cancelación solo está disponible **durante la
    preparación** (§1.3). Las instancias que ya pasaron a dictado o informe hay que
    terminarlas completando sus tareas. Por eso el bucle: cancela lo que puede, completa
    todo lo pendiente y dispara los temporizadores, repitiendo hasta que no queden
    instancias ocupando cupo.
    """
    VARIABLES = {
        "evaluarrecursos": {"requiereAula": False, "requierePlataforma": True,
                            "requiereEquipamiento": False},
        "definirdocentequedesarrollaelcurso": {"docenteSeleccionado": "docente1"},
        "desarrollarcurso": {"cursoDesarrollado": True},
        "elaborarinformederesultados": {"informe": "Cierre automático de la demo anterior"},
        "enviarinformeaempresayarchivarexpediente": {"informeEnviado": True,
                                                     "expedienteArchivado": True},
        "confeccionarlistadefinitiva": {"listaDefinitiva": "Lista confirmada"},
    }

    for vuelta in range(14):
        activas = requests.get(f"{url}/process-api/runtime/process-instances",
                               params={"size": 200}, auth=auth, timeout=30).json()["data"]
        ocupadas = [p["id"] for p in activas if _ocupa_cupo(url, auth, p["id"])]
        if not ocupadas:
            print(f"  cupo libre ({vuelta} vueltas)\n")
            return

        for instancia in ocupadas:
            motor.mensaje(instancia, "Cancelación")

        tareas = requests.get(f"{url}/process-api/runtime/tasks",
                              params={"size": 200}, auth=auth, timeout=30).json()["data"]
        for t in tareas:
            if not t["assignee"]:
                requests.post(f"{url}/process-api/runtime/tasks/{t['id']}",
                              json={"action": "claim", "assignee": "administrativo1"},
                              auth=auth, timeout=30)
            payload = {"action": "complete"}
            variables = VARIABLES.get(t["taskDefinitionKey"])
            if variables:
                payload["variables"] = [{"name": k, "value": v} for k, v in variables.items()]
            requests.post(f"{url}/process-api/runtime/tasks/{t['id']}",
                          json=payload, auth=auth, timeout=30)

        for instancia in ocupadas:
            motor.disparar_timer(instancia, intentos=1, espera=0.1)

        time.sleep(1.2)

    print("  AVISO: quedaron instancias ocupando cupo; algunos escenarios pueden "
          "salir no viables\n", file=sys.stderr)


def _ocupa_cupo(url, auth, instancia):
    """Una instancia cuenta para el límite de §3.1 si ya pasó a preparación."""
    variables = requests.get(
        f"{url}/process-api/runtime/process-instances/{instancia}/variables",
        auth=auth, timeout=30).json()
    return any(v["name"] == "cursoActivo" and v["value"] for v in variables)


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--bff", default="http://localhost:8081")
    parser.add_argument("--url", default="http://localhost:8080")
    parser.add_argument("--limpiar", action="store_true",
                        help="cancelar las instancias en curso antes de preparar")
    args = parser.parse_args()

    sesion = requests.Session()
    login = sesion.post(f"{args.bff}/login",
                        data={"username": "jefeadministrativo", "password": "password"},
                        timeout=30, allow_redirects=False)
    if login.status_code not in (301, 302):
        print("ERROR: no se pudo entrar al sitio. ¿Está levantado el stack?", file=sys.stderr)
        return 1

    motor = Motor(args.url, ("admin", "test"))
    if args.limpiar:
        limpiar(motor, args.url, ("admin", "test"))

    demo = Demo(args.bff, motor, sesion)

    # El orden importa por el límite de §3.1: solo cinco cursos pueden estar en
    # ejecución a la vez, así que los escenarios que quedan parados después de la
    # aceptación entran justo. Primero van los de negociación, que no ocupan cupo, y
    # la no viable va última, cuando el límite ya está completo.
    #
    # "Confirmado, pendiente de inicio" quedó fuera a propósito: es el sexto estado
    # post-aceptación y no entra. Se llega a él desde el curso en preparación,
    # completando sus tres ramas en vivo, que además es lindo de mostrar.
    pasos = [
        demo.escenario_solicitud_nueva,      # 9
        demo.escenario_esperando_respuesta,  # 2, 3 y 4 desde la app de comunicaciones
        demo.escenario_revisar_propuesta,    # 2
        demo.escenario_sin_respuesta,        # 4
        demo.escenario_envio_informe,        # 1   ocupa cupo
        demo.escenario_informe,              #     ocupa cupo
        demo.escenario_en_desarrollo,        #     ocupa cupo
        demo.escenario_cancelacion,          # 6   ocupa cupo
        demo.escenario_en_preparacion,       # 10  ocupa cupo (quinto y último)
        demo.escenario_no_viable,            # 5
    ]

    for paso in pasos:
        nombre = paso.__name__.replace("escenario_", "")
        try:
            paso()
            print(f"  ✓ {nombre}")
        except Exception as e:
            print(f"  ✗ {nombre}: {e}", file=sys.stderr)

    print("\n" + "=" * 78)
    print("LISTO PARA LA DEMO")
    print("=" * 78)
    for escenario, donde, quien, instancia in demo.preparados:
        print(f"\n{escenario}")
        print(f"   dónde:    {donde}")
        print(f"   usuario:  {quien}")
        print(f"   instancia: {instancia}")

    print("\n" + "-" * 78)
    print("Sitio:       http://localhost:8081       (empresa: ver sus credenciales abajo)")
    print("Flowable UI: http://localhost:8088/flowable-ui/   admin / test")
    print("Contraseña del personal: password")
    print("\nLas credenciales de cada empresa están en las variables 'empresaUsuario' y")
    print("'empresaPasswordTemporal' de su instancia, o en el log: docker compose logs flowable-app")
    return 0


if __name__ == "__main__":
    sys.exit(main())
