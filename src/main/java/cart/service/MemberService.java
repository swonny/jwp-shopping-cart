package cart.service;

import cart.dao.MemberDao;
import cart.dao.entity.MemberEntity;
import cart.dto.response.ResponseMemberDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final MemberDao memberDao;

    @Autowired
    public MemberService(MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    public List<ResponseMemberDto> findAll() {
        List<MemberEntity> memberEntities = memberDao.findAll();
        return memberEntities.stream()
                .map(memberEntity -> ResponseMemberDto.transferEntityToDto(memberEntity))
                .collect(Collectors.toList());
    }
}
