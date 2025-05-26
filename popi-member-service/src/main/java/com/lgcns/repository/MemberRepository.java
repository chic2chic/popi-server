package com.lgcns.repository;

import com.lgcns.domain.Member;
import com.lgcns.domain.OauthInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByOauthInfo(OauthInfo oauthInfo);

    Optional<Member> findByOauthInfo(OauthInfo oauthInfo);
}
