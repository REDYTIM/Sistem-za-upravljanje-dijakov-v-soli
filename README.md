# Sistem-za-upravljanje-dijakov-v-oli
Vrstni red zagona:

    - Zaženite Backend – počakajte, da se popolnoma inicializira

    - Zaženite Frontend – šele ko backend povsem deluje

Opozorilo o časovni omejitvi (Timeout)
Ob prijavi ali registraciji se lahko pojavi napaka "read timeout". To je pogosto posledica:
    - Počasne internetne povezave
    - Dolgega časa odziva strežnika (prvi zagon je lahko počasnejši)

projekt izgelda tako: 
    Sistem-za-upravljanje-dijakov-v-oli/
    ├── backend/                 # Spring Boot aplikacija
    │   ├── src/main/java/
    │   ├── pom.xml
    │   └── application.properties
    ├── frontend/                # React aplikacija
    │   ├── src/
    │   ├── public/
    │   └── package.json
    └── README.md