package wad.seoul_nolgoat.web.inquiry.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InquiryListDto {

    private final Long id;
    private final String title;
    private final Boolean isPublic;
    private final Long userId;
    private final String userNickname;
    private final String userProfileImage;
    private final String createDate;
}
