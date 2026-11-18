#!/usr/bin/env python3
"""Exporta el historial de actividades de Flowable a un CSV de event log (enunciado 4.4).

La fuente es `ACT_HI_ACTINST`: una fila por cada actividad que ejecutó cada instancia,
con sus tiempos. Se puede leer de dos maneras:

  `--fuente db`   (default) lee la tabla directamente de PostgreSQL.
  `--fuente rest` usa `/process-api/history/historic-activity-instances`.

La diferencia importa. El modelo usa una **compuerta basada en eventos** para esperar la
respuesta de la empresa: cuando la instancia llega ahí, el motor abre las cuatro ramas
(Aceptación, Rechazo, Modificaciones y el plazo de 5 días) y, en cuanto una ocurre,
cancela las otras tres. En el historial quedan registradas **las cuatro**, y las
canceladas se distinguen solo por `DELETE_REASON_ = 'event based gateway cancel'`, un
campo que la API REST no expone.

Si se exporta por REST, el log dice que cada instancia pasó por Aceptación *y* Rechazo
*y* Modificaciones, y los recorridos dejan de significar algo. Por eso el default es la
base: ahí se puede descartar la rama que no ocurrió y el log refleja lo que pasó.

Uso:
    python exportar_history.py                        # negociación -> negociacion.csv
    python exportar_history.py --etapa completo -o proceso.csv
    python exportar_history.py --fuente rest          # sin acceso a la base
"""

import argparse
import csv
import sys

import psycopg2
import requests

# Columnas con los nombres que espera pm4py sin necesidad de renombrar nada
COLUMNAS = [
    "case:concept:name",   # instancia del proceso
    "concept:name",        # actividad
    "time:timestamp",      # inicio
    "end_timestamp",
    "org:resource",        # quien la ejecutó, cuando es una tarea de usuario
    "activity_type",
]

# La negociación va desde que llega la solicitud hasta que se acepta la propuesta (y se
# habilita el acceso de la empresa) o la solicitud se archiva por rechazo, falta de
# respuesta o inviabilidad. Todo lo que sigue —el subproceso de preparación, el dictado
# y el cierre— queda afuera.
ACTIVIDADES_NEGOCIACION = {
    "solicitudrecibida",
    "evaluarviabilidad",
    "esviable",
    "notificardecision",
    "solicitudnoviable",
    "registraryelaborarpropuesta",
    "enviarpropuesta",
    "esperarrespuesta",
    "aceptacion",
    "rechazo",
    "modificaciones",
    "plazo5dias",
    "seenviaron2recordatorios",
    "enviarrecordatorio",
    "revisarpropuesta",
    "archivarsolicitud",
    "solicitudcerrada",
    "habilitaraccesodelaempresa",
}

# La negociación de un caso se considera concluida cuando pasó por alguna de estas: o
# la propuesta se aceptó y el curso pasó a preparación, o la solicitud se archivó, o se
# notificó que no era viable.
CIERRES_DE_NEGOCIACION = {
    "habilitaraccesodelaempresa",
    "solicitudcerrada",
    "archivarsolicitud",
    "notificardecision",
}

# Los nodos de control no son actividades de negocio: ensucian el log sin aportar
TIPOS_IGNORADOS = {
    "exclusiveGateway",
    "parallelGateway",
    "inclusiveGateway",
    "eventBasedGateway",
}


CONSULTA_SQL = """
    SELECT a.proc_inst_id_,
           a.act_id_,
           a.act_name_,
           a.act_type_,
           a.assignee_,
           a.start_time_,
           a.end_time_
      FROM act_hi_actinst a
      JOIN act_re_procdef d ON d.id_ = a.proc_def_id_
     WHERE d.key_ = %s
       -- Las ramas que la compuerta de eventos descartó no ocurrieron: si quedaran,
       -- cada instancia parecería haber sido aceptada y rechazada a la vez.
       AND (a.delete_reason_ IS NULL OR a.delete_reason_ <> 'event based gateway cancel')
     ORDER BY a.proc_inst_id_, a.start_time_, a.id_
"""


def descargar_de_db(dsn, process_key):
    """Lee el historial de PostgreSQL, descartando las ramas canceladas."""
    with psycopg2.connect(dsn) as conexion, conexion.cursor() as cursor:
        cursor.execute(CONSULTA_SQL, (process_key,))
        return [
            {
                "processInstanceId": fila[0],
                "activityId": fila[1],
                "activityName": fila[2],
                "activityType": fila[3],
                "assignee": fila[4],
                "startTime": fila[5].isoformat() if fila[5] else "",
                "endTime": fila[6].isoformat() if fila[6] else "",
            }
            for fila in cursor.fetchall()
        ]


