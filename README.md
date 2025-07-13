# r3edge-mini-lock

Verrou distribué minimaliste via base SQL pour garantir l’unicité d’exécution des tâches critiques en environnement multi-instance (Spring Boot, Kubernetes, etc.).

---

## ✅ Fonctionnalités

- Verrouillage de ressource basé sur une table SQL
- Compatible Spring Boot, JPA et environnements distribués (Docker, Kubernetes…)
- Gestion fine du cycle de vie d’un verrou (acquisition, mise à jour, libération, expiration)
- Intégration simple via `@Repository` Spring Data
- Ne nécessite aucune dépendance tierce propriétaire

---

## 🧩 Utilisation

### Entité fournie : `ExecutionLock`

Elle gère :

- Le nom de la ressource
- L’instance qui la verrouille
- Le timestamp d’acquisition
- La date d’expiration
- Le statut (`LOCKED`, `RELEASED`)
- Le motif de libération (`NORMAL_RELEASE`, `TIMEOUT_EXPIRED`, etc.)

### Repository fourni : `ExecutionLockRepository`

Interface Spring Data prête à l’emploi avec méthodes natives pour :

- Acquérir un verrou (`acquireLock`)
- Libérer ou mettre à jour un verrou (`releaseLock`, `updateStatus`)
- Nettoyer les verrous expirés (`deleteExpiredLocks`)

---

## 📦 Compatibilité

✅ Testée avec :  
- **Spring Boot** `3.5.3`  
- **Spring Cloud** `2025.0.0`  
- **Java** `17` et `21`

🧘 Lib légère, sans dépendance transitive aux starters : fonctionne avec toute stack Spring moderne.  
Pas de `fat-jar`, pas de verrouillage.

---

## 🔧 Intégration dans un projet Spring Boot

Ajoutez la dépendance :

```groovy
dependencies {
    implementation "com.r3edge:r3edge-mini-lock:0.0.3"
    implementation "org.springframework.boot:spring-boot-starter-data-jpa"
    runtimeOnly "org.postgresql:postgresql" // ou autre driver JDBC
}
```

Et configurez votre datasource dans `application.yml` :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ma_base
    username: user
    password: password
  jpa:
    hibernate:
      ddl-auto: update
```

---

## 🧪 Tests

La bibliothèque inclut une configuration de test basée sur H2 pour valider les comportements.

---

## 📦 Publication

Cette bibliothèque est publiée sur GitHub Packages.

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/dsissoko/r3edge-mini-lock")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USER")
            password = project.findProperty("gpr.key") ?: System.getenv("TWEAKED_PAT")
        }
    }
}
```

---

[![Build and Test - r3edge-mini-lock](https://github.com/dsissoko/r3edge-mini-lock/actions/workflows/cicd_code.yml/badge.svg)](https://github.com/dsissoko/r3edge-mini-lock/actions/workflows/cicd_code.yml)
