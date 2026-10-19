package com.web.config;

import com.web.doss.LoginAttemptService;
import com.web.entity.User;
import com.web.enums.*;
import com.web.exception.UserNotActivatedException;
import com.web.repository.*;
import com.web.utils.UserUtils;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthFlowTests {
    UserRepository users;User user;MockHttpServletRequest request;MockHttpServletResponse response;
    @BeforeEach void setup() {
        users=mock(UserRepository.class);user=new User();user.setId(42L);user.setEmail("customer@example.test");
        user.setActived(true);user.setActivationStatus(true);user.setRole(Role.ROLE_USER);user.setUserType(UserType.NORMAL);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        request=new MockHttpServletRequest();request.setScheme("http");request.setServerName("localhost");request.setServerPort(80);
        response=new MockHttpServletResponse();
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
    CustomLoginSuccessHandler handler() {
        CustomLoginSuccessHandler h=new CustomLoginSuccessHandler();ReflectionTestUtils.setField(h,"userRepository",users);
        ReflectionTestUtils.setField(h,"loginAttemptService",mock(LoginAttemptService.class));return h;
    }
    UsernamePasswordAuthenticationToken auth(){return new UsernamePasswordAuthenticationToken(user.getEmail(),"",List.of(new SimpleGrantedAuthority(user.getRole().name())));}
    void save(String host,String path) {
        request.setMethod("GET");request.setServerName(host);request.setRequestURI(path);
        new HttpSessionRequestCache().saveRequest(request,response);
        request.setServerName("localhost");request.setMethod("POST");request.setRequestURI("/perform_login");
    }
    @Test void pendingAccountCannotBypassActivationUsingASavedUrl() throws Exception {
        save("localhost","/tai-khoan");user.setActivationStatus(false);SecurityContextHolder.getContext().setAuthentication(auth());
        handler().onAuthenticationSuccess(request,response,auth());
        assertTrue(response.getRedirectedUrl().startsWith("/verify-otp?email="));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void customerIsNotRedirectedBackToAnAdminPage() throws Exception {
        save("localhost","/adm-prj/qtht-001/product/save");handler().onAuthenticationSuccess(request,response,auth());
        assertEquals("/",response.getRedirectedUrl());
    }
    @Test void savedStatic404PathDoesNotTrapLogin() throws Exception {
        save("localhost","/image/logo.jpg");handler().onAuthenticationSuccess(request,response,auth());
        assertEquals("/",response.getRedirectedUrl());
    }
    @Test void savedExternalHostIsRejected() throws Exception {
        save("evil.example","/tai-khoan");handler().onAuthenticationSuccess(request,response,auth());
        assertEquals("/",response.getRedirectedUrl());
    }
    @Test void safeSavedAccountPageIsRestored() throws Exception {
        save("localhost","/tai-khoan");handler().onAuthenticationSuccess(request,response,auth());
        assertEquals("/tai-khoan",java.net.URI.create(response.getRedirectedUrl()).getPath());
    }
    @Test void adminHasTheAdminLandingPage() throws Exception {
        user.setRole(Role.ROLE_ADMIN);handler().onAuthenticationSuccess(request,response,auth());
        assertEquals("/adm-prj/qtht-001/invoice/list",response.getRedirectedUrl());
    }
    @Test void googlePrincipalAndUserIdUseEmailNotDisplayName() {
        var raw=new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),Map.of("sub","id","name","Not An Email","email"," CUSTOMER@EXAMPLE.TEST "),"sub");
        var principal=new CustomOAuth2User(raw,raw.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(new OAuth2AuthenticationToken(principal,principal.getAuthorities(),"google"));
        UserUtils utils=new UserUtils();ReflectionTestUtils.setField(utils,"userRepository",users);
        assertEquals("customer@example.test",principal.getName());assertEquals(42L,utils.getIdUserLogged());
    }
    @Test void pendingLoginFailureRedirectsWithoutGrantingAuthentication() throws Exception {
        CustomAuthenticationFailureHandler h=new CustomAuthenticationFailureHandler();
        h.onAuthenticationFailure(request,response,new UserNotActivatedException("pending","customer+tag@example.test"));
        assertEquals("/verify-otp?email=customer%2Btag%40example.test",response.getRedirectedUrl());
    }
    @Test void unconfiguredGoogleLaunchReturnsFriendlyLoginInsteadOfStartingOAuth() throws Exception {
        request.setServletPath("/oauth2/authorization/google");MockFilterChain chain=new MockFilterChain();
        new OAuthAvailabilityFilter(new MockEnvironment()).doFilter(request,response,chain);
        assertEquals("/login",response.getRedirectedUrl());assertNull(chain.getRequest());
    }
    @Test void configuredGoogleLaunchContinuesNormally() throws Exception {
        request.setServletPath("/oauth2/authorization/google");MockFilterChain chain=new MockFilterChain();
        MockEnvironment env=new MockEnvironment().withProperty("spring.security.oauth2.client.registration.google.client-id","test-id")
                .withProperty("spring.security.oauth2.client.registration.google.client-secret","test-secret");
        new OAuthAvailabilityFilter(env).doFilter(request,response,chain);assertNotNull(chain.getRequest());
    }
    @Test void pendingRegistrationIsNotAnAuthenticatedUserDetailsAccount() {
        PendingRegistrationRepository pending=mock(PendingRegistrationRepository.class);
        when(pending.existsById("new@example.test")).thenReturn(true);
        WebSecurityConfig config=new WebSecurityConfig();ReflectionTestUtils.setField(config,"userRepository",users);
        ReflectionTestUtils.setField(config,"pendingRegistrationRepository",pending);
        assertThrows(UserNotActivatedException.class,()->config.userDetailsService().loadUserByUsername(" NEW@EXAMPLE.TEST "));
    }
    @Test void legacyNullActivationCannotBypassCustomerVerification() {
        user.setActivationStatus(null);
        WebSecurityConfig config=new WebSecurityConfig();ReflectionTestUtils.setField(config,"userRepository",users);
        assertThrows(UserNotActivatedException.class,()->config.userDetailsService().loadUserByUsername(user.getEmail()));
    }
    @Test void disabledPendingAccountIsNotPresentedAsAnActivationOpportunity() {
        user.setActivationStatus(false);user.setActived(false);
        WebSecurityConfig config=new WebSecurityConfig();ReflectionTestUtils.setField(config,"userRepository",users);
        assertThrows(com.web.exception.LoginException.class,()->config.userDetailsService().loadUserByUsername(user.getEmail()));
    }
}
