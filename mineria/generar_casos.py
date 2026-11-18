#!/usr/bin/env python3
"""Genera instancias con recorridos variados de la etapa de negociación (enunciado 4.4).

La minería necesita un event log con varios casos y, sobre todo, con **recorridos
distintos**: si todas las instancias siguen el camino feliz, el análisis no muestra nada.
Este script recorre a propósito los cinco caminos que el modelo permite en la negociación:

    aceptacion      solicitud -> propuesta -> Aceptación           (pasa a preparación)
    rechazo         solicitud -> propuesta -> Rechazo              -> Archivar solicitud
    modificaciones  solicitud -> propuesta -> Modificaciones -> Revisar -> Aceptación
    mod_rechazo     igual que el anterior, pero termina en rechazo
    sin_respuesta   solicitud -> propuesta -> 2 recordatorios      -> Archivar solicitud

Las altas van por el sitio (el BFF), así que cada instancia queda vinculada a su curso en
la base de la institución, igual que una solicitud real. Los mensajes de la empresa y los
temporizadores se disparan por la API REST del motor, que es lo que haría la app de
comunicaciones del enunciado 4.2.2.

Uso:
    python generar_casos.py                 # 16 casos repartidos entre los cinco caminos
    python generar_casos.py --casos 30
"""

import argparse
import datetime
import itertools
import random
import sys
import time

import requests

EMPRESAS = [
    ("Molinos del Sur", "rrhh@molinosdelsur.com", "+543584445566"),
    ("TecnoRio SRL", "capacitacion@tecnorio.com", "+543585556677"),
    ("Frigorifico Pampa", "personal@frigopampa.com", "+543586667788"),
    ("Cooperativa La Union", "formacion@launion.coop", "+543587778899"),
    ("Textiles Andinas", "rrhh@textilesandinas.com", "+543588889900"),
    ("Metalurgica Rio Cuarto", "capacitacion@metalrc.com", "+543581112233"),
]

CURSOS = [
    ("Introduccion a BPMN", "Modelado de procesos con notacion BPMN 2.0"),
    ("Seguridad e Higiene", "Normativa vigente y prevencion en planta"),
    ("Excel Avanzado", "Tablas dinamicas, macros y analisis de datos"),
    ("Atencion al Cliente", "Comunicacion efectiva y manejo de reclamos"),
    ("Primeros Auxilios", "Protocolos de emergencia y RCP basico"),
    ("Liderazgo de Equipos", "Delegacion, feedback y gestion de conflictos"),
]

# Proporción de cada camino. Se repite en ciclo hasta completar los casos pedidos.
MEZCLA = (["rechazo"] * 3 + ["sin_respuesta"] * 3 + ["modificaciones"] * 2
          + ["mod_rechazo"] * 1 + ["aceptacion"] * 1)


class Motor:
    """Lo mínimo de la API REST de Flowable para mover una instancia."""

    def __init__(self, url, auth):
        self.url = url
        self.auth = auth

    def tarea(self, instancia, clave):
        r = requests.get(f"{self.url}/process-api/runtime/tasks",
                         params={"processInstanceId": instancia, "taskDefinitionKey": clave},
                         auth=self.auth, timeout=30)
        r.raise_for_status()
        datos = r.json()
        return datos["data"][0]["id"] if datos["total"] else None

    def completar(self, tarea_id, variables=None):
        payload = {"action": "complete"}
        if variables:
            payload["variables"] = [{"name": k, "value": v} for k, v in variables.items()]
        requests.post(f"{self.url}/process-api/runtime/tasks/{tarea_id}",
                      json=payload, auth=self.auth, timeout=30).raise_for_status()

    def mensaje(self, instancia, nombre, variables=None):
        r = requests.get(f"{self.url}/process-api/runtime/executions",
                         params={"processInstanceId": instancia,
                                 "messageEventSubscriptionName": nombre},
                         auth=self.auth, timeout=30)
        r.raise_for_status()
        datos = r.json()
        if not datos["total"]:
            return False

        payload = {"action": "messageEventReceived", "messageName": nombre}
        if variables:
            payload["variables"] = [{"name": k, "value": v} for k, v in variables.items()]
        requests.put(f"{self.url}/process-api/runtime/executions/{datos['data'][0]['id']}",
                     json=payload, auth=self.auth, timeout=30).raise_for_status()
        return True

    def disparar_timer(self, instancia, intentos=8, espera=0.4):
        """Ejecuta el temporizador pendiente, para no esperar los plazos reales.

        Después de disparar uno, el motor tarda un instante en crear el siguiente, así
        que se reintenta: sin esto, el segundo recordatorio nunca llega y la instancia
        queda esperando para siempre.
        """
        for intento in range(intentos):
            r = requests.get(f"{self.url}/process-api/management/timer-jobs",
                             params={"processInstanceId": instancia}, auth=self.auth, timeout=30)
            r.raise_for_status()
            datos = r.json()

            if datos["total"]:
                requests.post(
                    f"{self.url}/process-api/management/timer-jobs/{datos['data'][0]['id']}",
                    json={"action": "move"}, auth=self.auth, timeout=30).raise_for_status()
                time.sleep(espera)
                return True

            time.sleep(espera)
        return False


