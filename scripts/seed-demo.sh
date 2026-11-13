#!/usr/bin/env bash
#
# Carga datos de prueba en la BD de la institución y en el motor.
#
# Da de alta empresas y solicitudes por la API del BFF —igual que lo haría el sitio
# web— y las deja repartidas en distintos estados, para tener algo que mostrar en la
# demo y recorridos variados para el análisis de §4.3 y la minería de §4.4.
#
# El esquema de la base NO lo crea este script: lo genera Hibernate al arrancar el BFF
# (`spring.jpa.hibernate.ddl-auto=update`). Acá solo se cargan datos.
#
# Uso:
#   ./scripts/seed-demo.sh [cantidad]     # por defecto 6 solicitudes
#
# Requiere el stack levantado (docker compose up -d) y curl + python3.
set -euo pipefail

BFF="${BFF_URL:-http://localhost:8081}"
ENGINE="${ENGINE_URL:-http://localhost:8080}"
ENGINE_AUTH="${ENGINE_AUTH:-admin:test}"
USER="${SEED_USER:-jefeadministrativo}"
PASS="${SEED_PASS:-password}"
TOTAL="${1:-6}"

COOKIES="$(mktemp)"
trap 'rm -f "$COOKIES"' EXIT

EMPRESAS=(
  "ACME SA|contacto@acme.com|+543581234567"
  "Molinos del Sur|rrhh@molinosdelsur.com|+543584445566"
  "TecnoRio SRL|capacitacion@tecnorio.com|+543585556677"
  "Frigorifico Pampa|personal@frigopampa.com|+543586667788"
  "Cooperativa La Union|formacion@launion.coop|+543587778899"
  "Textiles Andinas|rrhh@textilesandinas.com|+543588889900"
)

CURSOS=(
  "Introduccion a BPMN|Modelado de procesos de negocio con notacion BPMN 2.0"
  "Seguridad e Higiene|Normativa vigente y practicas de prevencion en planta"
  "Excel Avanzado|Tablas dinamicas, macros y analisis de datos"
  "Atencion al Cliente|Comunicacion efectiva y manejo de reclamos"
  "Primeros Auxilios|Protocolos de emergencia y RCP basico"
  "Liderazgo de Equipos|Delegacion, feedback y gestion de conflictos"
)

log()  { printf '%s\n' "$*"; }
step() { printf '  %-28s %s\n' "$1" "$2"; }

require_stack() {
  if ! curl -sf -o /dev/null "$BFF/health"; then
    log "ERROR: el BFF no responde en $BFF. Levantá el stack con 'docker compose up -d'."
    exit 1
  fi
  if ! curl -sf -o /dev/null "$ENGINE/health"; then
    log "ERROR: el motor no responde en $ENGINE."
    exit 1
  fi
}

login() {
  local code
  code=$(curl -s -c "$COOKIES" -o /dev/null -w '%{http_code}' -X POST "$BFF/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "$(python3 -c 'import json,sys; print(json.dumps({"username":sys.argv[1],"password":sys.argv[2]}))' "$USER" "$PASS")")
  if [ "$code" != "200" ]; then
    log "ERROR: no se pudo autenticar como '$USER' (HTTP $code)."
    log "       Revisá que el seed de usuarios haya corrido: curl -s $ENGINE/groups"
    exit 1
  fi
}

# Da de alta la solicitud por el BFF. Imprime el processInstanceId.
crear_solicitud() {
  local empresa="$1" curso="$2"
  IFS='|' read -r nombre email telefono <<< "$empresa"
  IFS='|' read -r titulo descripcion <<< "$curso"

  local body
  body=$(python3 - "$nombre" "$email" "$telefono" "$titulo" "$descripcion" <<'PY'
import json, sys, datetime
nombre, email, telefono, titulo, descripcion = sys.argv[1:6]
hoy = datetime.date.today()
fechas = [(hoy + datetime.timedelta(days=d)).isoformat() for d in (45, 60, 75)]
print(json.dumps({
    "contact": {"name": nombre, "email": email, "phone": telefono},
    "name": titulo,
    "description": descripcion,
    "participantCount": 12,
    "possibleDates": fechas,
}))
PY
)
  curl -s -b "$COOKIES" -X POST "$BFF/api/courses" -H 'Content-Type: application/json' -d "$body" \
    | python3 -c 'import sys,json; print(json.load(sys.stdin).get("processInstanceId",""))'
}

tarea_de() {
  curl -s -u "$ENGINE_AUTH" "$ENGINE/process-api/runtime/tasks?processInstanceId=$1&taskDefinitionKey=$2" \
    | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d["data"][0]["id"] if d["total"] else "")'
}

