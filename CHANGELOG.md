# Changelog - Muzei Roma

Tutte le modifiche degne di nota a questo progetto saranno documentate in questo file.

## [0.13] - 2026-03-16

### Corretto
- Rimosso `file_paths.xml` orfano (nessun FileProvider dichiarato nel Manifest).
- Aggiunto `fallbackToDestructiveMigration()` per gestire versioni database < 4 senza crash.
- `executeRequest()` ora propaga le eccezioni al chiamante per una corretta gestione degli errori di rete.
- `Response` OkHttp chiusa correttamente tramite `.use {}` eliminando potenziali connection leak.
- Ripristinato `persistentUri` all'URL HTTPS originale per stabilità con Muzei API.
- `proguard-rules.pro` reso chirurgico: keep solo su classi/membri strettamente necessari.

## [0.12] - 2026-03-16

### Corretto
- Estensione file immagine derivata dal `Content-Type` HTTP (non più hardcoded `.jpg`).
- `OkHttpClient` reso singleton via `companion object` + `lazy` (evita ricreazione ad ogni worker run).
- Aggiunto `readTimeout(30s)` a `OkHttpClient`.
- `totalArtworks` caricato una sola volta in `onCreate`, non ad ogni `collect`.
- `webUri` reso null-safe con `takeIf { isNotEmpty() }`.

## [0.11] - 2026-03-16

### Corretto
- Migrazione Room 4→5 aggiunta esplicitamente (`MIGRATION_4_5`).
- Creato `proguard-rules.pro` con regole per Room, WorkManager e Muzei API.
- Rimosso file di test `ninfa_pre.csv` dagli asset.

## [0.10] - 2026-03-15

### Aggiunto
- Implementata la versione 0.10 con miglioramenti strutturali.
- Rafforzata la logica di visualizzazione centrata (Center Crop) per le immagini orizzontali in Muzei.
- Ottimizzato il testo del pulsante Home: ora mostra "scarica altre" o "Sincronizzato".
- Implementata la ritenzione automatica dei log (pruning) per mantenere solo gli ultimi 200 messaggi.

### Modificato
- Database portato alla versione 5 con rimozione di `fallbackToDestructiveMigration()` per garantire la persistenza dei dati nei futuri aggiornamenti.
- Migliorata la stabilità del Provider rimuovendo gli operatori `!!` e aggiungendo timeout di rete espliciti (30s).
- Corretta la datazione storica del changelog per riflettere lo sviluppo reale del progetto.

## [0.9] - 2026-03-15
- Sperimentazione della logica di centratura immagini landscape.
- Prima implementazione del pulsante dinamico "scarica altre".

## [0.8] - 2026-03-15
- Implementazione del sistema di log persistente su Room con interfaccia RecyclerView.
- Supporto per codifica caratteri Windows-1252 (risoluzione problemi accenti).
- Correzione automatica degli URL di Wikipedia (risolto download Traditio Legis ed Estasi).

## [0.7] - 2026-03-15
- Introduzione di metadati arricchiti nell'UI di Muzei (Attribution include forma e tipo opera).
- Sostituzione di `setArtwork()` con `addArtwork()` per evitare glitch sul wallpaper.
- Aggiunta della nuova icona adattiva dell'app.

## [0.6] - 2026-03-15
- Cambio package name ufficiale in `it.fonsolo.muzeiroma`.
- Nuovo sistema di gestione file: il Provider serve direttamente i flussi immagine a Muzei.

## [0.5] - 2026-03-15
- Aggiunta licenza open source GPLv3.
- Passaggio definitivo al dataset `ninfa.csv`.

## [0.4] - 2026-03-12
- Prima revisione del database Room per il supporto a miniature e download locali.

## [0.3] - 2026-03-12
- Implementazione database locale SQLite (Room).
- Aggiunto comando Muzei "Prossima opera" e "Dettagli Wikipedia".

## [0.2] - 2026-03-12
- Creazione icone launcher adattive iniziali.
- Ottimizzazione risoluzione immagini a 1920px.

## [0.1] - 2026-03-12
- Versione iniziale (Beta).
- Implementazione base `MuzeiArtProvider`.