def alta_por_el_sitio(bff, sesion, empresa, curso):
    """Registra la solicitud como lo haría una empresa desde la portada."""
    nombre, email, telefono = empresa
    titulo, descripcion = curso
    hoy = datetime.date.today()

    respuesta = sesion.post(f"{bff}/solicitudes", data={
        "companyName": nombre,
        "companyEmail": email,
        "companyPhone": telefono,
        "name": titulo,
        "description": descripcion,
        "participantCount": random.randint(8, 25),
        "preferredDates": ", ".join(
            (hoy + datetime.timedelta(days=d)).isoformat() for d in (60, 75)),
    }, timeout=30, allow_redirects=False)
    respuesta.raise_for_status()


def ultima_instancia(bff, sesion):
    """El curso recién creado es el primero del listado, que viene por id descendente."""
    cursos = sesion.get(f"{bff}/api/courses", timeout=30)
    cursos.raise_for_status()
    datos = cursos.json()
    return datos[0]["processInstanceId"] if datos else None


def elaborar_propuesta(motor, instancia, presupuesto):
    tarea = motor.tarea(instancia, "registraryelaborarpropuesta")
    if not tarea:
        return False
    motor.completar(tarea, {
        "propuesta": "Propuesta inicial",
        "programa": "Programa de 4 modulos, 16 horas",
        "presupuesto": presupuesto,
    })
    return True


def archivar(motor, instancia):
    tarea = motor.tarea(instancia, "archivarsolicitud")
    if tarea:
        motor.completar(tarea)


def recorrer(motor, instancia, camino):
    """Lleva la instancia por el camino pedido. Devuelve cómo terminó."""
    fecha = (datetime.date.today() + datetime.timedelta(days=60)).isoformat()

    if not elaborar_propuesta(motor, instancia, 120000):
        return "sin propuesta (no viable)"

    if camino == "aceptacion":
        motor.mensaje(instancia, "Aceptación", {"fechaAcordada": fecha})
        return "aceptada"

    if camino == "rechazo":
        motor.mensaje(instancia, "Rechazo")
        archivar(motor, instancia)
        return "rechazada"

    if camino in ("modificaciones", "mod_rechazo"):
        motor.mensaje(instancia, "Solicitud de modificaciones",
                      {"observaciones": "Reducir la carga horaria"})
        revisar = motor.tarea(instancia, "revisarpropuesta")
        if revisar:
            motor.completar(revisar, {"propuesta": "Propuesta revisada",
                                      "programa": "Programa de 3 modulos, 12 horas",
                                      "presupuesto": 95000})
        if camino == "modificaciones":
            motor.mensaje(instancia, "Aceptación", {"fechaAcordada": fecha})
            return "modificada y aceptada"
        motor.mensaje(instancia, "Rechazo")
        archivar(motor, instancia)
        return "modificada y rechazada"

    if camino == "sin_respuesta":
        # Dos recordatorios y el cierre: tres vencimientos del plazo de respuesta
        for _ in range(3):
            if not motor.disparar_timer(instancia):
                break
        archivar(motor, instancia)
        return "cerrada sin respuesta"

    return "?"


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--casos", type=int, default=16, help="cuántas instancias generar")
    parser.add_argument("--bff", default="http://localhost:8081")
    parser.add_argument("--url", default="http://localhost:8080", help="URL del motor")
    parser.add_argument("--user", default="jefeadministrativo")
    parser.add_argument("--password", default="password")
    parser.add_argument("--engine-user", default="admin")
    parser.add_argument("--engine-password", default="test")
    args = parser.parse_args()

    sesion = requests.Session()
    login = sesion.post(f"{args.bff}/login",
                        data={"username": args.user, "password": args.password},
                        timeout=30, allow_redirects=False)
    if login.status_code not in (301, 302) or "login?error" in login.headers.get("Location", ""):
        print(f"ERROR: no se pudo entrar al sitio como '{args.user}'.", file=sys.stderr)
        return 1

    motor = Motor(args.url, (args.engine_user, args.engine_password))
    caminos = itertools.cycle(MEZCLA)
    resumen = {}

    for i in range(args.casos):
        camino = next(caminos)
        empresa = EMPRESAS[i % len(EMPRESAS)]
        curso = CURSOS[(i // 2) % len(CURSOS)]

        try:
            alta_por_el_sitio(args.bff, sesion, empresa, curso)
            instancia = ultima_instancia(args.bff, sesion)
            if not instancia:
                print(f"  [{i}] no se pudo leer la instancia recién creada", file=sys.stderr)
                continue
            final = recorrer(motor, instancia, camino)
        except requests.RequestException as e:
            print(f"  [{i}] {camino}: ERROR {e}", file=sys.stderr)
            continue

        resumen[final] = resumen.get(final, 0) + 1
        print(f"  [{i:>2}] {camino:<15} -> {final}")

    print("\nResumen:")
    for final, cantidad in sorted(resumen.items(), key=lambda x: -x[1]):
        print(f"  {cantidad:>3}  {final}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
