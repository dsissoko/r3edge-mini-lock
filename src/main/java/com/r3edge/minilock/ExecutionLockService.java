package com.r3edge.minilock;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.r3edge.minilock.ExecutionLock.LockDetail;
import com.r3edge.minilock.ExecutionLock.LockStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * Service de gestion des verrous d'exécution (ExecutionLock). Permet
 * d'acquérir, libérer et gérer automatiquement les verrous pour garantir
 * qu'une ressource n'est traitée que par une seule instance à la fois.
 */
@Service
@Slf4j
public class ExecutionLockService {

	@Autowired
	private ExecutionLockRepository executionLockRepository;

	private static final long DEFAULT_LOCK_DURATION_MILLI = 2000;

	/**
	 * Tente d'acquérir un verrou sur une ressource donnée.
	 *
	 * @param resource      La ressource à verrouiller.
	 * @param locker        Identifiant de l'instance qui acquiert le verrou.
	 * @param timeoutMillis Durée du verrou en millisecondes.
	 * @return true si le verrou est acquis avec succès, false sinon.
	 */
	public boolean acquireLock(String resource, String locker, long timeoutMillis) {
		Optional<ExecutionLock> existingLockOpt = executionLockRepository.findById(resource);

		if (existingLockOpt.isPresent()) {
			ExecutionLock existingLock = existingLockOpt.get();
			if (existingLock.getStatus() == LockStatus.LOCKED && !existingLock.isExpired()) {
				log.warn("⛔ Lock déjà actif pour {} par {}", resource, existingLock.getLocked_by());
				return false;
			}
			log.info("♻️ Lock existant mais réutilisable pour {}, ancien statut: {}", resource, existingLock.getStatus());
		}

		if (timeoutMillis <= 0) {
			timeoutMillis = DEFAULT_LOCK_DURATION_MILLI;
		}

		ExecutionLock newLock = new ExecutionLock(resource, locker, timeoutMillis);
		executionLockRepository.save(newLock);
		log.info("✅ Lock acquis pour {} par {}", resource, locker);
		return true;
	}

	/**
	 * Libère un verrou détenu par une instance spécifique.
	 *
	 * @param resource La ressource à libérer.
	 * @param locker   Identifiant de l'instance qui libère le verrou.
	 * @param reason   Raison de la libération.
	 * @return true si la libération a réussi, false sinon.
	 */
	public boolean releaseLock(String resource, String locker, LockDetail reason) {
		Optional<ExecutionLock> existingLockOpt = executionLockRepository.findById(resource);

		if (existingLockOpt.isEmpty()) {
			log.warn("⚠️ Tentative de libération d'un lock inexistant : {}", resource);
			return false;
		}

		ExecutionLock existingLock = existingLockOpt.get();

		if (existingLock.getStatus() == LockStatus.RELEASED) {
			log.warn("⚠️ Lock déjà libéré pour {} par {}", resource, existingLock.getLocked_by());
			return false;
		}

		if (!existingLock.getLocked_by().equals(locker)) {
			log.warn("⛔ Tentative de libération d'un lock par un autre locker ! {} ≠ {}", locker, existingLock.getLocked_by());
			return false;
		}

		existingLock.updateStatus(LockStatus.RELEASED, reason);
		existingLock.setUpdated_at(LocalDateTime.now());
		executionLockRepository.save(existingLock);

		log.info("✅ Lock libéré avec succès pour {} par {}", resource, locker);
		return true;
	}

	public boolean releaseLockNormal(String resource, String locker) {
		return releaseLock(resource, locker, LockDetail.NORMAL_RELEASE);
	}

	public boolean releaseLockTimeout(String resource) {
		return releaseLock(resource, "SYSTEM", LockDetail.TIMEOUT_EXPIRED);
	}

	public boolean forceReleaseLock(String resource, String adminUser) {
		return releaseLock(resource, adminUser, LockDetail.FORCE_RELEASE_BY_ADMIN);
	}

	public boolean releaseLockOnShutdown(String resource, String locker) {
		return releaseLock(resource, locker, LockDetail.SYSTEM_SHUTDOWN);
	}

	/**
	 * Tâche planifiée exécutée toutes les 15 minutes pour libérer les verrous expirés.
	 */
	@Scheduled(fixedRate = 900_000)
	public void autoReleaseExpiredLocks() {
		LocalDateTime now = LocalDateTime.now();
		List<ExecutionLock> expiredLocks = executionLockRepository.findExpiredLocks(now);

		if (expiredLocks.isEmpty()) {
			log.info("✅ Aucun lock expiré à libérer.");
			return;
		}

		log.info("🔍 {} locks expirés détectés, libération en cours...", expiredLocks.size());

		for (ExecutionLock lock : expiredLocks) {
			log.info("⏳ Lock expiré détecté : {} détenu par {}", lock.getResource(), lock.getLocked_by());
			releaseLockTimeout(lock.getResource());
		}

		log.info("✅ Tous les locks expirés ont été libérés.");
	}

	/**
	 * Tente d'acquérir un verrou et exécute le bloc de code fourni si le verrou est acquis.
	 * Libère automatiquement le verrou après l'exécution.
	 *
	 * @param resource   Nom de la ressource à verrouiller.
	 * @param expiration Durée du verrou avant expiration.
	 * @param task       Bloc de code à exécuter si le verrou est obtenu.
	 * @return true si le bloc a été exécuté, false sinon.
	 */
	public boolean runIfUnlocked(String resource, Duration expiration, Runnable task) {
		String lockerId = getLockerId();

		boolean acquired = acquireLock(resource, lockerId, expiration.toMillis());

		if (!acquired) {
			log.warn("⛔ Impossible d'acquérir le lock pour {}", resource);
			return false;
		}

		try {
			log.info("▶️ Exécution protégée de la tâche '{}'", resource);
			task.run();
			return true;
		} catch (Exception e) {
			log.error("❌ Erreur pendant l’exécution protégée de {}", resource, e);
			return false;
		} finally {
			releaseLockNormal(resource, lockerId);
		}
	}

	/**
	 * Variante de {@link #runIfUnlocked} qui retourne une valeur via {@link Callable}
	 * et permet de propager les exceptions.
	 *
	 * @param resource   Nom de la ressource à verrouiller.
	 * @param expiration Durée du verrou.
	 * @param task       Bloc de code à exécuter.
	 * @param <T>        Type de la valeur retournée.
	 * @return La valeur retournée par le bloc, ou {@code null} si non exécuté.
	 * @throws Exception si le bloc lève une exception.
	 */
	public <T> T runIfUnlockedThrowing(String resource, Duration expiration, Callable<T> task) throws Exception {
		String lockerId = getLockerId();

		boolean acquired = acquireLock(resource, lockerId, expiration.toMillis());

		if (!acquired) {
			log.warn("⛔ Impossible d'acquérir le lock pour {}", resource);
			return null;
		}

		try {
			log.info("▶️ Exécution protégée (Callable) de '{}'", resource);
			return task.call();
		} finally {
			releaseLockNormal(resource, lockerId);
		}
	}

	/**
	 * Fournit l'identifiant de l'instance courante (peut être externalisé plus tard).
	 *
	 * @return identifiant unique de l'instance (hostname ou ID technique).
	 */
	private String getLockerId() {
		return System.getenv().getOrDefault("LOCKER_ID", "default-locker");
	}
}
