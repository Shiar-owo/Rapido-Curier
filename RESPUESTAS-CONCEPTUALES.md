# RESPUESTAS-CONCEPTUALES

---

## Pregunta 1 — Service Discovery

Hardcodear una IP o puerto es frágil: en Docker un contenedor puede cambiar de IP al reiniciar, en Kubernetes los pods son efímeros y las instancias se escalan dinámicamente. Eureka resuelve esto registrando cada servicio con un nombre lógico (`auth-service`, `clients-service`) y resolviéndolo a la dirección actual mediante load balancing del lado del cliente. Cuando una instancia deja de responder al heartbeat dentro del intervalo de expiración (30s en nuestra config), Eureka la marca como expirada y la elimina del registro; las demás instancias dejan de recibirlas en sus siguientes llamadas. Así el sistema se adapta automáticamente a caídas, reinicios y escalado sin reconfiguración manual.

---

## Pregunta 2 — API Gateway vs. Load Balancer

Un load balancer de red opera en capas 4-7 y distribuye tráfico según reglas estáticas (round-robin, hash), sin entender el contenido HTTP. Un API Gateway opera en capa 7 con plena comprensión del protocolo: puede validar JWT, agregar headers de trazabilidad, aplicar rate limiting, transformar rutas y reescribir paths. Por ejemplo, nuestro Gateway valida el token JWT y propaga `X-User-Id`/`X-User-Roles` a los servicios downstream — algo que un load balancer de red no puede hacer por sí solo porque no inspecciona ni modifica el payload HTTP.

---

## Pregunta 3 — Estados del Circuit Breaker

En **Closed** el circuito está cerrado y las llamadas pasan normalmente; si la tasa de fallos supera el umbral (50%), el circuito abre. En **Open** el circuito está abierto y todas las llamadas se rechazan inmediatamente sin intentar el servicio downstream, evitando colapsarlo; tras esperar `waitDurationInOpenState` (10s), pasa a Half-Open. En **Half-Open** se permiten unas pocas llamadas de prueba para verificar si el servicio se recuperó; si tienen éxito el circuito vuelve a Closed, si fallan vuelve a Open. El estado Half-Open es necesario porque sin él el circuito Open bloquearía el servicio eternamente aunque se haya recuperado, impidiendo la recuperación automática del sistema.

---

## Pregunta 4 — Config Server vs. Vault

La **URL de conexión** (`jdbc:postgresql://postgres-paquetes:5432/rapidocourier_paquetes`) va en el **Config Server** porque es información de infraestructura no sensible que puede compartirse entre entornos. La **contraseña** (`paquetes_pass`) va en **Vault** porque es un secreto que requiere control de acceso, auditoría y rotación sin reiniciar servicios. No es recomendable almacenar ambos en el mismo lugar porque el Config Server almacena configuración en un repositorio Git (visible para cualquiera con acceso al repo), mientras que Vault aplica políticas de acceso, versionado de secretos y encriptación en reposo; mezclarlos expondría credenciales en texto plano en el historial de Git.

---

## Pregunta 5 — Consistencia eventual vs. fuerte

Si usas Feign para notificar al servicio de facturación y este no está disponible, la llamada falla y la excepción se propaga: o bien la transición de estado se revierte (consistencia fuerte pero disponibilidad comprometida) o bien el paquete queda como "entregado" sin que exista la factura (inconsistencia). La comunicación asincrónica por eventos (por ejemplo, con Kafka o RabbitMQ) ofrece la ventaja de que el evento se publica en una cola y el servicio de facturación lo procesa cuando se recupere, sin perder datos ni bloquear la operación principal. Esto da disponibilidad y tolerancia a fallos a costa de consistencia eventual: durante unos segundos o minutos ambos servicios pueden estar desincronizados, pero eventualmente convergen. En un sistema courier donde la facturación puede esperar unos minutos sin impacto al cliente, esta es generalmente la decisión correcta.
