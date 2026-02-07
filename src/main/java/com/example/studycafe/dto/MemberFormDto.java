package com.example.studycafe.dto;

import com.example.studycafe.model.Member;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberFormDto {
    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.") // xxxxx@xxxx.xxx 형식 검증
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.") // 6자리 이상 조건
    private String password;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    @Min(value = 1, message = "나이는 1살 이상이어야 합니다.") // 1살 이상 조건
    private Integer age;

    // 역할은 시스템이 부여하므로 DTO에는 없고 여기서 기본값 "USER"를 설정합니다.
    public Member toEntity(String encodedPassword) {
        return Member.builder()
                .email(this.email)
                .password(encodedPassword) // 암호화된 비밀번호 주입
                .name(this.name)
                .age(this.age)
                .role("ROLE_USER") // 기본 권한 설정
                .build();
    }
}