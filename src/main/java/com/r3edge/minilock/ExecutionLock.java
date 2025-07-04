package com.r3edge.minilock;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Entité représentant un verrou d'exécution dans la base de données. Utilisée
 * pour gérer l'accès concurrent aux ressources critiques.
 * 
 * <p>
 * Un lock est créé lorsqu'une ressource est en cours d'utilisation et peut être
 * libéré normalement, forcé par un administrateur ou automatiquement après
 * expiration.
 * </p>
 */
@Table(name = "execution_lock")
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Data
public class ExecutionLock {

	/** Durée maximale par défaut du verrou en minutes. */
	public static final int DEFAULT_MAX_LOCK_MINUTES_DURATION = 2;

	/** Identifiant unique de la ressource verrouillée. */
	@Id
	@NonNull
	private String resource;

	/** Horodatage du moment où le verrou a été acquis. */
	@NonNull
	private LocalDateTime locked_at;

	/** Dernière mise à jour du verrou. */
	@NonNull
	private LocalDateTime updated_at;

	/** Date d'expiration du verrou. */
	@NonNull
	private LocalDateTime lock_expires_at;

	/** Statut actuel du verrou (LOCKED ou RELEASED). */
	@Enumerated(EnumType.STRING)
	@NonNull
	private LockStatus status;

	/** Identifiant de l'instance qui détient le verrou. */
	@NonNull
	private String locked_by;

	/**
	 * Détail de la libération du verrou (ex: expiration, libération normale, etc.).
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 50, nullable = true)
	private LockDetail lockDetail;

	/**
	 * Constructeur pour créer un verrou avec la durée par défaut.
	 * 
	 * @param resourceToLock Nom de la ressource à verrouiller.
	 * @param locker         Identifiant de l'instance qui détient le verrou.
	 */
	public ExecutionLock(String resourceToLock, String locker) {
		this(resourceToLock, locker, DEFAULT_MAX_LOCK_MINUTES_DURATION);
	}

	/**
	 * Constructeur permettant de définir une durée de verrouillage personnalisée.
	 * 
	 * @param resourceToLock  Nom de la ressource à verrouiller.
	 * @param locker          Identifiant de l'instance qui détient le verrou.
	 * @param expirationMilli Durée du verrou en millisecondes avant expiration.
	 */
	public ExecutionLock(String resourceToLock, String locker, long expirationMilli) {
		this.resource = resourceToLock;
		this.locked_at = LocalDateTime.now();
		this.updated_at = locked_at;
		this.status = LockStatus.LOCKED;
		this.locked_by = locker;
		this.lock_expires_at = this.locked_at.plusNanos(expirationMilli * 1_000_000L);
	}

	/**
	 * Met à jour le statut du verrou et son détail de libération.
	 * 
	 * @param status     Nouveau statut du verrou (LOCKED, RELEASED).
	 * @param lockDetail Raison de la libération du verrou.
	 * @return L'instance mise à jour de {@code ExecutionLock}.
	 */
	public ExecutionLock updateStatus(LockStatus status, LockDetail lockDetail) {
		this.status = status;
		this.lockDetail = lockDetail;
		this.updated_at = LocalDateTime.now(); // Mise à jour de l'horodatage
		return this;
	}

	/**
	 * Vérifie si le verrou est expiré.
	 * 
	 * @return {@code true} si le verrou est expiré, sinon {@code false}.
	 */
	public boolean isExpired() {
		return LocalDateTime.now().isAfter(lock_expires_at);
	}

	/**
	 * Enum représentant les statuts possibles d'un verrou.
	 */
	/**
	 * Statut d’un verrou d’exécution.
	 */
	public enum LockStatus {

	    /**
	     * La ressource est actuellement verrouillée.
	     */
	    LOCKED,

	    /**
	     * La ressource a été libérée.
	     */
	    RELEASED
	}

	/**
	 * Enum décrivant la raison pour laquelle un verrou a été libéré.
	 */
	public enum LockDetail {

	    /**
	     * Verrou libéré normalement après l’exécution.
	     */
	    NORMAL_RELEASE,

	    /**
	     * Verrou libéré automatiquement car le timeout a expiré.
	     */
	    TIMEOUT_EXPIRED,

	    /**
	     * Verrou libéré manuellement par un administrateur.
	     */
	    FORCE_RELEASE_BY_ADMIN,

	    /**
	     * Verrou libéré automatiquement à l’arrêt du système.
	     */
	    SYSTEM_SHUTDOWN,

	    /**
	     * Verrou libéré à cause d’une erreur pendant le traitement.
	     */
	    ERROR_DURING_PROCESS
	}
}
