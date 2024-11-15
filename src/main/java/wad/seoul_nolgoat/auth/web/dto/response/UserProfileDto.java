package wad.seoul_nolgoat.auth.web.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserProfileDto {

    private final Long userId;
    private final String loginId;
    private final String nickname;
    private final String profileImage;
}