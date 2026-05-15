# Davivienda - Optimizador Inteligente de JARs

Herramienta grafica que analiza y reduce el tamano de archivos JAR eliminando clases no utilizadas mediante analisis de bytecode con ASM.

## Requisitos

- Java 8 (1.8) o superior
- No requiere Maven ni ninguna otra herramienta

## Descarga

Descarga la ultima version desde [Releases](https://github.com/JhonmySoftware/jar-shrinker/releases):

| Archivo | Descripcion |
|---------|-------------|
| `jar-shrinker-1.0.0.jar` | JAR ejecutable (145 KB) |
| `run-shrinker.bat` | Lanzador para Windows |

## Uso

### Opcion 1 - Ejecutar el .bat (Windows)

```
run-shrinker.bat
```

### Opcion 2 - Ejecutar directamente

```
java -jar jar-shrinker-1.0.0.jar
```

## Como funciona

1. Abre la interfaz y arrastra tu **fat JAR** (o haz clic en "Seleccionar JAR")
2. Indica los **paquetes raiz** de tu proyecto (ej: `org.example.tests, org.testng`)
3. Haz clic en **"Comprimir y Exportar"**
4. El tool analiza bytecode con ASM, identifica SOLO las clases alcanzables desde tus entry points
5. Genera un nuevo JAR con las clases eliminadas

## Ejemplo de reduccion

| JAR | Original | Optimizado | Ahorro |
|-----|----------|------------|--------|
| automation-fat.jar | 52 MB | ~10 MB | ~80% |
| selenium-standalone.jar | 30 MB | ~6 MB | ~80% |

Los resultados varian segun la cantidad de codigo no utilizado en tus dependencias.

## Compilar desde codigo fuente

```bash
mvn clean package
```

## Tecnologia

- Java 8+
- ASM 9.6 (bytecode analysis)
- Swing (interfaz grafica)
- Maven Shade Plugin (autocontenido)
