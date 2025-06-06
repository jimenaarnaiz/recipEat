<p align="center">
  <a href="https://sonarcloud.io/dashboard?id=jimenaarnaiz_recipEat">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=jimenaarnaiz_recipEat&metric=sqale_rating" alt="Maintainability"/>
    <img src="https://sonarcloud.io/api/project_badges/measure?project=jimenaarnaiz_recipEat&metric=bugs" alt="Bugs"/>
    <img src="https://sonarcloud.io/api/project_badges/measure?project=jimenaarnaiz_recipEat&metric=code_smells" alt="Code Smells"/>
    <img src="https://sonarcloud.io/api/project_badges/measure?project=jimenaarnaiz_recipEat&metric=security_rating" alt="Security Rating"/>
    <img src="https://sonarcloud.io/api/project_badges/measure?project=jimenaarnaiz_recipEat&metric=vulnerabilities" alt="Vulnerabilities"/>
    <img src="https://sonarcloud.io/api/project_badges/measure?project=jimenaarnaiz_recipEat&metric=duplicated_lines_density" alt="Duplicated Lines"/>
    <img src="https://sonarcloud.io/api/project_badges/measure?project=jimenaarnaiz_recipEat&metric=ncloc" alt="Code Lines"/>
    <img src="https://sonarcloud.io/api/project_badges/measure?project=jimenaarnaiz_recipEat&metric=coverage" alt="Coverage"/>
    <img src="https://sonarcloud.io/api/project_badges/measure?project=jimenaarnaiz_recipEat&metric=reliability_rating" alt="Code Lines"/>
  </a>
</p>

<h1 align="center"> :fork_and_knife:recipEat:fork_and_knife: </h1>
Decidir qué cocinar cada día puede ser una tarea complicada. recipEat es una aplicación móvil para Android que nace con el objetivo de facilitar esta tarea
al proporcionar una manera rápida y eficiente de encontrar recetas basadas
en ingredientes, simplificando la planificación de comidas y reduciendo el
desperdicio de alimentos.

## 🔨Funcionalidades del proyecto

- `Gestión de recetas`: Los usuarios pueden explorar recetas en la pantalla de inicio, buscan recetas filtradas por ingredientes o por título y ver los detalles de cada receta. También pueden agregar recetas a sus favoritos y añadirlas al historial de cocina.
- `Historial de cocina`: La aplicación registra las recetas cocinadas por el usuario, mostrando las recetas cocinadas en los últimos 7 o 30 días, y facilitando el acceso rápido a las más recientes.
- `Búsqueda por ingredientes`: Incluye una funcionalidad de búsqueda de ingredientes con autocompletado, que permite a los usuarios seleccionar varios ingredientes y obtener recetas que contengan esos ingredientes (todos o algunos de ellos).
- `Creación y gestión de recetas personalizadas`: Los usuarios pueden crear sus propias recetas, agregando imagen, tiempo de preparación, raciones, ingredientes, pasos y ocasión. Además, pueden editar o eliminar estas recetas.
- `Modo offline`: La aplicación permite a los usuarios acceder sin conexión a Internet a: las 15 primeras recetas que se muestran en el Home, las últimas 15 recetas visitadas, las recetas creadas y las recetas favoritas. Los datos se almacenan localmente utilizando Room y se sincronizan automáticamente cuando hay conexión disponible.
- `Plan semanal`: La aplicación genera un plan semanal de comidas, asignando recetas de desayuno, almuerzo y cena a cada día de la semana. Este plan se actualiza automáticamente cada lunes mediante un worker, garantizando que no se repitan recetas de la semana anterior ni de la actual. También limita el consumo de carne y pasta a no más de 3 días y registra qué "aisle" se usa en cada comida para evitar repetir el mismo tipo de ingrediente en un día.
- `Recetas personalizadas en el Home`: La pantalla de inicio muestra un conjunto de recetas recomendadas de forma personalizada en base a las recetas favoritas, teniendo en cuenta el tipo de receta, el tiempo o el tipo de dieta.
 
## 💻Tecnologías utilizadas
<p align="left">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://developer.android.com/images/logos/android-studio.svg" alt="Android Studio" height="50"/>
  <img src="https://firebase.google.com/downloads/brand-guidelines/PNG/logo-vertical.png" alt="Firebase" height="50"/>
</p>



