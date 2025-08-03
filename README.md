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
    implementation "com.r3edge:r3edge-mini-lock:0.1.0"
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    // À adapter selon votre base de données:
    runtimeOnly 'org.postgresql:postgresql'
    // ou runtimeOnly 'com.h2database:h2' pour les tests
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
  minilock:
    enabled: true      # true par défaut
    # ttl-seconds: 60  # Durée par défaut d’un lock (optionnel, défaut : 60s)
```

### Utiliser le service de lock dans votre code :

```java
import com.r3edge.minilock.ExecutionLockService;

@Service
@RequiredArgsConstructor
public class MonJob {

    private final ExecutionLockService lockService;

    public void execute() {
        if(lockService.acquireLock("MON_JOB", Duration.ofMinutes(5))) {
            try {
                // Placez ici votre code à protéger en mutex
            } finally {
                lockService.releaseLock("MON_JOB");
            }
        } else {
            // Un autre process détient déjà le lock, skip ou logguez à votre convenance
        }
    }
}
```

### Lancer votre service

> ℹ️ Au 1er démarrage, mini lock va créer la table de verrou automatiquement:

```sql
Hibernate: create table execution_lock (lock_expires_at timestamp(6), locked_at timestamp(6), updated_at timestamp(6), lock_detail varchar(50) check (lock_detail in ('NORMAL_RELEASE','TIMEOUT_EXPIRED','FORCE_RELEASE_BY_ADMIN','SYSTEM_SHUTDOWN','ERROR_DURING_PROCESS')), locked_by varchar(255), resource varchar(255) not null, status varchar(255) check (status in ('LOCKED','RELEASED')), primary key (resource))
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

---

📫 Maintenu par [@dsissoko](https://github.com/dsissoko) – contributions bienvenues.

[![Build and Test - r3edge-mini-lock](https://github.com/dsissoko/r3edge-mini-lock/actions/workflows/cicd_code.yml/badge.svg)](https://github.com/dsissoko/r3edge-mini-lock/actions/workflows/cicd_code.yml)

