# Global Dispatch — NewCron sobre Solace PubSub+

Solución del reto **Global Dispatch** (Guatemaltek): un sistema que simula cómo
NewCron conecta transportistas de car haulers con predios de automóviles en
Estados Unidos, usando **Solace PubSub+** como bróker de mensajería.

## 1. ¿Qué hace este proyecto?

Tres programas independientes (Spring Boot), cada uno una pieza del flujo descrito en el reto:

| Programa | Rol | Qué hace |
|---|---|---|
| `order-intake` | Productor | Recibe el payload de una solicitud de carga (archivo `.json`), valida las reglas de fechas de NewCron y publica el resultado en Solace |
| `carrier-dashboard` | Consumidor | Simula el panel de transportistas: muestra en tiempo real las cargas disponibles y permite "tomar" una desde la consola |
| `client-status` | Consumidor | Simula el apartado del cliente: muestra en tiempo real si su solicitud fue `Accepted` o `Cancelled`, y por qué |

### Reglas de validación (`common/.../DateValidator.java`)

1. `pickupDate` no puede ser anterior a la fecha actual.
2. Si `pickupDate` es **hoy**, la solicitud no puede llegar después de las **3:00 p.m.**
3. `deliveryDate` debe ser mayor a `pickupDate`, con **al menos un día** de diferencia.

Si alguna regla falla, se genera un payload `Cancelled` con la nota correspondiente
(igual al ejemplo del documento). Si todas pasan, el pedido se publica para los
transportistas y se genera de inmediato un payload `Accepted` (esto refleja
exactamente el ejemplo del documento: la nota dice que el cliente *recibirá un
correo cuando un transportista acepte*, es decir, `Accepted` = "tu solicitud fue
aceptada por el sistema", no = "un transportista ya la tomó". La toma real de
la carga en el dashboard es una simulación operativa aparte, sin payload propio,
porque el documento no define uno para ese evento).

### Arquitectura de mensajería

```
                 publica pedido válido               consume
 order-intake ───────────────────────────►  orders/new  ────────► carrier-dashboard
      │                                    (tópico)                (cola: orders-pending-q)
      │
      │  publica Accepted / Cancelled                consume
      └───────────────────────────────────► orders/result ───────► client-status
                                            (tópico)                (cola: orders-results-q)
```

- Se usan **tópicos** para publicar (`orders/new`, `orders/result`) y **colas
  durables** para consumir (`orders-pending-q`, `orders-results-q`), mapeadas
  al tópico correspondiente ("Topic to Queue Mapping", patrón estándar de
  Solace). Las colas se crean automáticamente al arrancar cada consumidor
  (`session.provision(...)` + `session.addSubscription(...)`), no hace falta
  crearlas a mano en la consola.
- Usar tópico + cola (en vez de publicar directo a una cola) permite, si
  quisieras extenderlo, tener varios dashboards o varios clientes escuchando
  al mismo tiempo sin tocar el productor.

## 2. Requisitos previos

- JDK 17+
- Maven 3.9+
- Una cuenta y un servicio de **Solace Cloud** (o Solace PubSub+ Software si prefieres correrlo local con Docker)

## 3. Crear el broker en Solace Cloud

1. Entra a [console.solace.cloud](https://console.solace.cloud/mc/services) y crea una cuenta (hay plan gratuito).
2. En **Cluster Manager**, haz clic en **Create Service**, elige el plan gratuito y una región cercana.
3. Cuando el servicio esté "Running", entra a **Manage** → pestaña **Connect**.
4. Copia estos cuatro datos (los necesitarás en el paso 4):
   - **SMF Host** (algo como `tcps://xxxxx.messaging.solace.cloud:55443`, o usa el puerto `55555` sin TLS si prefieres)
   - **Message VPN**
   - **Username**
   - **Password**

## 4. Configurar las credenciales

Cada módulo lee la configuración de variables de entorno (con valores por
defecto en `application.properties`). Antes de correr cualquier programa,
exporta:

```bash
export SOLACE_HOST="tcp://xxxxx.messaging.solace.cloud:55555"
export SOLACE_VPN="tu-vpn"
export SOLACE_USERNAME="tu-usuario"
export SOLACE_PASSWORD="tu-password"
```

## 5. Compilar el proyecto

Desde la raíz `global-dispatch/`:

```bash
mvn clean install
```

Esto compila los cuatro módulos (`common`, `order-intake`, `carrier-dashboard`,
`client-status`) y corre las pruebas unitarias de `DateValidator`.

## 6. Ejecutar el flujo completo

Abre **tres terminales** (con las variables de entorno del paso 4 ya exportadas en cada una).

**Terminal 1 — Dashboard de transportistas (déjalo corriendo):**
```bash
cd carrier-dashboard
mvn spring-boot:run
```

**Terminal 2 — Estado del cliente (déjalo corriendo):**
```bash
cd client-status
mvn spring-boot:run
```

**Terminal 3 — Enviar una solicitud:**
```bash
cd order-intake
mvn spring-boot:run -Dspring-boot.run.arguments=../order-intake/sample-payloads/valid.json
```

Verás:
- En la Terminal 1: la carga nueva apareciendo, y puedes escribir `6600111` para "tomarla".
- En la Terminal 2: `[ACCEPTED] Pedido #6600111 -> You will receive an email when a carrier accepts this dispatch request`

> Nota: los `.json` de ejemplo en `order-intake/sample-payloads/` tienen fechas
> fijas. Antes de probar, ajusta `pickupDate`/`deliveryDate` para que sean
> coherentes con la fecha real en que corras la prueba.

## 7. Probar los casos de cancelación

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=../order-intake/sample-payloads/invalid-pickup-in-past.json
mvn spring-boot:run -Dspring-boot.run.arguments=../order-intake/sample-payloads/invalid-delivery-too-soon.json
```

En la Terminal 2 (Client Status) deberías ver ambos como `[CANCELLED]`, con el
motivo correspondiente ("Pickup date cannot be earlier than the current date."
/ "Delivery date must be at least one day after the pickup date.") y **no**
deberían aparecer en el dashboard de transportistas (Terminal 1), porque
nunca se publican en `orders/new`.

También puedes correr solo las pruebas unitarias de las reglas de negocio, sin necesidad de un broker Solace:

```bash
cd common
mvn test
```

## 8. Estructura del repositorio

```
global-dispatch/
├── pom.xml                      # POM padre (agrega los 4 módulos)
├── README.md
├── common/                      # Modelos, validación y utilidades Solace compartidas
│   └── src/main/java/com/newcron/dispatch/common/
│       ├── model/                (ShipperOrderRequest, Stop, Vehicle, OrderResult)
│       ├── validation/            DateValidator.java  (+ tests)
│       └── solace/                SolaceSessionFactory, JsonMessageCodec, DurableQueueProvisioner
├── order-intake/                 # Productor: valida y publica
│   └── sample-payloads/          Payloads .json de ejemplo (válido y dos casos inválidos)
├── carrier-dashboard/             # Consumidor: dashboard de transportistas
└── client-status/                 # Consumidor: estado para el cliente
```

