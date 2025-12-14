# DailyMovie

Una app de Android para buscar películas y series, guardarlas en listas y descubrir qué ver hoy.

La idea es sencilla: abres la app y lo primero que ves es una película elegida para ti. No la misma para todo el mundo, sino una que encaje con lo que te gusta. A partir de ahí puedes tirar del hilo: entrar en su ficha, ver el tráiler, mirar quién sale, abrir la ficha de ese actor y acabar descubriendo otra película suya que no conocías.

## Qué se puede hacer

**Descubrir**
- La portada te enseña una película recomendada según tus gustos, con su tráiler y su sinopsis, y te dice por qué te la enseña: "Porque sigues a Christopher Nolan", "Porque te gustó El Padrino"
- Cuatro listas siempre al día: en cartelera, populares, mejor valoradas y próximos estrenos
- Un botón de "no sé qué ver, sorpréndeme" para cuando llevas veinte minutos dando vueltas

**Buscar**
- Películas, series y gente del cine a la vez, y va buscando solo mientras escribes
- Sagas: si buscas "El Padrino" te sale la trilogía entera antes que las películas sueltas
- Filtros de año, nota mínima y plataforma de streaming, con las que de verdad hay en tu país
- Ordenar por lo que más encaja, por nota o por año, y seguir bajando para ver más
- Guarda lo que has buscado antes, para no volver a escribirlo

**Series**
- Sección propia con las series en emisión, populares y mejor valoradas
- Cada serie con sus temporadas y todos sus episodios, con imagen, fecha y duración
- Marca los episodios que ya has visto y la app lleva la cuenta: "llevas 12 de 62"
- En la portada, una fila de **"Sigue viendo"** con lo que tienes a medias

**Fichas completas**
- Sinopsis, géneros, duración, presupuesto, recaudación y clasificación por edad
- Dónde verla: las plataformas de streaming de tu país
- Reparto y director, y tocando a cualquiera se abre su ficha con su biografía, sus fotos y su filmografía completa (cine y series)
- Galería de imágenes de la película, que se ven a pantalla completa
- Tráilers, películas similares y recomendadas
- A qué saga pertenece, para ver el resto de una sentada
- Lo que ha escrito la gente sobre ella, y enlaces a IMDb y a su web oficial

**Tus listas**
- Favoritos y vistos
- Listas propias con el nombre que quieras, y cada una dice cuántas películas tiene
- Historial de lo que has buscado

**Tus números**
- Cuántas películas has visto, cuántas series llevas, de qué década es lo que más ves y qué es lo más antiguo que has guardado

**Tu cuenta**
- Registro con correo o entrando con Google
- Al registrarte, tres preguntas para saber qué te gusta: géneros, películas y a quién sigues. Todo opcional, y se cambia cuando quieras desde Ajustes
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
| Huecos de carga | Shimmer |
| Pantalla de arranque | core-splashscreen |

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

**La película del día tiene varios caminos.** Si hay una elegida a mano para hoy, se enseña esa. Si no, entra el recomendador, que prueba cuatro vías de más personal a menos: algo de un actor o director que sigues, algo parecido a una de tus favoritas, algo de tus géneros, y en último caso lo más popular del momento. Si una vía no da nada se cae a la siguiente, así la portada nunca se queda vacía. Y como la elección va con el día del año, cambia de un día para otro pero no cada vez que abres la app: si cambiara a cada rato no sería "la película del día" de nadie.

**El diseño no es casualidad.** Azul marino de sala a oscuras (`#1c2541`), menta y teal para lo que importa (`#6fffe9`, `#5bc0be`) y una tipografía monoespaciada, Courier Prime, en toda la app, que le da ese aire de créditos escritos a máquina. No es lo habitual en Android, y esa es justo la gracia.

## De dónde viene esto

DailyMovie empezó como proyecto final del ciclo de **Desarrollo de Aplicaciones Multiplataforma**, en 2024. El código de aquella entrega está congelado en la rama `checkpoint-DAM`, por si quieres ver de dónde salió.

Lo que hay en `main` es la continuación: arreglar lo que estaba mal, rehacer la arquitectura para que se pueda probar y crecer, y añadir lo que le faltaba para ser una app de verdad.

## Licencia

Ver [LICENSE](LICENSE).

Los datos de películas y series vienen de [TMDB](https://www.themoviedb.org/), pero este proyecto no está avalado ni certificado por ellos.
