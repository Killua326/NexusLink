**Registro de Bugs y Soluciones: NexusLink WebDAV Server

Este documento registra los desafíos técnicos superados al construir un servidor WebDAV sobre Android Storage Access Framework (SAF) para interconectar clientes nativos de macOS (Finder) y Windows (Explorer).
1. Bug: Finder de macOS borra el archivo al intentar guardar ("Safe Save Destructivo")

Síntoma: Al abrir un archivo desde macOS, modificarlo y presionar "Guardar", el archivo original desaparecía (o pesaba 0 bytes) y no se aplicaban los cambios.

Causa Raíz:
El Finder de macOS utiliza un flujo de guardado seguro (Safe Save via Shadow Bundle). En lugar de hacer un PUT directo sobre el archivo, realiza lo siguiente:

    Crea una carpeta temporal (shadow).

    Hace un PUT del nuevo contenido dentro de esa carpeta temporal.

    Hace un MOVE (rename) del archivo temporal hacia la raíz para reemplazar el original.

    Hace un DELETE del backup.

El Storage Access Framework (SAF) de Android no permite renombres cruzados entre directorios (renameTo). Cuando el Finder pedía mover el archivo de la carpeta shadow a la raíz, SAF fallaba silenciosamente, pero el Finder continuaba con el flujo ejecutando el DELETE del archivo original. Resultado: Pérdida total de datos. Además, nuestro PUT anterior ejecutaba existing.delete() prematuramente.

Solución Implementada:

    Transacciones Atómicas en PUT: Se modificó la lógica para que las subidas escriban primero en un archivo .tmp. Solo si el flujo de bytes finaliza con éxito (writeSuccess == true), se procede a borrar el original y renombrar el archivo temporal.

    MOVE Cross-Directory (safMove): Se implementó una función safMove inteligente. Primero comprueba si el origen y destino comparten el mismo directorio padre (en cuyo caso usa renameTo). Si los directorios difieren o el renombrado nativo falla, ejecuta una copia binaria de emergencia (InputStream a OutputStream) y elimina el origen.

2. Bug: Windows bloquea copias indicando "El archivo es demasiado grande para el dispositivo"

Síntoma:
A pesar de tener gigabytes de espacio libre en el dispositivo Android, Windows Explorer rechazaba cualquier intento de copiar archivos hacia la unidad de red, mostrando un error de falta de espacio.

Causa Raíz:
Antes de iniciar una transferencia, el cliente WebClient de Windows realiza una petición PROPFIND con Depth: 0 a la raíz del servidor. Espera encontrar las propiedades <D:quota-available-bytes> y <D:quota-used-bytes> formateadas estrictamente según la RFC 4331.
En la primera versión, estas etiquetas estaban mezcladas dentro del mismo <D:propstat> que getcontentlength y se aplicaban a todos los archivos. Windows no lograba parsearlas, asumiendo un espacio disponible de 0 bytes.

Solución Implementada:

    Cálculo de Espacio Real: Se integró la clase StatFs apuntando a Environment.getDataDirectory().path para extraer dinámicamente los bytes libres del sistema de archivos interno de Android.

    Inyección en XML (RFC 4331): En el método buildPropfindXml, se creó un bloque <D:propstat> completamente separado y exclusivo para las cuotas, y se configuró para que se envíe únicamente cuando el nodo consultado es una carpeta (isDirectory). Esto satisfizo las validaciones previas a la copia de Windows.

3. Bug: Nombres de archivo con doble extensión (Ej. archivo.txt.txt o archivo.bin)

Síntoma:
Al subir un archivo, Android agregaba extensiones no deseadas de forma automática, rompiendo las referencias que el cliente esperaba.

Causa Raíz:
El método nativo DocumentFile.createFile(mimeType, displayName) de Android es "demasiado inteligente". Si el MIME type no coincide exactamente con lo que Android considera que debería ser la extensión, el ContentProvider fuerza la adición de una segunda extensión (por ejemplo, añadiendo .bin a flujos application/octet-stream).

Solución Implementada:
Se creó el wrapper createSafFileExact(). Este método crea el archivo y, acto seguido, verifica si el nombre final otorgado por Android difiere del solicitado por el cliente. De ser así, invoca renameTo inmediatamente para limpiar el nombre y forzar la nomenclatura exacta requerida por el protocolo WebDAV.
4. Limitaciones Conocidas (Aceptadas)

    Advertencia de "Otra aplicación modificó el archivo" en macOS: Ocurre esporádicamente en editores como TextEdit. Se debe a que el servidor implementa un sistema de LOCK falso (dummy) y no posee motor de ETags, apoyándose únicamente en getlastmodified (el cual SAF de Android muta impredeciblemente).

    Advertencia de Versioning en macOS: Al cerrar un archivo modificado, Finder indica que el volumen no soporta almacenamiento permanente de versiones. Esto es estándar en servidores de terceros, ya que no implementamos las extensiones privativas .DocumentRevisions-V100 del ecosistema APFS de Apple.**