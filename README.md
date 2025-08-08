# r3edge-mini-lock | ![Logo](logo_ds.png)

**Librairie Java pour permettre une exécution mutex de vos charges de travail dans le cloud.**

> 🚀 Pourquoi adopter `r3edge-mini-lock` ?
> 
> ✅ **Autoconfiguration** avec création automatique de la table de gestion des locks  
> ✅ 1 **API ultra-simple** : Demandez un lock par nom, exécutez, relâchez, c’est tout  
> ✅ **100 % Spring Boot 3.x** (utilise JPA pour la persistence)   
> ✅ Vous permet de couvrir **simplement** les principaux Cloud Design Patterns de gestion concurrente :
>  - [Scheduler Agent Supervisor](https://learn.microsoft.com/en-us/azure/architecture/patterns/scheduler-agent-supervisor)  
>  - [Leader Election](https://learn.microsoft.com/en-us/azure/architecture/patterns/leader-election)  
>  - [Competing Consumers](https://learn.microsoft.com/en-us/azure/architecture/patterns/competing-consumers)  
>
> ✅ Parfaitement complémentaire avec [`r3edge-task-dispatcher`](https://github.com/dsissoko/r3edge-task-dispatcher) pour garantir l’exclusivité des tâches planifiées en environnement distribué.  

This project is documented in French 🇫🇷 by default.  
An auto-translated English version is available here:

[👉 English (auto-translated by Google)](https://translate.google.com/translate?sl=auto&tl=en&u=https://github.com/dsissoko/r3edge-mini-lock)

---

## 📋 Fonctionnalités clés

- ✅ Pose d'un verrou en 1 ligne de code
- ✅ Libération explicite du lock ou expiration automatique  
- ✅ Nettoyage automatique des locks expirés avec délai d'expiration configurable  
- ✅ Compatible avec toutes les bases supportées par Spring Data JPA  
- ✅ Prêt à l’emploi grâce à l’autoconfiguration Spring Boot  

---

## ⚙️ Intégration rapide

### Ajouter les dépendances nécessaires :

```groovy
repositories {
    mavenCentral()
    // Dépôt GitHub Packages de r3edge-mini-lock
    maven {
        url = uri("https://maven.pkg.github.com/dsissoko/r3edge-mini-lock")
        credentials {
            username = ghUser
            password = ghKey
        }
    }
    mavenLocal()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // Base de données
    implementation 'org.postgresql:postgresql:42.7.7'
    
    // Pour mini-lock
    implementation "com.r3edge:r3edge-mini-lock:0.0.7"
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    testImplementation 'com.h2database:h2'
}
```

> ⚠️ Cette librairie est publiée sur **GitHub Packages**: Même en open source, **GitHub impose une authentification** pour accéder aux dépendances.  
> Il faudra donc valoriser ghUser et ghKey dans votre gradle.properties:

```properties
#pour récupérer des packages github 
ghUser=your_github_user
ghKey=github_token_with_read_package_scope
```

### Configurer le **datasource** puis Activer mini-lock dans votre configuration Spring Boot:

```yaml
r3edge:
  minilock: # pas de configuration à faire pour l'instant
    # cleanup_expired_frequency: 10*60 # par défaut 15 minutes
    
spring:
  datasource:
    url: jdbc:postgresql://mainline.proxy.rlwy.net:24622/railway
    username: app_user
    password: big_strong_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

```

> ⚠️ Pour bénéficier de la fonction de nettoyage automatique des verrous, nous devez **configurer votre app Spring Boot avec @EnableScheduling**.  

### Utiliser le service de lock dans votre code :

```java
package com.example.demo;

import java.time.Duration;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.r3edge.minilock.ExecutionLockService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemoService {

    private static final String RESOURCE_TEASER = "teaser-ai-extraction";

    private final ExecutionLockService lockService;

    @Value("${spring.application.name:default-locker}")
    private String locker;

    // 👁‍🗨 MÉTHODE "MÉTIER" À PROTÉGER ABSOLUMENT
    private void videoTeasingAIExtraction() {
        log.info("🎬 Extraction teaser vidéo par IA en cours...");
        // Simulation d'un gros traitement IA
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("✅ Teaser généré !");
    }

    // 1️⃣ - Acquire/release manuel
    public boolean extractTeaser_acquireRelease() {
        long timeout = 60000;

        boolean locked = lockService.acquireLock(RESOURCE_TEASER, locker, timeout);
        if (locked) {
            try {
                videoTeasingAIExtraction();
                return true;
            } finally {
                lockService.releaseLockNormal(RESOURCE_TEASER, locker);
            }
        } else {
            log.warn("⛔ Teaser déjà en cours sur cette vidéo !");
            return false;
        }
    }

    // 2️⃣ - Protection auto via runIfUnlocked (Runnable)
    public boolean extractTeaser_runIfUnlocked() {
        return lockService.runIfUnlocked(
            RESOURCE_TEASER,
            Duration.ofSeconds(30),
            this::videoTeasingAIExtraction
        );
    }

    // 3️⃣ - Protection auto via runIfUnlockedThrowing (Callable<T>)
    public String extractTeaser_runIfUnlockedThrowing() throws Exception {
        return lockService.runIfUnlockedThrowing(
            RESOURCE_TEASER,
            Duration.ofSeconds(30),
            (Callable<String>) () -> {
                videoTeasingAIExtraction();
                return "Teaser généré (avec retour)";
            }
        );
    }

    // Ajout d'un hook pour libérer le lock sur shutdown (ex: dernier teaser traité)
    @PostConstruct
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("🛑 Shutdown hook: libération du lock pour resource={}", RESOURCE_TEASER);
            boolean released = lockService.releaseLockOnShutdown(RESOURCE_TEASER, locker);
            if (released) {
                log.info("Lock sur '{}' libéré proprement à l'arrêt.", RESOURCE_TEASER);
            } else {
                log.warn("Aucun lock à libérer pour '{}', ou déjà libéré.", RESOURCE_TEASER);
            }
        }));
    }
}
```

### Lancer votre service

> ℹ️ Au 1er démarrage, mini lock va créer la table de verrou automatiquement:

```sql
Hibernate: create table execution_lock (lock_expires_at timestamp(6), locked_at timestamp(6), updated_at timestamp(6), lock_detail varchar(50) check (lock_detail in ('NORMAL_RELEASE','TIMEOUT_EXPIRED','FORCE_RELEASE_BY_ADMIN','SYSTEM_SHUTDOWN','ERROR_DURING_PROCESS')), locked_by varchar(255), resource varchar(255) not null, status varchar(255) check (status in ('LOCKED','RELEASED')), primary key (resource))
```

> Mini lock affiche des logs explicites en cas d'échec d'acquisition de verrou

```test
2025-08-03T18:13:00.437+02:00  INFO 10484 --- [r3edge-mini-lock-starter] [       Thread-8] c.r3edge.minilock.ExecutionLockService  : ✅ Lock acquis pour teaser-ai-extraction par r3edge-mini-lock-starter
2025-08-03T18:13:00.445+02:00  INFO 10484 --- [r3edge-mini-lock-starter] [       Thread-8] com.example.demo.DemoService            : 🎬 Extraction teaser vidéo par IA en cours...
2025-08-03T18:13:00.472+02:00 ERROR 10484 --- [r3edge-mini-lock-starter] [      Thread-10] o.h.engine.jdbc.spi.SqlExceptionHelper  : ERROR: duplicate key value violates unique constraint "execution_lock_pkey"
  Détail : Key (resource)=(teaser-ai-extraction) already exists.
2025-08-03T18:13:00.472+02:00 ERROR 10484 --- [r3edge-mini-lock-starter] [       Thread-9] o.h.engine.jdbc.spi.SqlExceptionHelper  : ERROR: duplicate key value violates unique constraint "execution_lock_pkey"
  Détail : Key (resource)=(teaser-ai-extraction) already exists.
2025-08-03T18:13:00.649+02:00  WARN 10484 --- [r3edge-mini-lock-starter] [      Thread-10] c.r3edge.minilock.ExecutionLockService  : 
❌ LOCK CONCURRENT ACQUISITION FAILED
  - Resource     : teaser-ai-extraction
  - Locker lost  : default-locker
  - Locker owner : r3edge-mini-lock-starter
  - Status       : LOCKED

2025-08-03T18:13:00.649+02:00  WARN 10484 --- [r3edge-mini-lock-starter] [      Thread-10] c.r3edge.minilock.ExecutionLockService  : ⛔ Impossible d'acquérir le lock pour teaser-ai-extraction
2025-08-03T18:13:00.649+02:00  INFO 10484 --- [r3edge-mini-lock-starter] [      Thread-10] com.example.demo.DemoConcurrentRunner   : Thread 3 (runIfUnlockedThrowing): LOCK FAIL
2025-08-03T18:13:00.650+02:00  WARN 10484 --- [r3edge-mini-lock-starter] [       Thread-9] c.r3edge.minilock.ExecutionLockService  : 
❌ LOCK CONCURRENT ACQUISITION FAILED
  - Resource     : teaser-ai-extraction
  - Locker lost  : default-locker
  - Locker owner : r3edge-mini-lock-starter
  - Status       : LOCKED

2025-08-03T18:13:00.651+02:00  WARN 10484 --- [r3edge-mini-lock-starter] [       Thread-9] c.r3edge.minilock.ExecutionLockService  : ⛔ Impossible d'acquérir le lock pour teaser-ai-extraction
2025-08-03T18:13:00.651+02:00  INFO 10484 --- [r3edge-mini-lock-starter] [       Thread-9] com.example.demo.DemoConcurrentRunner   : Thread 2 (runIfUnlocked): LOCK FAIL

```

---

## 📦 Stack de référence

✅ Cette librairie a été conçue et testée avec :

- Java 17+
- Spring Boot 3.x
- Spring Data JPA
- Base de données SQL supportée (PostgreSQL, H2, MySQL, etc.)

---

## 🗺️ Roadmap

### 🔧 À venir
- Rien: mini lock est volontairement minimaliste

### 🧠 En réflexion
- Proposer d'avantage d'options de configuration
- Messages i18n

---

📫 Maintenu par [@dsissoko](https://github.com/dsissoko) – contributions bienvenues.

[![Build and Test - r3edge-mini-lock](https://github.com/dsissoko/r3edge-mini-lock/actions/workflows/cicd_code.yml/badge.svg)](https://github.com/dsissoko/r3edge-mini-lock/actions/workflows/cicd_code.yml)

