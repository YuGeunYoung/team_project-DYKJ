package com.project.dykj.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.project.dykj.domain.member.entity.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, String>{

}
