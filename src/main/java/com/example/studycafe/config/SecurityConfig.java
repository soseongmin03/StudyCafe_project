package com.example.studycafe.config;

import com.example.studycafe.repository.MemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import java.util.Collections;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean // 회원정보로 로그인
    public UserDetailsService userDetailsService(MemberRepository memberRepository) {
        return username -> {
            var member = memberRepository.findByEmail(username).orElseThrow(() ->
                    new UsernameNotFoundException("User not found" + username));
            System.out.println("======================================");
            System.out.println("로그인 시도 이메일: " + username);
            System.out.println("DB에 저장된 권한: [" + member.getRole() + "]");
            System.out.println("======================================");
            // [수정 포인트] 권한을 더 명확하게 부여하는 코드로 변경
            return new User(
                    member.getEmail(),
                    member.getPassword(),
                    Collections.singleton(new SimpleGrantedAuthority(member.getRole()))
            );
        };
        /*return username -> {
            var member = memberRepository.findByEmail(username).orElseThrow(() ->
                    new UsernameNotFoundException("User not found" + username));
            return User.builder()
                    .username(username)
                    .password(member.getPassword())
                    .authorities(member.getRole()).build();
        };*/
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/home","/","/signup").permitAll()
                        .requestMatchers("/book/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers("/member/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/book",true) //로그인 성공후 이동
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout") //로그아웃성공
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                );
        http.sessionManagement(session -> session
                .maximumSessions(1)             // 한 아이디당 최대 허용 세션 수
                .maxSessionsPreventsLogin(false) // true면 신규 로그인 차단, false면 기존 로그인 만료
        );
        return http.build();
    }
}
