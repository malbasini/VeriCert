# Configurazione Certificati per Adobe Acrobat/Reader

## Problema: "Validità della firma sconosciuta" / "Identità firmatario sconosciuta"

Questi messaggi appaiono quando Adobe non riconosce l'autorità di certificazione (CA) che ha firmato il documento.

**⚠️ IMPORTANTE**: Il sistema VeriCert usa **certificati autofirmati** per ogni tenant. Questo significa che il certificato firma se stesso invece di essere firmato da una CA riconosciuta. Per questo motivo, **gli utenti devono installare manualmente il certificato pubblico** in Adobe.

## Soluzione: Installare il Certificato CA in Adobe

### Passo 1: Ottenere il Certificato CA

Contattare l'amministratore di sistema per ottenere:
- Il certificato ROOT della CA (file .cer o .crt)
- Eventuali certificati INTERMEDI della catena

### Passo 2: Installare il Certificato in Adobe Acrobat/Reader

#### Su Windows/Mac:

1. **Aprire Adobe Acrobat/Reader**

2. **Accedere alle Preferenze**
   - Windows: Menu `Modifica` → `Preferenze`
   - Mac: Menu `Adobe Acrobat` → `Preferenze`

3. **Navigare nelle Impostazioni Firme**
   - Nel pannello sinistro, selezionare `Firme`
   - Cliccare su `Identità e certificati fidati` → `Altro...`

4. **Importare il Certificato**
   - Nel pannello `Certificati fidati`, cliccare su `Importa`
   - Selezionare il file del certificato CA (.cer o .crt)
   - Cliccare su `Apri`

5. **Configurare il Livello di Fiducia**
   - Nella finestra `Importa dettagli contatto`, selezionare:
     - ✅ `Usa questo certificato come radice fidata`
     - ✅ `Firme dei documenti e come identità del certificatore`
   - Cliccare su `OK`

6. **Confermare**
   - Apparirà un avviso di sicurezza
   - Leggere attentamente e cliccare su `OK` se si desidera procedere

7. **Verificare l'Installazione**
   - Il certificato dovrebbe ora apparire nell'elenco dei certificati fidati
   - Aprire nuovamente un PDF firmato per verificare

### Passo 3: Verificare la Firma

Dopo l'installazione:
1. Aprire un PDF firmato
2. Cliccare sul pannello delle firme (icona penna)
3. La firma dovrebbe ora mostrare un segno di spunta verde ✓
4. Il messaggio dovrebbe essere: "Firmato e tutte le firme sono valide"

## Verifica della Catena di Certificati nel File P12

### Requisiti per il File P12

