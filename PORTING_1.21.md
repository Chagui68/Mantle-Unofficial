# Hoja de Ruta Técnica para el Port de Mantle a Minecraft 1.21.X (hasta 1.21.11)

Este documento detalla la arquitectura y los pasos requeridos para portar **Mantle** a **Minecraft 1.21.X**.

---

## 1. Contexto del Proyecto
- **Líneas de código Java:** ~43,504 en 594 archivos.
- **Rol en el ecosistema:** Mantle es la librería base de la cual depende Tinkers' Construct (más de 970 archivos de TCon dependen de Mantle).
- **Versión origen:** Minecraft 1.20.1 (Java 17 / Forge 47.2.0)
- **Versión destino:** Minecraft 1.21.X (Java 21 / NeoForge 21.x o Forge 1.21.x)

---

## 2. Puntos Clave de Migración

1. **Java 21:** Configuración de toolchain actualizada a JVM 21.
2. **Sistema de Datos y Serialización:**
   - Transición de NBT tradicional a `DataComponentType` para items y libros de información (`Materials and You`, etc.).
   - Actualización de Codecs para la deserialización de páginas de libros y recetas.
3. **Fluidos y Capacidades (NeoForge 21.x):**
   - Migración de `IFluidHandler` de Forge clásico a las `BlockCapability` de fluidos de NeoForge.
   - Reemplazo de `ICapabilityProvider` por `Data Attachments`.
4. **Networking:**
   - Conversión de paquetes de red a `CustomPacketPayload`.
5. **Generación de Artefacto:**
   - La tarea `jar` generará el artefacto en `build/libs/`, permitiendo que el proyecto adyacente `TinkersConstruct` lo consuma automáticamente mediante la regla `flatDir { dir '../Mantle/build/libs' }`.
