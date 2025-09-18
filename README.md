# DailyMovie

Una app de Android para buscar películas y series, guardarlas en listas y descubrir qué ver hoy.

La idea es sencilla: abres la app y lo primero que ves es una película elegida para ti. No la misma para todo el mundo, sino una que encaje con lo que te gusta. A partir de ahí puedes tirar del hilo: entrar en su ficha, ver el tráiler, mirar quién sale, abrir la ficha de ese actor y acabar descubriendo otra película suya que no conocías.

<!-- TODO: capturas de pantalla -->

## Qué se puede hacer

**Descubrir**
- La portada te enseña una película recomendada según tus gustos, con su tráiler y su sinopsis
- Cuatro listas siempre al día: en cartelera, populares, mejor valoradas y próximos estrenos
- Buscador de películas, que guarda lo que has mirado antes

**Series**
- Sección propia con las series en emisión, populares y mejor valoradas
- Cada serie con sus temporadas y todos sus episodios, con imagen, fecha y duración

**Fichas completas**
- Sinopsis, géneros, duración, presupuesto, recaudación y clasificación por edad
- Dónde verla: las plataformas de streaming de tu país
- Reparto y director, y tocando a cualquiera se abre su ficha con su biografía y su filmografía
- Galería de imágenes de la película, que se ven a pantalla completa
- Tráilers, películas similares y recomendadas

**Tus listas**
- Favoritos y vistos
- Listas propias con el nombre que quieras
- Historial de lo que has buscado

**Tu cuenta**
- Registro con correo o entrando con Google
- Puedes borrar tu cuenta y todos tus datos cuando quieras

## Cómo funciona por dentro

La app está hecha en **Kotlin** siguiendo **MVVM**, con tres capas bien separadas:

```
views/  (pantallas)  →  viewmodels/  (estado)  →  data/  (repositorios)
```

Las pantallas solo pintan. Los ViewModels guardan el estado y deciden qué enseñar. Y los repositorios son los únicos que saben de dónde salen los datos.

Esa última capa es una interfaz a propósito. Los ViewModels reciben `MovieRepository` y `UserRepository` por constructor, así que en un test se les puede pasar un doble y comprobar que hacen lo que deben sin salir a internet ni tocar Firebase.

**De dónde salen los datos**
- **TMDB** para todo el catálogo: películas, series, personas, imágenes y tráilers
- **Firebase Auth** para las cuentas
- **Firestore** para tus listas, tu historial y tus gustos, con guardado offline

**Qué se ha usado**

| Para qué | Con qué |
|---|---|
| Llamadas a la API | Retrofit + Gson |
| Imágenes | Glide |
| Pantallas | ViewBinding, sin Compose |
| Navegación | Navigation Component + BottomNavigation |
| Cuentas y datos | Firebase Auth, Firestore y Google Sign-In |

## Cómo compilarlo

Necesitas **JDK 17** y el SDK de Android con la **API 34**.

**1. Consigue una clave de TMDB.** Es gratis: te registras en [themoviedb.org](https://www.themoviedb.org/) y la sacas desde tu perfil, en Configuración → API.

**2. Ponla en `local.properties`**, en la raíz del proyecto:

```properties
TMDB_API_KEY=tu_clave_aqui
```

Ese fichero no se sube al repositorio, así que la clave no sale de tu ordenador. Si se te olvida ponerla el proyecto compila igual, pero no se cargará ninguna película.

**3. Compila:**

```bash
./gradlew assembleDebug
```

El APK sale en `app/build/outputs/apk/debug/`.

Para pasar los tests:

```bash
./gradlew testDebugUnitTest
```

### Si quieres el inicio de sesión con Google

Google comprueba la firma de la app, así que tienes que dar de alta la huella de tu build en Firebase. Saca tu SHA-1 con:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
  -storepass android -keypass android
```

Y lo pegas en la consola de Firebase, en la configuración del proyecto. Sin ese paso el botón de Google devuelve el error 10, que no es un fallo del código.

## Un par de cosas que conviene saber

**Los tráilers se abren en YouTube.** No se reproducen dentro de la app, y no es por dejadez: YouTube ya no permite reproducir sus vídeos incrustados en un WebView de Android. Se probó con la librería del reproductor, cargando la URL directamente, con un iframe, tocando la cabecera Referer y el User-Agent, y aceptando las cookies. Siempre daba "vídeo no disponible", en dos dispositivos distintos. Así que se enseña la miniatura y al tocarla se abre YouTube. Volver a reproducirlos dentro sigue siendo el objetivo.

**La película del día tiene tres caminos.** Si hay una elegida a mano para hoy, se enseña esa. Si no, se busca una según los géneros que marcaste al registrarte. Y si nunca dijiste tus gustos, se enseña la más popular del momento. Así la portada nunca se queda vacía.

## De dónde viene esto

DailyMovie empezó como proyecto final del ciclo de **Desarrollo de Aplicaciones Multiplataforma**, en 2024. El código de aquella entrega está congelado en la rama `checkpoint-DAM`, por si quieres ver de dónde salió.

Lo que hay en `main` es la continuación: arreglar lo que estaba mal, rehacer la arquitectura para que se pueda probar y crecer, y añadir lo que le faltaba para ser una app de verdad.

## Licencia

Ver [LICENSE](LICENSE).

Los datos de películas y series vienen de [TMDB](https://www.themoviedb.org/), pero este proyecto no está avalado ni certificado por ellos.
