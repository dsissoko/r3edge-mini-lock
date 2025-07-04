package com.r3edge.minilock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.r3edge.minilock.ExecutionLock.LockStatus;

/**
 * Repository JPA pour la gestion des verrous d'exécution
 * ({@link ExecutionLock}). Fournit des méthodes pour acquérir, mettre à jour,
 * libérer et nettoyer les verrous expirés.
 */
@Repository
public interface ExecutionLockRepository extends JpaRepository<ExecutionLock, String> {

	/**
	 * Acquiert un verrou sur une ressource en insérant une nouvelle entrée dans la
	 * table.
	 * 
	 * @param resource      La ressource à verrouiller.
	 * @param lockedAt      Date et heure de l'acquisition du verrou.
	 * @param updatedAt     Dernière mise à jour du verrou.
	 * @param lockExpiresAt Date et heure d'expiration du verrou.
	 * @param status        Statut initial du verrou (LOCKED ou RELEASED).
	 * @param lockedBy      Identifiant de l'instance qui acquiert le verrou.
	 * @return Nombre de lignes insérées (1 si succès, 0 sinon).
	 */
	@Query(value = "INSERT INTO execution_lock (resource, locked_at, updated_at, lock_expires_at, status, locked_by) "
			+ "VALUES (:resource, :lockedAt, :updatedAt, :lockExpiresAt, :status, :lockedBy)", nativeQuery = true)
	int acquireLock(@Param("resource") String resource, @Param("lockedAt") LocalDateTime lockedAt,
			@Param("updatedAt") LocalDateTime updatedAt, @Param("lockExpiresAt") LocalDateTime lockExpiresAt,
			@Param("status") String status, @Param("lockedBy") String lockedBy);

	/**
	 * Met à jour le statut d'un verrou existant.
	 * 
	 * @param resource  La ressource concernée.
	 * @param status    Le nouveau statut à appliquer.
	 * @param updatedAt Date et heure de mise à jour.
	 * @return Nombre de lignes mises à jour (1 si succès, 0 sinon).
	 */
	@Query("UPDATE ExecutionLock e SET e.status = :status, e.updated_at = :updatedAt WHERE e.resource = :resource")
	int updateStatus(@Param("resource") String resource, @Param("status") LockStatus status,
			@Param("updatedAt") LocalDateTime updatedAt);

	/**
	 * Libère un verrou détenu par un locker spécifique.
	 * 
	 * @param resource La ressource concernée.
	 * @param lockedBy Identifiant de l'instance qui détenait le verrou.
	 * @return Nombre de lignes supprimées (1 si succès, 0 sinon).
	 */
	@Query("DELETE FROM ExecutionLock e WHERE e.resource = :resource AND e.locked_by = :lockedBy")
	int releaseLock(@Param("resource") String resource, @Param("lockedBy") String lockedBy);

	/**
	 * Recherche un verrou valide (actif et non expiré) pour une ressource donnée.
	 * 
	 * @param resource La ressource concernée.
	 * @param now      Date et heure actuelles pour comparer l'expiration.
	 * @return Un {@link Optional} contenant le verrou s'il est encore actif.
	 */
	@Query("SELECT e FROM ExecutionLock e WHERE e.resource = :resource AND e.lock_expires_at > :now")
	Optional<ExecutionLock> findValidLock(@Param("resource") String resource, @Param("now") LocalDateTime now);

	/**
	 * Récupère la liste des verrous expirés (statut LOCKED mais expiration
	 * dépassée).
	 * 
	 * @param now Date et heure actuelles pour filtrer les verrous expirés.
	 * @return Liste des verrous expirés.
	 */
	@Query("SELECT e FROM ExecutionLock e WHERE e.status = 'LOCKED' AND e.lock_expires_at < :now")
	List<ExecutionLock> findExpiredLocks(@Param("now") LocalDateTime now);

	/**
	 * Supprime tous les verrous expirés de la base de données.
	 * 
	 * @param now Date et heure actuelles pour comparer l'expiration.
	 * @return Nombre de verrous supprimés.
	 */
	@Query("DELETE FROM ExecutionLock e WHERE e.lock_expires_at < :now")
	int deleteExpiredLocks(@Param("now") LocalDateTime now);
}