Il file P12 deve contenere:
1. **Certificato End-Entity** (il certificato dell'utente)
2. **Certificati Intermedi** (uno o più, se presenti)
3. **Certificato ROOT** (opzionale ma raccomandato)
4. **Chiave Privata** (necessaria per la firma)

### Come Verificare il Contenuto del P12

#### Usando OpenSSL (da riga di comando):

```bash
# Elencare i certificati nel file P12
openssl pkcs12 -in certificate.p12 -info -nokeys

# Verificare la catena completa
openssl pkcs12 -in certificate.p12 -cacerts -nokeys
```

Dovrebbe mostrare tutti i certificati nella catena.

### Come Creare un P12 con la Catena Completa

Se il vostro P12 non contiene la catena completa:

```bash
# Esportare la chiave privata
openssl pkcs12 -in old.p12 -nocerts -out private.key

# Esportare il certificato
openssl pkcs12 -in old.p12 -clcerts -nokeys -out cert.crt

# Creare un nuovo P12 con la catena completa
openssl pkcs12 -export -out new.p12 \
  -inkey private.key \
  -in cert.crt \
  -certfile intermediate.crt \
  -certfile root.crt
```

## Configurazione TSA (Time Stamp Authority) - Opzionale

Per migliorare ulteriormente la validazione delle firme, è possibile configurare un server TSA.

### Nel file `application.properties`:

```properties
# TSA gratuito di esempio (Freetsa)
vericert.tsa.url=http://freetsa.org/tsr

# Oppure TSA commerciale
# vericert.tsa.url=https://timestamp.digicert.com
```

### TSA Pubblici Gratuiti:

- **FreeTSA**: `http://freetsa.org/tsr`
- **DigiCert**: `http://timestamp.digicert.com`
- **Sectigo**: `http://timestamp.sectigo.com`

**Nota**: I servizi gratuiti potrebbero avere limitazioni di rate o disponibilità.

## Risoluzione Problemi

### "La firma è valida ma il firmatario è sconosciuto"

**Causa**: Il certificato ROOT non è installato come fidato in Adobe.

**Soluzione**: Seguire i passaggi sopra per installare il certificato ROOT.

### "Impossibile verificare la firma"

**Causa**: La catena di certificati è incompleta nel PDF.

**Soluzione**:
1. Verificare che il file P12 contenga la catena completa
2. Controllare i log dell'applicazione per vedere quanti certificati sono stati inclusi
3. Ricreare il file P12 con tutti i certificati intermedi

### "Il certificato è scaduto"

**Causa**: Il certificato usato per firmare è scaduto.

**Soluzione**: Usare un certificato valido per le nuove firme. Le firme esistenti rimangono valide se hanno un timestamp TSA.

## ⚠️ CERTIFICATI AUTOFIRMATI - IL TUO CASO

**VeriCert genera automaticamente certificati autofirmati per ogni tenant.**

### Come Riconoscere un Certificato Autofirmato

Nei log dell'applicazione quando viene firmato un PDF:

```
=== PDF Signing Certificate Chain ===
Certificate chain length: 1
Certificate 0:
  Subject: CN=Azienda Demo,OU=Tenant,O=Vercert,C=IT
  Issuer:  CN=Azienda Demo,OU=Tenant,O=Vercert,C=IT  ← IDENTICI!
  ⚠️  SELF-SIGNED CERTIFICATE - Users must manually trust this in Adobe
```

**Se Subject = Issuer** → Certificato autofirmato

### Soluzione: Distribuire il Certificato Pubblico agli Utenti

#### Metodo 1: Estrarre dal Database (Più Semplice)

Il certificato è già salvato in formato PEM nella tabella `signing_key`:

```sql
SELECT cert_pem
FROM signing_key
WHERE status = 'ACTIVE'
  AND kid LIKE 'tenant-%';
```

Salva il contenuto come `tenant-certificate.crt` e distribuiscilo agli utenti.

#### Metodo 2: Creare un Endpoint per il Download

Aggiungi questo controller per permettere agli admin di scaricare il certificato:

```java
@GetMapping("/admin/tenant/certificate/download")
public ResponseEntity<String> downloadPublicCertificate() {
    User user = getCurrentUser();
    Long tenantId = user.getTenant().getId();

    SigningKeyEntity sk = tenantSigningKeyService.ensureTenantKey(
        tenantId,
        user.getTenant().getName()
    );

    String certPem = sk.getCertPem();

    return ResponseEntity.ok()
        .header("Content-Disposition",
                "attachment; filename=tenant-" + tenantId + "-certificate.crt")
        .contentType(MediaType.TEXT_PLAIN)
        .body(certPem);
}
```

#### Metodo 3: Estrarre da P12 (se necessario)

```bash
# Estrarre il certificato pubblico dal P12
openssl pkcs12 -in certificate.p12 -clcerts -nokeys -out tenant-certificate.crt

# Quando richiesto, inserire la password del P12
```

### Installazione per gli Utenti Finali

1. **Ottenere** il file `tenant-certificate.crt` dall'amministratore

2. **Aprire Adobe Acrobat/Reader**
   - Windows: `Modifica` → `Preferenze`
   - Mac: `Adobe Acrobat` → `Preferenze`

3. **Importare il Certificato**
   - `Firme` → `Identità e certificati fidati` → `Altro...`
   - `Certificati fidati` → `Importa`
   - Selezionare il file `.crt`

4. **IMPORTANTE - Configurare la Fiducia**:
   - ✅ `Usa questo certificato come radice fidata`
   - ✅ `Firme dei documenti e come identità del certificatore`
   - ✅ `Creazione di certificati affidabili` (opzionale)

5. **Accettare l'Avviso** di sicurezza

6. **Testare** con un PDF firmato

### ⚠️ Limitazioni dei Certificati Autofirmati

| Aspetto | Certificato Autofirmato | Certificato da CA Commerciale |
|---------|------------------------|-------------------------------|
| **Installazione utente** | ❌ Manuale per ogni utente | ✅ Automatica (CA già fidata) |
| **Scalabilità** | ❌ Problematica con molti utenti | ✅ Ottima |
| **Costo** | ✅ Gratuito | ❌ A pagamento (€50-500/anno) |
| **Validità Adobe** | ⚠️ Solo dopo installazione | ✅ Immediata |
| **Uso consigliato** | Test, uso interno | Produzione, utenti esterni |

### 🏢 Passare a una CA Commerciale (Raccomandato per Produzione)

#### 1. Acquistare un Certificato

Provider consigliati:
- **DigiCert** (~€200/anno) - Il più affidabile
- **GlobalSign** (~€150/anno) - Buon rapporto qualità/prezzo
- **Sectigo** (~€80/anno) - Economico

Tipo: **"Document Signing Certificate"** o **"Code Signing Certificate"**

#### 2. Ricevere i File

Dopo la verifica, riceverai:
- `your-certificate.crt` (certificato end-entity)
- `intermediate.crt` (certificati intermedi della CA)
- `root.crt` (certificato root - opzionale)
- `private.key` (chiave privata)

#### 3. Creare il P12 con Catena Completa

```bash
openssl pkcs12 -export -out production.p12 \
  -inkey private.key \
  -in your-certificate.crt \
  -certfile intermediate.crt \
  -name "VeriCert Production Certificate" \
  -passout pass:SecurePassword123
```

#### 4. Importare nel Sistema

Aggiorna il database:

```sql
UPDATE signing_key
SET p12_blob = LOAD_FILE('/path/to/production.p12'),
    cert_pem = LOAD_FILE('/path/to/your-certificate.crt'),
    p12_password_enc = 'encrypted_password_here',
    status = 'ACTIVE'
WHERE kid = 'tenant-1-20260127';
```

**Oppure** usa il metodo `rotateTenantKey()` per sostituire la chiave corrente.

#### 5. Verificare

Dopo il cambio:
- I PDF firmati verranno automaticamente riconosciuti da Adobe
- Gli utenti NON dovranno installare nulla
- La firma mostrerà immediatamente il ✓ verde

### Script SQL per Esportare Certificati

```sql
-- Esporta tutti i certificati pubblici dei tenant attivi
SELECT
    kid AS 'Key ID',
    status AS 'Status',
    not_before_ts AS 'Valid From',
    not_after_ts AS 'Valid To',
    cert_pem AS 'Certificate PEM'
FROM signing_key
WHERE status = 'ACTIVE'
ORDER BY not_before_ts DESC;

-- Salva in un file
SELECT cert_pem
INTO OUTFILE '/tmp/tenant-certificates.crt'
FROM signing_key
WHERE kid = 'tenant-1-20260127';
```

## Best Practices

1. ✅ Usare certificati da una CA riconosciuta quando possibile
2. ✅ Includere SEMPRE la catena completa nel file P12
3. ✅ Configurare un servizio TSA per timestamp affidabili
4. ✅ Testare i PDF firmati in Adobe prima della produzione
5. ✅ Documentare il processo di installazione dei certificati per gli utenti finali

## Supporto

Per ulteriori informazioni o problemi:
- Verificare i log dell'applicazione per messaggi di debug sulla catena di certificati
- Controllare che tutti i certificati nella catena siano validi e non scaduti
- Contattare l'amministratore di sistema per assistenza con i certificati
