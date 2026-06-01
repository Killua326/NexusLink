# **Contrato de Comunicación (Bridge) \- NexusLink**

Este documento define el contrato de interfaz entre la capa de **Frontend (React Native / TypeScript)** y la capa **Nativa (Android / Kotlin)**.

## **1\. Patrón de Comunicación**

* **Frontend \-\> Nativo:** Se utilizarán Promises (métodos asíncronos). Cualquier invocación debe ser gestionada como una operación asíncrona no bloqueante.  
* **Nativo \-\> Frontend:** Se utilizará un DeviceEventEmitter (Event Emitter) para enviar actualizaciones de estado en tiempo real.

## **2\. Métodos (TypeScript invoca a Kotlin)**

Todos los métodos deben ser implementados en el módulo nativo como funciones que retornan una Promise.  
| **Método** | **Argumentos** | **Retorno (Promise)** | **Descripción** |  
| startServer | config: ServerConfig | boolean | Inicia el servicio en segundo plano con la configuración dada. |  
| stopServer | Ninguno | boolean | Detiene el servicio y libera los recursos. |  
| getServerStatus | Ninguno | ServerState | Retorna el estado actual para sincronización al abrir la app. |  
| pickRootDirectory | Ninguno | {uri: string, name: string} | Lanza el selector de archivos (SAF). |

## **3\. Eventos (Kotlin emite a TypeScript)**

El módulo nativo debe emitir estos eventos para mantener la UI sincronizada.

* **onServerStateChanged**  
  * **Payload:** ServerState (objeto completo).  
  * **Disparo:** Cada vez que el servidor cambia de estado (iniciado, detenido, error).  
* **onConnectionCountUpdated**  
  * **Payload:** number (contador actual).  
  * **Disparo:** Cada vez que un cliente se conecta o desconecta.  
* **onServerError**  
  * **Payload:** { code: string, message: string }.  
  * **Disparo:** Errores críticos de red o del servidor.

## **4\. Modelos de Datos (Serialización)**

Cualquier objeto pasado por el Bridge debe ser serializable a JSON.

### **ServerConfig**

{  
  port: number;  
  rootDirectoryUri: string;  
  rootDirectoryName: string;  
  isReadOnly: boolean;  
  maxConnections: number;  
}

### **ServerState**

{  
  isRunning: boolean;  
  ipAddress: string | null;  
  activeConnections: number;  
  errorMessage: string | null;  
}

## **5\. Reglas de Implementación para Copilot**

1. **Asincronía:** Nunca realices operaciones de red o I/O en el hilo principal (Main Thread) en Kotlin. Usa CoroutineScope (Dispatchers.IO).  
2. **Serialización:** Usa librerías estándar (ej. JSONObject o Gson/Moshi si es necesario) para asegurar que el objeto retornado por Kotlin sea legible en TypeScript.  
3. **Persistencia:** Cualquier cambio en la configuración debe ser notificado al estado global inmediatamente tras el éxito de la promesa.