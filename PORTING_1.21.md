# Port de Mantle a Minecraft 1.21.X

Estado del port de **Mantle** a la rama 1.21 y decisiones de arquitectura que
lo sostienen.

---

## 1. Estado actual (verificado)

**Objetivo `1.21.1-neoforge`: funcionando.**

| Comprobación | Resultado |
|---|---|
| `:1.21.1-neoforge:compileJava` | correcto, 0 errores |
| `:1.21.1-neoforge:build` | correcto, genera jar + sources jar |
| Access transformer (validación activada) | correcto |
| `:1.21.1-neoforge:runData` | correcto — el mod carga y ejecuta sus 5 data providers |

Entorno: Java 21 · Gradle 9.7.1 · ModDevGradle 2.0.147 · Stonecutter 0.9.8 ·
NeoForge 21.1.77.

Que el datagen termine bien es la prueba relevante: implica que el mod se
construye, registra y arranca de verdad, no solo que compila.

---

## 2. Arquitectura del build

El proyecto usa **Stonecutter** como controlador multi-versión. Cada objetivo
es un nodo `versions/<mc>-<loader>/`:

- `settings.gradle.kts` declara los nodos y asocia cada uno a un buildscript
  por loader.
- `stonecutter.gradle.kts` es el controlador: fija las versiones de plugins
  una sola vez y expone constantes de loader a las fuentes, de modo que el
  código puede ramificar con `//? if neoforge { ... }`.
- `build.neoforge.gradle.kts` contiene el build de NeoForge y lee sus versiones
  de `versions/<nodo>/gradle.properties`.

Añadir una versión nueva es declarar el nodo y su `gradle.properties`.

### Solo NeoForge

El eje Forge quedó descartado por una incompatibilidad dura, comprobada:

- **ForgeGradle 6.0.54** (única vía para Forge 1.21.x) rechaza Gradle 9:
  *"Found Gradle version Gradle 9.7.1. Versions Gradle 9.0 and newer are not
  supported yet."*
- **Stonecutter 0.9.8** exige Gradle 9 o superior.

No pueden convivir en un mismo build. Soportar Forge exigiría un segundo build
independiente con su propio wrapper en Gradle 8, duplicando la cadena de
compilación para un loader cuyo ecosistema en 1.21 se movió a NeoForge.

---

## 3. Defectos corregidos en este port

Tres fallos que el "compila correctamente" estaba ocultando:

1. **El mod no habría cargado.** El repositorio seguía enviando el
   `META-INF/mods.toml` de Forge 1.20.1. NeoForge 1.21 lee
   `META-INF/neoforge.mods.toml`, y `processResources` buscaba ese nombre, que
   no existía: los placeholders `${loader_range}` y `${forge_range}` nunca se
   expandían. Se añadió el archivo correcto con el esquema de NeoForge
   (`type="required"`, dependencia de `neoforge` en vez de `forge`) y se
   eliminó el obsoleto.

2. **`loaderVersion` apuntaba a la versión equivocada.** Es la versión del
   proveedor de lenguaje `javafml` (4.x), no la de NeoForge. Con el rango de
   NeoForge, la carga fallaba con *"needs language provider javafml:21.1.77 or
   above to load, we have found 4.0.31"*. Solo el test de carga lo detectó.

3. **El access transformer no se aplicaba y estaba obsoleto.** No estaba
   registrado en el bloque `neoForge`, y sus 60 entradas usaban nombres SRG
   (`f_97726_`, `m_280092_`) que 1.20.2+ ya no resuelve. Con la validación
   activada, NeoForm las rechazó todas. Se eliminaron en lugar de remapearlas:
   las fuentes ya portadas compilan solo con las 9 entradas de `FlowingFluid`
   que ya estaban en nombres Mojang. La validación queda **activada** para que
   el archivo no vuelva a pudrirse en silencio.

---

## 4. Siguientes pasos

1. **Ampliar el rango de versiones.** Declarar nodos `1.21.4`, `1.21.5`,
   `1.21.8` y `1.21.11` e ir resolviendo las divergencias con condicionales de
   Stonecutter. Los saltos con más coste esperado son 1.21.2 (modelos de ítem y
   renderizado de entidades) y 1.21.5 (reescritura del sistema de modelos).
2. **Prueba en cliente.** El datagen valida el arranque en común; falta lanzar
   el cliente para validar el renderizado, los modelos y las pantallas de
   libros.
3. **Publicar el artefacto** para que TinkersConstruct lo consuma.
