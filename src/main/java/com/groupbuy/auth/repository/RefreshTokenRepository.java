package com.groupbuy.auth.repository;

import com.groupbuy.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the {@code refresh_tokens} table.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes all non-revoked tokens belonging to the same rotation family.
     * Used for reuse detection: if a rotated token is replayed, the entire family is revoked.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.familyId = :familyId AND rt.revoked = false")
    int revokeAllByFamilyId(@Param("familyId") String familyId);

    /**
     * Revokes all non-revoked tokens for a given user (e.g. on forced logout-all).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);
}
