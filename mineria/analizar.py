#!/usr/bin/env python3
"""Minería de procesos sobre la etapa de negociación (enunciado 4.4).

Toma el CSV que genera `exportar_history.py` y produce:

  1. Una representación gráfica del comportamiento observado (heurística y DFG).
  2. Los tres recorridos más frecuentes, con cantidad y porcentaje de instancias.
     Si hay menos de tres recorridos distintos, los muestra todos.

Uso:
    python analizar.py                              # lee negociacion.csv
    python analizar.py -i negociacion.csv -o salida/
"""

import argparse
import os
import sys
from collections import Counter

import pandas as pd
import pm4py

TOP_RECORRIDOS = 3


def cargar(ruta):
    """Lee el CSV y lo deja como event log de pm4py."""
    df = pd.read_csv(ruta)

    faltan = {"case:concept:name", "concept:name", "time:timestamp"} - set(df.columns)
    if faltan:
        raise ValueError(f"al CSV le faltan columnas: {', '.join(sorted(faltan))}")

    df["time:timestamp"] = pd.to_datetime(df["time:timestamp"], format="ISO8601", utc=True)

    # Dos actividades pueden arrancar en el mismo milisegundo (el motor encadena varias
    # de corrido); el orden del CSV ya viene por startTime, así que se respeta como
    # desempate para que las variantes salgan estables entre corridas.
    df = df.reset_index().sort_values(["case:concept:name", "time:timestamp", "index"])
    return df.drop(columns=["index"])


def recorridos(df):
    """Cuenta las secuencias de actividades distintas, de la más frecuente a la menos."""
    secuencias = (df.groupby("case:concept:name")["concept:name"]
                    .apply(lambda actividades: " → ".join(actividades)))
    return Counter(secuencias), len(secuencias)


def exportar_graficos(df, destino):
    """Guarda el DFG y el modelo heurístico. Devuelve los archivos que pudo escribir."""
    generados = []
    log = pm4py.convert_to_event_log(df)

    try:
        dfg, inicio, fin = pm4py.discover_dfg(log)
        ruta = os.path.join(destino, "dfg-negociacion.png")
        pm4py.save_vis_dfg(dfg, inicio, fin, ruta)
        generados.append(ruta)
    except Exception as e:
        print(f"  (no se pudo generar el DFG: {e})", file=sys.stderr)

    try:
        red = pm4py.discover_heuristics_net(log, dependency_threshold=0.5)
        ruta = os.path.join(destino, "heuristica-negociacion.png")
        pm4py.save_vis_heuristics_net(red, ruta)
        generados.append(ruta)
    except Exception as e:
        print(f"  (no se pudo generar la red heurística: {e})", file=sys.stderr)

    return generados


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("-i", "--input", default="negociacion.csv", help="CSV del event log")
    parser.add_argument("-o", "--output", default="resultados",
                        help="carpeta donde dejar los gráficos (default: resultados)")
    parser.add_argument("--sin-graficos", action="store_true",
                        help="solo el análisis por consola")
    args = parser.parse_args()

    try:
        df = cargar(args.input)
    except FileNotFoundError:
        print(f"ERROR: no existe {args.input}. Generalo con exportar_history.py", file=sys.stderr)
        return 1
    except ValueError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1

    variantes, total_instancias = recorridos(df)

    print("=" * 78)
    print("MINERÍA DE PROCESOS — ETAPA DE NEGOCIACIÓN")
    print("=" * 78)
    print(f"Instancias analizadas: {total_instancias}")
    print(f"Eventos:               {len(df)}")
    print(f"Actividades distintas: {df['concept:name'].nunique()}")
    print(f"Recorridos distintos:  {len(variantes)}")

    mostrar = min(TOP_RECORRIDOS, len(variantes))
    if len(variantes) < TOP_RECORRIDOS:
        print(f"\nHay menos de {TOP_RECORRIDOS} recorridos distintos: se muestran todos.")

    print(f"\n--- {mostrar} recorridos más frecuentes " + "-" * 44)
    for puesto, (secuencia, cantidad) in enumerate(variantes.most_common(mostrar), start=1):
        porcentaje = cantidad * 100 / total_instancias
        print(f"\n{puesto}. {cantidad} instancias ({porcentaje:.1f} %)")
        for paso, actividad in enumerate(secuencia.split(" → "), start=1):
            print(f"     {paso:>2}. {actividad}")

    cubierto = sum(c for _, c in variantes.most_common(mostrar))
    print(f"\nLos {mostrar} recorridos cubren {cubierto} de {total_instancias} instancias "
          f"({cubierto * 100 / total_instancias:.1f} %).")

    print("\n--- Frecuencia por actividad " + "-" * 49)
    for actividad, cantidad in Counter(df["concept:name"]).most_common():
        print(f"  {cantidad:>4}  {actividad}")

    if not args.sin_graficos:
        os.makedirs(args.output, exist_ok=True)
        print(f"\n--- Gráficos " + "-" * 64)
        for ruta in exportar_graficos(df, args.output):
            print(f"  {ruta}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
