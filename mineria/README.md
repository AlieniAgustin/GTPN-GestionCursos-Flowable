# Minería de procesos (§4.4)

Análisis de las ejecuciones reales de la **etapa de negociación** —todo lo previo a la
preparación del curso— con PM4Py sobre el historial del motor.

**La conclusión del análisis está en [`conclusion.md`](conclusion.md).**

| Archivo | Qué es |
|---|---|
| [`exportar_history.py`](exportar_history.py) | Arma el event log en CSV desde el historial de Flowable |
| [`generar_casos.py`](generar_casos.py) | Crea instancias recorriendo los cinco caminos de la negociación |
| [`analizar.py`](analizar.py) | Recorridos más frecuentes y gráficos, con PM4Py |
| [`negociacion.csv`](negociacion.csv) | El event log usado en el análisis entregado |
| [`conclusion.md`](conclusion.md) | Coincidencias, diferencias y limitaciones |
| `resultados/` | DFG y red heurística en PNG |

---

## Preparar el entorno

Una sola vez, desde la raíz del repositorio:

```bash
sudo apt install python3-venv graphviz
```

```bash
python3 -m venv .venv && .venv/bin/pip install pm4py pandas requests psycopg2-binary
```

> `graphviz` es lo que dibuja los PNG: sin él el análisis corre igual pero no genera
> gráficos. `psycopg2-binary` se suma a las dependencias del enunciado porque el log se
> lee de la base; el porqué está en la sección 3.2 de la conclusión.

---

## Reproducir el análisis

Con el stack levantado (`docker compose up -d`), desde esta carpeta:

**1. Generar casos** (opcional: solo si la base no tiene suficientes instancias)

```bash
../.venv/bin/python generar_casos.py --casos 22
```

Reparte las instancias entre los cinco caminos: aceptación, rechazo, modificaciones,
modificaciones con rechazo y cierre por falta de respuesta. Las altas van por el sitio,
así que cada instancia queda vinculada a su curso igual que una solicitud real.

> Ojo con el límite de §3.1: cuando hay cinco cursos en ejecución, las solicitudes nuevas
> salen **no viables** y no llegan a la negociación. Es el comportamiento correcto, pero
> si querés generar casos viables hay que tener cupo libre.

**2. Exportar el event log**

```bash
../.venv/bin/python exportar_history.py -o negociacion.csv
```

**3. Analizar**

```bash
../.venv/bin/python analizar.py -i negociacion.csv -o resultados
```

Imprime los tres recorridos más frecuentes con su cantidad y porcentaje, la frecuencia por
actividad, y deja los gráficos en `resultados/`.

---

## Opciones útiles

```bash
# El proceso completo, no solo la negociación
../.venv/bin/python exportar_history.py --etapa completo -o proceso.csv

# Sin acceso a la base (ver la limitación 4.2 de la conclusión)
../.venv/bin/python exportar_history.py --fuente rest

# Incluir las negociaciones que siguen abiertas
../.venv/bin/python exportar_history.py --incluir-en-curso

# Solo el análisis por consola, sin generar PNG
../.venv/bin/python analizar.py --sin-graficos
```
