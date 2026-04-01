# Changelog - Muzei Roma

Tutte le modifiche degne di nota a questo progetto saranno documentate in questo file.

## [0.17] - 2026-04-01

### Modificato
- Rimosso plugin `foojay-resolver` da `settings.gradle` (compatibilità F-Droid).

## [0.16] - 2026-03-26

### Aggiunto
- **Condivisione opera**: Nuova funzione per condividere l'immagine corrente direttamente dal menu di Muzei.
- **Supporto FileProvider**: Reintrodotto per permettere la condivisione sicura dei file scaricati con altre applicazioni.

### Modificato
- Versione dell'app aggiornata a 0.16 (saltata la 0.15).

## [0.14] - 2026-03-25

### Aggiunto
- **Archivio ampliato**: dataset `ninfa.csv` aggiornato con 7 nuove opere (41 totali), incluse architetture e statue di autore ignoto.
- **Immagine del giorno**: il wallpaper segue un ciclo di 41 giorni con partenza dal 24 marzo 2026 (colonna GIORNO del CSV).
- **Impostazioni dentro Muzei**: premendo "Impostazioni" nella selezione sorgente di Muzei si apre `MuzeiSettingsActivity` con le opzioni di rotazione:
  - *Solo immagine del giorno*: Muzei mostra unicamente l'opera abbinata alla data corrente.
  - *Rotazione* (default): coda strutturata `[giorno, r1, r2, r3, giorno, ...]`, con numero di opere casuali personalizzabile.
- **Selezione lingua**: Supporto per Italiano, Inglese e Francese.
- **Link al progetto**: Aggiunto link al sito ufficiale nella Home.

### Corretto
- **Visualizzazione Centrata**: Ottimizzata la gestione delle immagini orizzontali affinché Muzei applichi il Center Crop.
- **Stabilità Rete**: Risolto connection leak in OkHttp e aggiunti timeout di 30s.
- **Estensioni File**: Gestione dinamica delle estensioni (.jpg, .png, .webp) basata sul Content-Type del server.
- **Nomi e Accenti**: Risolti problemi di codifica caratteri nel database (passaggio a Windows-1252 per il CSV).

### Modificato
- Portato il database alla versione 6 con gestione corretta delle migrazioni.
- Target SDK aggiornato a 35 per conformità Play Store.

## [0.13] - 2026-03-16
- Correzione `persistentUri` per stabilità con Muzei API.
- Rimozione codice morto e file orfani.

## [0.12] - 2026-03-16
- `OkHttpClient` reso singleton per efficienza.
- Migliorata la gestione del contatore opere nella Home.

## [0.11] - 2026-03-16
- Aggiunta migrazione Room 4→5.
- Configurazione ProGuard/R8 per build di release.

## [0.10] - 2026-03-15
- Implementato sistema di log persistente visualizzabile nella Home.

## [0.1] - 2026-03-12
- Versione iniziale (Beta).
- Implementazione base `MuzeiArtProvider`.
