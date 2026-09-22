# Publicación de Mantle Unofficial en Modrinth

La publicación está automatizada en `.github/workflows/publish-modrinth.yml`. La CI
normal compila el target NeoForge; la publicación ocurre solamente al publicar un
GitHub Release o al ejecutar manualmente el workflow.

## Preparación única del proyecto

1. Crear en Modrinth un proyecto de tipo **Mod** con un título que lo identifique
   claramente como un port no oficial, por ejemplo **Mantle Unofficial — 1.21.1
   NeoForge**. Mantener la nota de no afiliación y atribución MIT del README.
2. En GitHub → Settings → Secrets and variables → Actions, añadir la variable
   `MODRINTH_PROJECT_ID` con el ID del proyecto.
3. Añadir el secreto `MODRINTH_TOKEN`. Crear el token desde la cuenta dueña del
   proyecto con los permisos mínimos `VERSION_CREATE` y `PROJECT_WRITE`.
   No guardarlo en archivos del repositorio ni imprimirlo en los logs.

En cada publicación, CI compila con Java 21, excluye el sources JAR, sincroniza el
README completo y `docs/assets/icon.svg`, y publica el JAR principal como
NeoForge para Minecraft 1.21.1. Para una release etiquetada, usar `vX.Y.Z`; los
tags de prerelease pasan como beta. El envío manual valida versión y canal.

Antes de crear la primera release, verificar los metadatos de la página en
Modrinth y usar inicialmente el canal alpha/beta hasta que el port se valide en
juego. La automatización no publica en respuesta a cada push o pull request.
