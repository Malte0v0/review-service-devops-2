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
vi är ett team på två personer, vi kom överrens om att Korta feature-branches 
och täta merges till main ger färre och mindre merge-konflikter, 
och att main alltid är deploybar passar väl ihop med vår CI/CD-pipeline


####Från branch till produktion####
- Skapa en feature-branch från senaste main.
- Gör ändringen och pusha branchen.
- Öppna en pull request mot main (PR-mallen fylls i).
- CI körs automatiskt: tester körs och projektet byggs.
- Den andre gruppmedlemmen granskar och lämnar kommentarer. Ändringar görs vid behov.
- När PR:en är godkänd och CI är grön mergas den till main.
- Vid merge till main bygger pipelinen en Docker-image, taggar den och pushar den till Docker Hub.
! inte klar [Beskriv hur deployen sker till Render – automatiskt vid merge, eller manuellt.]
  [VG: Staging deployas automatiskt, produktion via manuell trigger / tagg.]!

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




####Rollback (VG)####





Merge-konflikten



Gruppmedlemmar
simon
Malte