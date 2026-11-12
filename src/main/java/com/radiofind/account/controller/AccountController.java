package com.radiofind.account.controller;

import com.radiofind.account.dto.BackupCodesResponse;
import com.radiofind.account.dto.ConfirmTwoFactorEnrollmentRequest;
import com.radiofind.account.dto.PasswordConfirmationRequest;
import com.radiofind.account.dto.SecurityOverviewResponse;
import com.radiofind.account.dto.StartTwoFactorEnrollmentRequest;
import com.radiofind.account.dto.TwoFactorEnrollmentResponse;
import com.radiofind.account.service.AccountDeletionService;
import com.radiofind.account.service.AccountSecurityService;
import com.radiofind.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class AccountController {

  private final AccountSecurityService accountSecurityService;
  private final AccountDeletionService accountDeletionService;

  @GetMapping("/security")
  public SecurityOverviewResponse getSecurityOverview(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return accountSecurityService.getOverview(userDetails);
  }

  @PostMapping("/security/two-factor/enrollment")
  public TwoFactorEnrollmentResponse startTwoFactorEnrollment(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody StartTwoFactorEnrollmentRequest request) {
    return accountSecurityService.startEnrollment(userDetails, request);
  }

  @PostMapping("/security/two-factor/enrollment/confirm")
  public SecurityOverviewResponse confirmTwoFactorEnrollment(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ConfirmTwoFactorEnrollmentRequest request) {
    return accountSecurityService.confirmEnrollment(userDetails, request.code());
  }

  @PostMapping("/security/backup-codes")
  public BackupCodesResponse regenerateBackupCodes(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody PasswordConfirmationRequest request) {
    return accountSecurityService.regenerateBackupCodes(userDetails, request.password());
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAccount(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody PasswordConfirmationRequest request) {
    accountDeletionService.delete(userDetails, request.password());
  }
}
