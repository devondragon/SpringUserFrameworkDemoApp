package com.digitalsanctuary.spring.demo.user.ui.page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.*;

/**
 * Page object for update password page
 */
public class UpdatePasswordPage {

    private final SelenideElement CURRENT_PASSWORD_FIELD = Selenide.$x("//input[@id='oldPassword']");
    private final SelenideElement NEW_PASSWORD_FIELD = Selenide.$x("//input[@id='password']");
    private final SelenideElement CONFIRM_PASSWORD_FIELD = Selenide.$x("//input[@id='matchPassword']");
    private final SelenideElement UPDATE_PASSWORD_BUTTON = Selenide.$x("//button[@type='submit']");
    private final SelenideElement GLOBAL_MESSAGE = Selenide.$x("//div[@id='globalMessage']");
    private final SelenideElement BACK_TO_PROFILE_LINK = Selenide.$x("//a[contains(@href, 'update-user')]");

    public UpdatePasswordPage(String url) {
        Selenide.open(url);
    }

    public UpdatePasswordPage() {
        // For navigation to this page from other pages
    }

    /**
     * Update user password
     */
    public UpdatePasswordPage updatePassword(String currentPassword, String newPassword, String confirmPassword) {
        CURRENT_PASSWORD_FIELD.setValue(currentPassword);
        NEW_PASSWORD_FIELD.setValue(newPassword);
        CONFIRM_PASSWORD_FIELD.setValue(confirmPassword);
        UPDATE_PASSWORD_BUTTON.click();
        
        // Wait for response message
        GLOBAL_MESSAGE.should(appear);
        return this;
    }

    /**
     * Update password with matching new password
     */
    public UpdatePasswordPage updatePassword(String currentPassword, String newPassword) {
        return updatePassword(currentPassword, newPassword, newPassword);
    }

    /**
     * Get the success/error message displayed after password update
     */
    public String getMessage() {
        return GLOBAL_MESSAGE.shouldBe(visible).text();
    }

    /**
     * Check if password update was successful
     */
    public boolean isPasswordUpdateSuccessful() {
        return GLOBAL_MESSAGE.has(cssClass("alert-success"));
    }

    /**
     * Check if there's a password mismatch error
     */
    public boolean hasPasswordMismatchError() {
        SelenideElement matchPasswordError = CONFIRM_PASSWORD_FIELD.parent().$(".form-text.text-danger");
        return matchPasswordError.exists() && matchPasswordError.isDisplayed();
    }

    /**
     * Navigate back to profile page
     */
    public UpdateUserPage goBackToProfile() {
        BACK_TO_PROFILE_LINK.click();
        return new UpdateUserPage();
    }

    /**
     * Wait for page to load completely
     */
    public UpdatePasswordPage waitForPageLoad() {
        CURRENT_PASSWORD_FIELD.should(appear);
        NEW_PASSWORD_FIELD.should(appear);
        CONFIRM_PASSWORD_FIELD.should(appear);
        UPDATE_PASSWORD_BUTTON.should(appear);
        return this;
    }

    /**
     * Clear all form fields
     */
    public UpdatePasswordPage clearAllFields() {
        CURRENT_PASSWORD_FIELD.clear();
        NEW_PASSWORD_FIELD.clear();
        CONFIRM_PASSWORD_FIELD.clear();
        return this;
    }
}