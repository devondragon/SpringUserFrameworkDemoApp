package com.digitalsanctuary.spring.demo.user.ui.page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.*;

/**
 * Page object for registration pending verification page
 */
public class VerificationPendingPage {

    private final SelenideElement VERIFICATION_MESSAGE = Selenide.$x("//div[contains(@class, 'alert')]");
    private final SelenideElement RESEND_VERIFICATION_LINK = Selenide.$x("//a[contains(@href, 'resend')]");
    private final SelenideElement LOGIN_LINK = Selenide.$x("//a[contains(@href, 'login')]");

    public VerificationPendingPage(String url) {
        Selenide.open(url);
    }

    public VerificationPendingPage() {
        // For navigation to this page from other pages
    }

    /**
     * Get the verification pending message
     */
    public String getVerificationMessage() {
        return VERIFICATION_MESSAGE.shouldBe(visible).text();
    }

    /**
     * Click the resend verification email link
     */
    public VerificationPendingPage resendVerificationEmail() {
        RESEND_VERIFICATION_LINK.click();
        return this;
    }

    /**
     * Navigate to login page
     */
    public LoginPage goToLogin() {
        LOGIN_LINK.click();
        return new LoginPage("http://localhost:8080/user/login.html");
    }

    /**
     * Check if the verification message is displayed
     */
    public boolean isVerificationMessageDisplayed() {
        return VERIFICATION_MESSAGE.exists() && VERIFICATION_MESSAGE.isDisplayed();
    }

    /**
     * Check if resend verification link is available
     */
    public boolean isResendLinkAvailable() {
        return RESEND_VERIFICATION_LINK.exists() && RESEND_VERIFICATION_LINK.isDisplayed();
    }

    /**
     * Wait for page to load completely
     */
    public VerificationPendingPage waitForPageLoad() {
        VERIFICATION_MESSAGE.should(appear);
        return this;
    }

    /**
     * Simulate email verification by constructing verification URL
     * This method simulates clicking the verification link from email
     */
    public LoginSuccessPage simulateEmailVerification(String verificationToken) {
        String verificationUrl = "http://localhost:8080/user/registrationConfirm?token=" + verificationToken;
        Selenide.open(verificationUrl);
        return new LoginSuccessPage();
    }

    /**
     * Simulate email verification with an invalid token
     */
    public VerificationPendingPage simulateInvalidEmailVerification() {
        String verificationUrl = "http://localhost:8080/user/registrationConfirm?token=invalid-token";
        Selenide.open(verificationUrl);
        return this;
    }
}