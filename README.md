####review-service#####

En Spring Boot-tjänst för recensioner av rum.

####Teknik#####
- Java 17, Spring Boot, Maven 
- MariaDB
- Docker
- GitHub Actions (CI/CD)
- Railway (deploy)


####Arbetsflöde####
- Branchstrategi
main är alltid i ett deploybart skick och är skyddad.
Allt arbete görs på en egen branch med ett beskrivande namn, tex add/readme.
Branchen slås ihop med main via en pull request. Ingen pushar direkt till main.

Skydd på main (branch protection):

- Pull request krävs
- Minst 1 godkännande från en annan gruppmedlem
- CI måste vara grön innan merge

- Varför just den här strategin
vi är ett team på två personer, vi kom överrens om att Korta feature-branches med GitHub Flow
och täta merges till main ger färre och mindre merge-konflikter, 
och att main alltid är deploybar passar väl ihop med vår CI/CD-pipeline.
Vi behöver inga separata release-brancher eftersom
release styrs av vilken Docker-image som promotas till produktion.


####Från branch till produktion####
- Skapa en feature-branch från senaste main.
- Gör ändringen och pusha branchen.
- Öppna en pull request mot main (PR-mallen fylls i).
- CI körs automatiskt: tester körs och projektet byggs.
- Den andre gruppmedlemmen granskar och lämnar kommentarer. Ändringar görs vid behov.
- När PR:en är godkänd och CI är grön mergas den till main.
- Vid merge till main bygger pipelinen en Docker-image och taggar den med två taggar:
  commit-SHA:n (t.ex. review-service:3f2a1bc...) och staging. Båda pushas till Docker Hub.
- Staging (automatiskt): Railway-tjänsten för staging kör imagen review-service:staging
  och deployas med den nya imagen. Healthchek görs via /actuator/health och den gamla imagen stängs bara efter nya visar status UP.
- Produktion (manuellt): när staging ser bra ut körs workflowen promote.yml
  (Actions -> promote.yml -> Run workflow) med commit-SHA:n som input.
  Workflowen hämtar review-service:<sha>, taggar om den till review-service:production
  och pushar den. Railway-tjänsten för produktion kör review-service:production.
  Jobbet körs i GitHub-environmenten production.
- Samma image som testats i staging är alltså exakt den som hamnar i produktion,
  den byggs aldrig om.

####CI/CD-pipeline####

Pipelinen ligger i .github/workflows/ och körs vid push och pull request mot main.
går till följande
Run tests - Kör mvn test mot en tillfällig MariaDB-tjänst
Build with Maven - Bygger projektet
Login / build / push (endast vid push) - Bygger Docker-image och pushar den till Docker Hub
( känsliga värden som databasuppgifter och tokens ligger i github seecrets)

####Loggning####

Vi loggar med SLF4J och använder nivåerna så här:


- INFO	Normala, förväntade händelser (hämta, skapa, uppdatera, ta bort recensioner)
- WARN	Något efterfrågat hittades inte (tex inga recensioner för ett kund-ID)
 -ERROR	Ett externt beroende (om booking-service eller customer-service skulle finnas och dem svarar inte)


####Health check####

Tjänsten använder Spring Boot Actuator. Hälsan kan kollas på:
/actuator/health

management.endpoint.health.show-details=always är satt, så svaret visar varje komponent:

- db - Spring Boots inbyggda kontroll av databasanslutningen (MariaDB)
- externalApi - vår egen health indicator, ExternalApiHealthIndicator
  (src/main/java/org/example/reviewservice/health/). Den implementerar HealthIndicator och
  anropar GitHub Status API (https://www.githubstatus.com/api/v2/status.json).
  - Svarar API:t returneras UP med url och responseTime (ms).
  - Svarar det inte returneras DOWN med url och error (typ av fel) och en WARN loggas.
  - Connect- och read-timeout är 2 sekunder, så health-endpointen hänger inte om API:t är nere.
  - URL:en kan ändras med external.api.url, t.ex. till en adress som inte finns för att testa DOWN.

Om någon komponent är DOWN blir den totala statusen DOWN.

Railway använder samma endpoint som healthcheck (healthcheckPath i railway.json).
En ny deploy tas bara i bruk när /actuator/health svarar OK, annars ligger den
gamla versionen kvar.

####Rollback (VG)####

Eftersom varje image är taggad med sin commit-SHA finns alla tidigare versioner kvar på Docker Hub.
Rollback i produktion görs så här:

1. Ta fram SHA:n för den senaste fungerande versionen (git log på main,
   eller tidigare körningar av promote.yml i Actions).
2. Kör promote.yml igen med den SHA:n som input.
3. Workflowen taggar om den gamla imagen till review-service:production och pushar den.
4. Railways produktionstjänst deployar om med imagen och kontrollerar /actuator/health.

Ingen kod behöver byggas om, så rollbacken går snabbt.
Själva felet rättas sedan som vanligt: en ny branch (eller git revert) -> PR -> merge -> staging -> promote.

####Merge-konflikten####

Vi hade ingen merge konflikt eftersom vi arbetade inom olika områden.

Om vi hade haft en merge konflikt så hade den varit lätt att lösa genom GitHub's egna verktyg för det.



Gruppmedlemmar
Simon
Malte