# Completa la propuesta con programa y presupuesto, que es lo que se vuelca a la BD.
completar_propuesta() {
  local pid="$1" titulo="$2" presupuesto="$3"
  local tid; tid=$(tarea_de "$pid" registraryelaborarpropuesta)
  [ -n "$tid" ] || return 1
  local body
  body=$(python3 - "$titulo" "$presupuesto" <<'PY'
import json, sys
titulo, presupuesto = sys.argv[1], sys.argv[2]
print(json.dumps({"action": "complete", "variables": [
    {"name": "propuesta",   "value": f"Propuesta para {titulo}"},
    {"name": "programa",    "value": f"Programa de {titulo}: 4 modulos, 16 horas"},
    {"name": "presupuesto", "value": presupuesto},
]}))
PY
)
  curl -s -o /dev/null -X POST "$ENGINE/process-api/runtime/tasks/$tid" -u "$ENGINE_AUTH" \
    -H 'Content-Type: application/json' -d "$body"
}

# Envía un mensaje a la instancia, con variables opcionales en JSON.
mensaje() {
  local pid="$1" nombre="$2" vars="${3:-[]}"
  local exec_id
  exec_id=$(curl -s -u "$ENGINE_AUTH" -G "$ENGINE/process-api/runtime/executions" \
    --data-urlencode "processInstanceId=$pid" \
    --data-urlencode "messageEventSubscriptionName=$nombre" \
    | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d["data"][0]["id"] if d["total"] else "")')
  [ -n "$exec_id" ] || return 1
  local body
  body=$(python3 - "$nombre" "$vars" <<'PY'
import json, sys
print(json.dumps({"action": "messageEventReceived", "messageName": sys.argv[1],
                  "variables": json.loads(sys.argv[2])}))
PY
)
  curl -s -o /dev/null -X PUT "$ENGINE/process-api/runtime/executions/$exec_id" -u "$ENGINE_AUTH" \
    -H 'Content-Type: application/json' -d "$body"
}

require_stack
login
log "Cargando $TOTAL solicitudes en $BFF (usuario $USER)"
log ""

creadas=0
for i in $(seq 0 $((TOTAL - 1))); do
  empresa="${EMPRESAS[$((i % ${#EMPRESAS[@]}))]}"
  curso="${CURSOS[$((i % ${#CURSOS[@]}))]}"
  titulo="${curso%%|*}"

  pid=$(crear_solicitud "$empresa" "$curso")
  if [ -z "$pid" ]; then
    log "  [${i}] ERROR al crear la solicitud de '${empresa%%|*}'"
    continue
  fi
  creadas=$((creadas + 1))
  log "  [${i}] ${empresa%%|*} — $titulo"
  step "instancia" "$pid"

  # Se reparten los recorridos para que el event log tenga variedad (§4.4)
  case $((i % 4)) in
    0)  # aceptada: queda en preparación, con programa, presupuesto y fecha en la BD
        completar_propuesta "$pid" "$titulo" "180000.00" && step "propuesta" "enviada"
        fecha=$(python3 -c 'import datetime; print((datetime.date.today()+datetime.timedelta(days=45)).isoformat())')
        mensaje "$pid" "Aceptación" "[{\"name\":\"fechaAcordada\",\"value\":\"$fecha\"}]" \
          && step "aceptacion" "fecha acordada $fecha"
        ;;
    1)  # rechazada: termina RECHAZADO
        completar_propuesta "$pid" "$titulo" "95000.00" && step "propuesta" "enviada"
        mensaje "$pid" "Rechazo" && step "rechazo" "instancia cerrada"
        ;;
    2)  # con modificaciones: vuelve a revisión y queda esperando respuesta
        completar_propuesta "$pid" "$titulo" "120000.00" && step "propuesta" "enviada"
        mensaje "$pid" "Solicitud de modificaciones" "[{\"name\":\"observaciones\",\"value\":\"Reducir la carga horaria\"}]" \
          && step "modificaciones" "pendiente de revision"
        ;;
    3)  # recién ingresada: queda en la bandeja del jefe administrativo
        step "estado" "pendiente de propuesta"
        ;;
  esac
  log ""
done

log "Listo: $creadas solicitudes creadas."
log ""
log "Para ver cómo quedó la BD de la institución:"
log "  docker exec flowable-postgres psql -U flowable -d flowable -c \\"
log "    'select id, name, program is not null as programa, budget, agreed_date, status from courses order by id;'"