def descargar(base_url, usuario, password, process_key, pagina=100):
    """Trae todas las actividades históricas, paginando hasta agotar el historial."""
    actividades = []
    inicio = 0

    while True:
        respuesta = requests.get(
            f"{base_url}/process-api/history/historic-activity-instances",
            params={
                "processDefinitionKey": process_key,
                "start": inicio,
                "size": pagina,
                "sort": "startTime",
                "order": "asc",
            },
            auth=(usuario, password),
            timeout=30,
        )
        respuesta.raise_for_status()
        cuerpo = respuesta.json()
        datos = cuerpo.get("data", [])

        actividades.extend(datos)
        inicio += len(datos)

        if not datos or inicio >= cuerpo.get("total", 0):
            return actividades


def nombre_de(actividad):
    """El nombre legible del modelo; si no lo tiene, el id de la actividad."""
    return (actividad.get("activityName") or actividad.get("activityId") or "").strip()


def filas(actividades, solo_negociacion):
    for actividad in actividades:
        tipo = actividad.get("activityType", "")
        if tipo in TIPOS_IGNORADOS:
            continue

        if solo_negociacion and actividad.get("activityId") not in ACTIVIDADES_NEGOCIACION:
            continue

        nombre = nombre_de(actividad)
        if not nombre:
            continue

        yield {
            "case:concept:name": actividad.get("processInstanceId", ""),
            "concept:name": nombre,
            "time:timestamp": actividad.get("startTime", ""),
            "end_timestamp": actividad.get("endTime", "") or "",
            "org:resource": actividad.get("assignee", "") or "sistema",
            "activity_type": tipo,
            "activity_id": actividad.get("activityId", ""),
        }


def solo_completas(registros):
    """Descarta los casos cuya negociación sigue abierta.

    Una instancia que todavía espera la respuesta de la empresa tiene las cuatro ramas
    de la compuerta activas a la vez, porque de verdad están esperando en paralelo. Es
    el estado real, pero mete un recorrido que no es un camino sino una foto a mitad de
    camino. La minería de variantes se hace sobre casos terminados.
    """
    por_caso = {}
    for fila in registros:
        por_caso.setdefault(fila["case:concept:name"], []).append(fila)

    completas = [
        caso for caso in por_caso.values()
        if any(f["activity_id"] in CIERRES_DE_NEGOCIACION for f in caso)
    ]
    return [fila for caso in completas for fila in caso], len(por_caso) - len(completas)


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("-o", "--output", default="negociacion.csv",
                        help="archivo CSV de salida (default: negociacion.csv)")
    parser.add_argument("--etapa", choices=["negociacion", "completo"], default="negociacion",
                        help="qué parte del proceso exportar (default: negociacion)")
    parser.add_argument("--url", default="http://localhost:8080", help="URL del motor")
    parser.add_argument("--user", default="admin")
    parser.add_argument("--password", default="test")
    parser.add_argument("--process-key", default="Process_Curso")
    parser.add_argument("--fuente", choices=["db", "rest"], default="db",
                        help="de dónde leer el historial (default: db)")
    parser.add_argument("--dsn", default="postgresql://flowable:flowable@localhost:5432/flowable",
                        help="conexión a PostgreSQL cuando --fuente db")
    parser.add_argument("--incluir-en-curso", action="store_true",
                        help="incluir los casos cuya negociación todavía no concluyó")
    args = parser.parse_args()

    try:
        if args.fuente == "db":
            actividades = descargar_de_db(args.dsn, args.process_key)
        else:
            actividades = descargar(args.url, args.user, args.password, args.process_key)
            print("AVISO: por REST no se pueden descartar las ramas que la compuerta de "
                  "eventos canceló;\n       los recorridos van a mostrar Aceptación, "
                  "Rechazo y Modificaciones juntos.", file=sys.stderr)
    except psycopg2.Error as e:
        print(f"ERROR: no se pudo leer la base ({args.dsn}): {e}", file=sys.stderr)
        print("       Probá con --fuente rest.", file=sys.stderr)
        return 1
    except requests.RequestException as e:
        print(f"ERROR: no se pudo leer el historial de {args.url}: {e}", file=sys.stderr)
        return 1

    registros = list(filas(actividades, args.etapa == "negociacion"))

    descartadas = 0
    if args.etapa == "negociacion" and not args.incluir_en_curso:
        registros, descartadas = solo_completas(registros)

    for fila in registros:
        fila.pop("activity_id", None)

    if not registros:
        print("ERROR: el historial no tiene actividades para exportar.", file=sys.stderr)
        print("       Generá instancias con scripts/seed-demo.sh y volvé a intentar.",
              file=sys.stderr)
        return 1

    with open(args.output, "w", newline="", encoding="utf-8") as salida:
        escritor = csv.DictWriter(salida, fieldnames=COLUMNAS)
        escritor.writeheader()
        escritor.writerows(registros)

    instancias = {fila["case:concept:name"] for fila in registros}
    print(f"{args.output}: {len(registros)} eventos de {len(instancias)} instancias "
          f"(etapa: {args.etapa}, fuente: {args.fuente})")
    if descartadas:
        print(f"  {descartadas} instancias descartadas: su negociación sigue abierta")
    return 0


if __name__ == "__main__":
    sys.exit(main())
