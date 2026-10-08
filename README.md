# WalletPlus

App Android nativa de finanzas personales, **offline y privada**: registra ingresos y gastos en segundos, entiende en qué gastas, sigue presupuestos y metas, y recibe recomendaciones de ahorro basadas en tus propios datos.

Los datos nunca salen del dispositivo: la app no pide permiso de Internet, no tiene cuentas de usuario y la copia de seguridad en la nube está deshabilitada para la base de datos.

## Funcionalidades (Fase 1)

- **Dashboard**: saldo actual, ingresos y gastos del ciclo de pago (mes o quincena), capacidad y porcentaje de ahorro, presupuesto general, distribución de gastos, meta destacada y recomendación principal.
- **Registro rápido**: una sola hoja con teclado numérico propio, categoría preseleccionada, fecha, método de pago y "¿Es necesario?". Se puede deshacer al guardar o eliminar.
- **Historial**: agrupado por día, con búsqueda (descripción, notas, categoría) y filtros por tipo, fecha, categoría y método. Deslizar para eliminar.
- **Presupuestos**: general (ingreso esperado / gasto máximo / ahorro objetivo) y por categoría, con estados Normal, Precaución, Alto y Excedido. Se miden por mes o por quincena (del 1 al 15 y del 16 a fin de mes) según el ciclo de pago elegido; al cambiarlo, los montos se convierten (un mes = dos quincenas).
- **Metas de ahorro**: progreso, monto restante y ahorro recomendado por semana y por mes o quincena.
- **Análisis**: vista semanal, quincenal o mensual (por defecto, el ciclo de pago), tendencia de los últimos 6 periodos, distribución por categoría, necesario vs. opcional, comparación con el periodo anterior y gastos pequeños frecuentes.
- **Recomendaciones** por reglas locales (sin IA externa), con tono educativo.
- **Ajustes**: moneda, saldo inicial, categorías (crear, editar, desactivar), bloqueo con PIN, datos de demostración y borrado.

## Arquitectura

```text
:domain   Kotlin puro (sin Android): modelos, Money/Percent, cálculos,
          motor de recomendaciones, casos de uso e interfaces de repositorio.
:app      Android: Room (data), DI manual (core/di), seguridad, design system
          (core/ui) y pantallas Compose + ViewModel/StateFlow (presentation).
```

- **Clean Architecture ligera + MVVM**: UI → ViewModel → casos de uso → repositorio → Room.
- **Una sola fuente de verdad**: el saldo nunca se guarda; se calcula como saldo inicial + ingresos − gastos.
- **Dinero sin Double**: `Money` guarda centavos en `Long` (con desbordamiento detectado); `Percent` usa puntos básicos, sin NaN ni divisiones por cero.
- **Fechas con `java.time`**: `LocalDate` como epochDay, `Instant` en UTC.
- **Umbrales centralizados** en `domain/rules/FinanceRules.kt`.
- **Preparado para el futuro**: tabla de movimientos recurrentes, varias cuentas, contrato `ExportService` y bloqueo listo para sumar biometría.
- **Migración a Flutter**: `:domain` no conoce Android, así que entidades, reglas y casos de uso se pueden portar a Dart uno a uno.

## Stack

Kotlin 2.3, Jetpack Compose + Material 3, Navigation Compose (rutas tipadas), Room 2.8 (KSP), Lifecycle ViewModel + StateFlow, Coroutines y kotlinx.serialization. Tipografía Inter incluida en la app (funciona sin conexión).

## Compilar y probar

Requiere JDK 17 o superior (por ejemplo, el JBR de Android Studio) y el SDK de Android con la plataforma 37.

```bash
./gradlew :app:assembleDebug                 # APK de depuración
./gradlew :domain:test :app:testDebugUnitTest # pruebas unitarias
./gradlew :app:connectedDebugAndroidTest      # pruebas de Room en un dispositivo/emulador
```

Para explorar la app con datos de ejemplo, en la bienvenida toca **Explorar con datos de demostración** (se marcan claramente y se pueden borrar desde Ajustes).

> Las recomendaciones son educativas y se basan solo en tus registros; no constituyen asesoría financiera profesional.
