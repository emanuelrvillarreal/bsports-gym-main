package com.blue.open.Core.Repository;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.Db.JwtRefreshToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface JwtRefreshTokenRepository extends EntityRepository<JwtRefreshToken> {
    Optional<JwtRefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}
