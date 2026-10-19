package com.web.repository;

import com.web.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findTopByEmailAndOtpTypeOrderByLocalDateTimeDesc(String email, com.web.enums.OtpType type);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update OtpCode o set o.used = true where o.email = ?1 and o.otpType = com.web.enums.OtpType.REGISTER_OTP and o.id <> ?2 and o.used = false")
    void invalidatePreviousRegistrationCodes(String email, Long currentId);

    @Query(value = "select * from otp_codes where email = ?1 and code = ?2 and used = false and otp_type = 'REGISTER_OTP' order by expiry_time desc limit 1",nativeQuery = true)
    Optional<OtpCode> findByEmailAndCodeAndUsedFalse(String email, String code);

    @Query(value = "select * from otp_codes where email = ?1 and code = ?2 and used = false and otp_type = 'RESET_PASS' order by expiry_time desc limit 1",nativeQuery = true)
    Optional<OtpCode> findByEmailAndCodeResetPass(String email, String code);


}
