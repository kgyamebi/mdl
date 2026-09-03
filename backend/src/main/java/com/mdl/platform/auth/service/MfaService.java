package com.mdl.platform.auth.service;

import com.mdl.platform.auth.dto.MfaSetupResponse;
import com.mdl.platform.auth.entity.MfaCredential;
import com.mdl.platform.auth.repository.MfaCredentialRepository;
import com.mdl.platform.common.exception.UnauthorizedException;
import com.mdl.platform.users.entity.User;
import com.mdl.platform.users.repository.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MfaService {

    private final MfaCredentialRepository mfaCredentialRepository;
    private final UserRepository userRepository;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public MfaService(MfaCredentialRepository mfaCredentialRepository, UserRepository userRepository) {
        this.mfaCredentialRepository = mfaCredentialRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MfaSetupResponse beginSetup(Long userId, String accountLabel) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        mfaCredentialRepository.deleteByUserIdAndPrimaryFalse(userId);

        String secret = secretGenerator.generate();
        MfaCredential credential = new MfaCredential();
        credential.setUserId(userId);
        credential.setCredentialType("TOTP");
        credential.setCredentialData("{\"secret\":\"" + secret + "\"}");
        credential.setPrimary(false);
        mfaCredentialRepository.save(credential);

        String otpAuthUrl = new QrData.Builder()
                .label(accountLabel)
                .secret(secret)
                .issuer("modern DL")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build()
                .getUri();

        return new MfaSetupResponse(secret, otpAuthUrl);
    }

    @Transactional
    public void confirmSetup(Long userId, String code) {
        MfaCredential pending = mfaCredentialRepository.findByUserId(userId).stream()
                .filter(credential -> !credential.isPrimary())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No MFA setup in progress"));

        if (!verifyCode(pending.getCredentialData(), code)) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        mfaCredentialRepository.findByUserId(userId).forEach(existing -> {
            existing.setPrimary(false);
            mfaCredentialRepository.save(existing);
        });

        pending.setPrimary(true);
        mfaCredentialRepository.save(pending);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    public boolean verifyUserCode(Long userId, String code) {
        MfaCredential credential = mfaCredentialRepository.findByUserIdAndPrimaryTrue(userId)
                .orElseThrow(() -> new UnauthorizedException("MFA is not configured"));
        return verifyCode(credential.getCredentialData(), code);
    }

    private boolean verifyCode(String credentialData, String code) {
        String secret = extractSecret(credentialData);
        return codeVerifier.isValidCode(secret, code);
    }

    private String extractSecret(String credentialData) {
        int start = credentialData.indexOf("\"secret\":\"");
        if (start < 0) {
            throw new IllegalArgumentException("Invalid MFA credential");
        }
        start += 10;
        int end = credentialData.indexOf('"', start);
        return credentialData.substring(start, end);
    }
}
